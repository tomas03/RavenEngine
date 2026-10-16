package org.ravenEngine.components;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.ravenEngine.renderer.Camera;
import org.ravenEngine.renderer.Shader;
import org.ravenEngine.renderer.Sprite;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.scene.Component;
import org.ravenEngine.util.AssetPool;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Tilemap extends Component {
    private int columns;
    private int rows;
    private int tileWidth;
    private int tileHeight;

    private int[] tiles;
    private String spritesheetPath;
    private int numSprites;
    private int spriteSpacing;

    private transient Spritesheet spritesheet;
    private transient Shader shader;
    private transient int vaoID, vboID, eboID;
    private transient boolean isDirty = true;
    private transient int activeTileCount = 0;

    private static final int POS_SIZE = 2;
    private static final int COLOR_SIZE = 4;
    private static final int TEX_COORDS_SIZE = 2;
    private static final int TEX_ID_SIZE = 1;
    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE + TEX_COORDS_SIZE + TEX_ID_SIZE;
    private static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    private boolean lockMapBounds = true;

    //CONSTRUCTORES
    public Tilemap(int columns, int rows, int tileWidth, int tileHeight, String spritesheetPath, int numSprites, int spacing) {
        this.columns = columns;
        this.rows = rows;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.spritesheetPath = spritesheetPath;
        this.numSprites = numSprites;
        this.spriteSpacing = spacing;


        this.tiles = new int[columns * rows];
        Arrays.fill(this.tiles, -1);
    }

    // Requerido para deserialización con Gson
    public Tilemap() {
        this.tileWidth = 16;
        this.tileHeight = 16;
    }

    //OVERRIDES
    @Override
    public void start() {
        System.out.println("[Tilemap] Intentando cargar textura: " + this.spritesheetPath);
        this.shader = AssetPool.getShader("/Shaders/default.vert", "/Shaders/default.frag");
        if (this.spritesheetPath != null) {
            this.spritesheet = new Spritesheet(
                    AssetPool.getTexture(this.spritesheetPath),
                    this.tileWidth, this.tileHeight,
                    this.numSprites, this.spriteSpacing
            );
        }

        initBuffers();
        this.isDirty = true;
    }

    //FUNCIONES
    private void initBuffers() {
        int maxTiles = columns * rows;

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) maxTiles * 4 * VERTEX_SIZE_BYTES, GL_DYNAMIC_DRAW);

        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        int[] indices = new int[maxTiles * 6];
        for (int i = 0; i < maxTiles; i++) {
            int offsetArrayIndex = 6 * i;
            int offset = 4 * i;

            indices[offsetArrayIndex]     = offset + 0;
            indices[offsetArrayIndex + 1] = offset + 1;
            indices[offsetArrayIndex + 2] = offset + 3;
            indices[offsetArrayIndex + 3] = offset + 1;
            indices[offsetArrayIndex + 4] = offset + 2;
            indices[offsetArrayIndex + 5] = offset + 3;
        }

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



    public Vector2f worldToGrid(float worldX, float worldY) {
        float posX = gameObject != null ? gameObject.transform.pos.x : 0;
        float posY = gameObject != null ? gameObject.transform.pos.y : 0;

        int gridX = (int) Math.floor((worldX - posX) / tileWidth);
        int gridY = (int) Math.floor((worldY - posY) / tileHeight);

        return new Vector2f(gridX, gridY);
    }

    public Vector2f gridToWorld(int gridX, int gridY) {
        float posX = gameObject != null ? gameObject.transform.pos.x : 0;
        float posY = gameObject != null ? gameObject.transform.pos.y : 0;

        return new Vector2f(posX + (gridX * tileWidth), posY + (gridY * tileHeight));
    }

    private void rebuildMesh() {
        if (spritesheet == null) return;

        float[] vertices = new float[columns * rows * 4 * VERTEX_SIZE];
        int vertexOffset = 0;
        activeTileCount = 0;

        float originX = gameObject != null ? gameObject.transform.pos.x : 0;
        float originY = gameObject != null ? gameObject.transform.pos.y : 0;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                int tileId = tiles[x + y * columns];
                if (tileId < 0 || tileId >= spritesheet.size()) continue;

                Sprite sprite = spritesheet.getSprite(tileId);
                Vector2f[] texCoords = sprite.getTexCoords();

                float currentX = originX + (x * tileWidth);
                float currentY = originY + (y * tileHeight);
                float w = tileWidth;
                float h = tileHeight;

                // Vértices del quad
                // V0 (top-right)
                vertexOffset = loadVertex(vertices, vertexOffset, currentX + w, currentY + h, texCoords[0]);
                // V1 (bottom-right)
                vertexOffset = loadVertex(vertices, vertexOffset, currentX + w, currentY, texCoords[1]);
                // V2 (bottom-left)
                vertexOffset = loadVertex(vertices, vertexOffset, currentX, currentY, texCoords[2]);
                // V3 (top-left)
                vertexOffset = loadVertex(vertices, vertexOffset, currentX, currentY + h, texCoords[3]);

                activeTileCount++;
            }
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexOffset);
        buffer.put(vertices, 0, vertexOffset).flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

        isDirty = false;
    }

    private int loadVertex(float[] vertices, int offset, float x, float y, Vector2f uv) {
        vertices[offset]     = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = 1.0f; // R
        vertices[offset + 3] = 1.0f; // G
        vertices[offset + 4] = 1.0f; // B
        vertices[offset + 5] = 1.0f; // A
        vertices[offset + 6] = uv.x;
        vertices[offset + 7] = uv.y;
        vertices[offset + 8] = 1.0f; // Slot 1
        return offset + VERTEX_SIZE;
    }

    public void render(Camera camera) {
        if (isDirty) {
            rebuildMesh();
        }

        if (activeTileCount == 0 || spritesheet == null) return;

        shader.use();
        shader.uploadMat4f("uProjection", camera.getProjectionMatrix());
        shader.uploadMat4f("uView", camera.getViewMatrix());

        // 1. Activamos la unidad 0 (que corresponde a case 1 -> uTextures[0])
        glActiveTexture(GL_TEXTURE0);
        if (spritesheet.getSprite(0) != null) {
            spritesheet.getSprite(0).getTexture().bind();
        }

        // 2. Subimos los slots
        int[] slots = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        shader.uploadIntArray("uTextures", slots);

        // 3. Dibujamos el piso
        glBindVertexArray(vaoID);
        glDrawElements(GL_TRIANGLES, activeTileCount * 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        // 4. Limpiamos la unidad 0
        glActiveTexture(GL_TEXTURE0);
        if (spritesheet.getSprite(0) != null) {
            spritesheet.getSprite(0).getTexture().unbind();
        }
        shader.detach();
    }

    public void settileSolid(int tileIndex, boolean isSolid){
        if (isSolid){
            solidTilesIndices.add(tileIndex);
        }else{
            solidTilesIndices.remove(tileIndex);
        }
    }

    public boolean isSolid(int gridX,int gridY){
        //si se sale de los bordes del mapa actúa como pared
        if (gridX<0||gridX>=this.columns||gridY<0||gridY>=this.rows){
            return lockMapBounds;
        }

        //consulta el tile actual
        int tileId = getTile(gridX,gridY);
        if (tileId<0) return false;//aire o transparente
        return solidTilesIndices.contains(tileId);
    }

    //GETTERS Y SETTERS
    public void setTile(int gridX, int gridY, int spriteIndex) {
        if (gridX >= 0 && gridX < columns && gridY >= 0 && gridY < rows) {
            this.tiles[gridX + gridY * columns] = spriteIndex;
            this.isDirty = true;
        }
    }

    public int getTile(int gridX, int gridY) {
        if (gridX >= 0 && gridX < columns && gridY >= 0 && gridY < rows) {
            return this.tiles[gridX + gridY * columns];
        }
        return -1;
    }
    public int getColumns() { return columns; }
    public int getRows() { return rows; }
    public int getTileWidth() { return tileWidth; }
    public int getTileHeight() { return tileHeight; }
    public int[] getTiles() { return tiles; }
    public Spritesheet getSpritesheet() { return spritesheet; }
    public void setSpritesheet(Spritesheet spritesheet) { this.spritesheet = spritesheet; }

    //set que guarda que ID de sprite son paredes/obstáculos
    private final Set<Integer> solidTilesIndices = new HashSet<>();

}