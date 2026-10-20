package org.ravenEngine.renderer;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.ravenEngine.renderer.SpriteRenderer;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.util.AssetPool;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderBatch {
    private static final int POS_SIZE = 2;
    private static final int COLOR_SIZE = 4;
    private static final int TEX_COORDS_SIZE = 2;
    private static final int TEX_ID_SIZE = 1;

    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE + TEX_COORDS_SIZE + TEX_ID_SIZE;
    private static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private final SpriteRenderer[] sprites;
    private int numSprites;
    private boolean hasRoom;
    private final float[] vertices;
    private final FloatBuffer vertexBuffer; // Buffer reutilizable para 0 asignaciones por frame
    private final List<Texture> textures;
    private int vaoID, vboID;
    private final int maxBatchSize;
    private final Shader shader;

    public RenderBatch(int maxBatchSize, Shader shader) {
        this.shader = AssetPool.getShader("/Shaders/default.vert", "/Shaders/default.frag");
        this.sprites = new SpriteRenderer[maxBatchSize];
        this.maxBatchSize = maxBatchSize;

        this.vertices = new float[maxBatchSize * 4 * VERTEX_SIZE];
        this.vertexBuffer = BufferUtils.createFloatBuffer(this.vertices.length);
        this.numSprites = 0;
        this.hasRoom = true;
        this.textures = new ArrayList<>();
    }

    public void start() {
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        int eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        int[] indices = generateIndices();
        IntBuffer eboBuffer = BufferUtils.createIntBuffer(indices.length);
        eboBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, eboBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, (long) POS_SIZE * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, TEX_COORDS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, (long) (POS_SIZE + COLOR_SIZE) * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, TEX_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, (long) (POS_SIZE + COLOR_SIZE + TEX_COORDS_SIZE) * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
    }

    public void addSprite(SpriteRenderer spr) {
        int index = this.numSprites;
        this.sprites[index] = spr;
        this.numSprites++;

        if (spr.getTexture() != null) {
            if (!textures.contains(spr.getTexture())) {
                textures.add(spr.getTexture());
            }
        }

        loadVertexProperties(index);

        if (numSprites >= this.maxBatchSize) {
            this.hasRoom = false;
        }
    }

    public boolean destroyIfExists(GameObject go) {
        SpriteRenderer spr = go.getComponent(SpriteRenderer.class);
        if (spr != null) {
            for (int i = 0; i < numSprites; i++) {
                if (sprites[i] == spr || (sprites[i] != null && sprites[i].gameObject == go)) {
                    for (int j = i; j < numSprites - 1; j++) {
                        sprites[j] = sprites[j + 1];
                    }
                    sprites[numSprites - 1] = null;
                    this.numSprites--;
                    this.hasRoom = true;
                    return true;
                }
            }
        }
        return false;
    }

    public void render(Camera camera) {
        if (numSprites == 0) return;

        // 1. Y-Sorting: Insertion Sort optimizado por zIndex y base de los pies
        sortSprites();

        // 2. Regenerar los vértices con el orden correcto en el array
        for (int i = 0; i < numSprites; i++) {
            loadVertexProperties(i);
        }

        // 3. Subir al VBO usando el buffer precargado (Cero asignaciones en memoria)
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        vertexBuffer.clear();
        vertexBuffer.put(vertices, 0, numSprites * 4 * VERTEX_SIZE).flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        shader.use();
        shader.uploadMat4f("uProjection", camera.getProjectionMatrix());
        shader.uploadMat4f("uView", camera.getViewMatrix());

        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            textures.get(i).bind();
        }

        int[] slots = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        shader.uploadIntArray("uTextures", slots);

        glBindVertexArray(vaoID);
        glDrawElements(GL_TRIANGLES, this.numSprites * 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        for (Texture tex : textures) {
            tex.unbind();
        }
        shader.detach();
    }

    private void loadVertexProperties(int index) {
        SpriteRenderer sprite = this.sprites[index];
        if (sprite == null || sprite.gameObject == null || sprite.gameObject.transform == null) return;

        int offset = index * 4 * VERTEX_SIZE;

        Vector4f color = sprite.getColor();
        Vector2f[] texCoords = sprite.getTexCoords();

        int texId = 0;
        if (sprite.getTexture() != null) {
            for (int i = 0; i < textures.size(); i++) {
                if (textures.get(i).equals(sprite.getTexture())) {
                    texId = i + 1;
                    break;
                }
            }

            if (texId == 0) {
                if (textures.size() < 8) {
                    textures.add(sprite.getTexture());
                    texId = textures.size();
                } else {
                    System.err.println("[RenderBatch] Límite de 8 texturas alcanzado en el batch.");
                }
            }
        }

        float x = sprite.gameObject.transform.pos.x;
        float y = sprite.gameObject.transform.pos.y;
        float w = sprite.gameObject.transform.scale.x;
        float h = sprite.gameObject.transform.scale.y;

        loadSingleVertex(offset, x + w, y + h, color, texCoords[0].x, texCoords[0].y, texId);
        loadSingleVertex(offset + VERTEX_SIZE, x + w, y, color, texCoords[1].x, texCoords[1].y, texId);
        loadSingleVertex(offset + (2 * VERTEX_SIZE), x, y, color, texCoords[2].x, texCoords[2].y, texId);
        loadSingleVertex(offset + (3 * VERTEX_SIZE), x, y + h, color, texCoords[3].x, texCoords[3].y, texId);
    }

    private void loadSingleVertex(int offset, float x, float y, Vector4f c, float u, float v, float texId) {
        vertices[offset] = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = c.x;
        vertices[offset + 3] = c.y;
        vertices[offset + 4] = c.z;
        vertices[offset + 5] = c.w;
        vertices[offset + 6] = u;
        vertices[offset + 7] = v;
        vertices[offset + 8] = texId;
    }

    private int[] generateIndices() {
        int[] elements = new int[6 * maxBatchSize];
        for (int i = 0; i < maxBatchSize; i++) {
            int offsetArrayIndex = 6 * i;
            int offset = 4 * i;

            elements[offsetArrayIndex] = offset + 0;
            elements[offsetArrayIndex + 1] = offset + 1;
            elements[offsetArrayIndex + 2] = offset + 3;

            elements[offsetArrayIndex + 3] = offset + 1;
            elements[offsetArrayIndex + 4] = offset + 2;
            elements[offsetArrayIndex + 5] = offset + 3;
        }
        return elements;
    }

    private void sortSprites() {
        for (int i = 1; i < numSprites; i++) {
            SpriteRenderer key = sprites[i];
            int j = i - 1;

            while (j >= 0 && compareSprites(sprites[j], key) > 0) {
                sprites[j + 1] = sprites[j];
                j = j - 1;
            }
            sprites[j + 1] = key;
        }
    }

    private int compareSprites(SpriteRenderer a, SpriteRenderer b) {
        if (a == null || a.gameObject == null || a.gameObject.transform == null) return -1;
        if (b == null || b.gameObject == null || b.gameObject.transform == null) return 1;

        // 1. Capa fija (zIndex de Transform o SpriteRenderer)
        int zA = a.gameObject.transform.zIndex;
        int zB = b.gameObject.transform.zIndex;

        if (zA != zB) {
            return Integer.compare(zA, zB);
        }

        // 2. Y-Sorting: Base de los pies (posición Y + offset si está activo)
        float feetA = a.gameObject.transform.pos.y + a.getYSortOffset();
        float feetB = b.gameObject.transform.pos.y + b.getYSortOffset();

        // En coordenadas donde +Y es hacia arriba:
        // Mayor Y = más al fondo = menor índice en el batch (se dibuja primero)
        // Menor Y = más adelante/abajo = mayor índice en el batch (se dibuja encima)
        return Float.compare(feetB, feetA);
    }

    public boolean hasRoom() { return this.hasRoom; }
    public boolean hasTextureRoom() { return this.textures.size() < 8; }
    public boolean hasTexture(Texture tex) { return this.textures.contains(tex); }
}