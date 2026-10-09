package org.ravenEngine.renderer;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.ravenEngine.components.SpriteRenderer;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderBatch {
    // Pos (2), Color (4), UV (2), TexId (1) = 9 floats
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
    private final List< Texture > textures;
    private int vaoID, vboID;
    private final int maxBatchSize;
    private final Shader shader;

    public RenderBatch(int maxBatchSize, Shader shader) {
        this.shader = shader;
        this.sprites = new SpriteRenderer[maxBatchSize];
        this.maxBatchSize = maxBatchSize;

        this.vertices = new float[maxBatchSize * 4 * VERTEX_SIZE];
        this.numSprites = 0;
        this.hasRoom = true;
        this.textures = new ArrayList< >();
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

        // 0: Pos (2)
        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, 0);
        glEnableVertexAttribArray(0);

        // 1: Color (4)
        glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, (long) POS_SIZE * Float.BYTES);
        glEnableVertexAttribArray(1);

        // 2: UVs (2)
        glVertexAttribPointer(2, TEX_COORDS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, (long) (POS_SIZE + COLOR_SIZE) * Float.BYTES);
        glEnableVertexAttribArray(2);

        // 3: TexId (1)
        glVertexAttribPointer(3, TEX_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, (long) (POS_SIZE + COLOR_SIZE + TEX_COORDS_SIZE) * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
    }

    public void addSprite(SpriteRenderer spr) {
        int index = this.numSprites;
        this.sprites[index] = spr;
        this.numSprites++;

        if (spr.getTexture() != null && !textures.contains(spr.getTexture())) {
            textures.add(spr.getTexture());
        }

        loadVertexProperties(index);

        if (numSprites >= this.maxBatchSize) {
            this.hasRoom = false;
        }
    }

    public void render(Camera camera) {
        for (int i = 0; i < numSprites; i++) {
            loadVertexProperties(i);
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        shader.use();
        shader.uploadMat4f("uProjection", camera.getProjectionMatrix());
        shader.uploadMat4f("uView", camera.getViewMatrix());

        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            textures.get(i).bind();
        }
        shader.uploadInt("uTexture", 0);

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
        int offset = index * 4 * VERTEX_SIZE;

        Vector4f color = sprite.getColor();
        Vector2f[] texCoords = sprite.getTexCoords();

        float texId = 0.0f;
        if (sprite.getTexture() != null) {
            texId = 1.0f;
        }

        float x = sprite.gameObject.transform.pos.x;
        float y = sprite.gameObject.transform.pos.y;
        float w = sprite.gameObject.transform.scale.x;
        float h = sprite.gameObject.transform.scale.y;

        // Vértice 0: Arriba Derecha
        loadSingleVertex(offset, x + w, y + h, color, texCoords[0].x, texCoords[0].y, texId);
        // Vértice 1: Abajo Derecha
        loadSingleVertex(offset + VERTEX_SIZE, x + w, y, color, texCoords[1].x, texCoords[1].y, texId);
        // Vértice 2: Abajo Izquierda
        loadSingleVertex(offset + (2 * VERTEX_SIZE), x, y, color, texCoords[2].x, texCoords[2].y, texId);
        // Vértice 3: Arriba Izquierda
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

            // 3, 2, 0  -  0, 2, 1 (sentido antihorario estándar)
            elements[offsetArrayIndex] = offset + 0;
            elements[offsetArrayIndex + 1] = offset + 1;
            elements[offsetArrayIndex + 2] = offset + 3;

            elements[offsetArrayIndex + 3] = offset + 1;
            elements[offsetArrayIndex + 4] = offset + 2;
            elements[offsetArrayIndex + 5] = offset + 3;
        }
        return elements;
    }

    public boolean hasRoom() {
        return this.hasRoom;
    }
}