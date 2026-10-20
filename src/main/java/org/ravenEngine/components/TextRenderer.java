package org.ravenEngine.components;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.ravenEngine.renderer.Camera;
import org.ravenEngine.renderer.text.Font;

import java.io.File;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextRenderer extends Component {

    // Variables serializables
    private String text = "";
    private String fontPath = "/usr/share/fonts/noto/NotoSans-Regular.ttf";
    private float fontSize = 20.0f;
    private Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private Vector2f offset = new Vector2f(0.0f, 0.0f);
    private float scale = 1.0f;
    private boolean isVisible = true;

    // Recursos nativos excluidos de serialización
    private transient Font font;
    private static transient int vao = 0;
    private static transient int vbo = 0;
    private static transient int shaderProgram = 0;

    private static final int MAX_CHARS = 256;
    private static final int VERTEX_SIZE = 4;
    private static transient FloatBuffer vertexBuffer;

    public TextRenderer() {}

    public TextRenderer(String fontPath, float fontSize, String text) {
        this.fontPath = fontPath;
        this.fontSize = fontSize;
        this.text = text;
        this.font = new Font(fontPath, fontSize);
    }

    public TextRenderer(Font font, String text, Vector4f color, Vector2f offset) {
        this.font = font;
        this.text = text;
        this.fontSize = font != null ? font.getFontSize() : 20.0f;
        this.color.set(color);
        this.offset.set(offset);
    }

    @Override
    public void start() {
        initGLResources();
        ensureFontLoaded();
    }

    @Override
    public void update(float dt) {}

    private void ensureFontLoaded() {
        if (this.fontPath == null || this.fontPath.isEmpty()) return;

        // Si la fuente no existe o si cambió el tamaño o la ruta desde el Inspector
        boolean needsReload = (this.font == null)
                || (Math.abs(this.font.getFontSize() - this.fontSize) > 0.001f)
                || (!this.fontPath.equals(this.font.getFontPath()));

        if (needsReload) {
            File f = new File(this.fontPath);
            if (f.exists()) {
                if (this.font != null) {
                    this.font.destroy();
                }
                this.font = new Font(this.fontPath, this.fontSize);
            }
        }
    }

    public void render(Camera camera) {
        ensureFontLoaded();
        if (font == null || !isVisible || text == null || text.isEmpty()) return;
        if (gameObject == null || gameObject.transform == null) return;

        float startX = gameObject.transform.pos.x + offset.x;
        float startY = gameObject.transform.pos.y + offset.y;

        int vertexCount = 0;
        vertexBuffer.clear();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer pX = stack.floats(startX);
            FloatBuffer pY = stack.floats(startY);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '\n') {
                    pX.put(0, startX);
                    pY.put(0, pY.get(0) - (font.getFontSize() * scale));
                    continue;
                }

                if (c < 32 || c >= 128) continue;

                STBTruetype.stbtt_GetBakedQuad(
                        font.getCdata(),
                        font.getBitmapWidth(),
                        font.getBitmapHeight(),
                        c - 32,
                        pX, pY,
                        q,
                        false
                );

                float x0 = q.x0();
                float x1 = q.x1();
                float y0 = startY - (q.y0() - startY) * scale;
                float y1 = startY - (q.y1() - startY) * scale;

                // Triángulo 1
                vertexBuffer.put(x0).put(y1).put(q.s0()).put(q.t1());
                vertexBuffer.put(x0).put(y0).put(q.s0()).put(q.t0());
                vertexBuffer.put(x1).put(y0).put(q.s1()).put(q.t0());

                // Triángulo 2
                vertexBuffer.put(x0).put(y1).put(q.s0()).put(q.t1());
                vertexBuffer.put(x1).put(y0).put(q.s1()).put(q.t0());
                vertexBuffer.put(x1).put(y1).put(q.s1()).put(q.t1());

                vertexCount += 6;
                if (vertexCount >= MAX_CHARS * 6) break;
            }
        }

        vertexBuffer.flip();
        if (vertexCount == 0) return;

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        glUseProgram(shaderProgram);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matBuffer = stack.mallocFloat(16);
            Matrix4f viewProj = new Matrix4f(camera.getProjectionMatrix()).mul(camera.getViewMatrix());
            viewProj.get(matBuffer);
            int uViewProjLoc = glGetUniformLocation(shaderProgram, "uViewProjection");
            glUniformMatrix4fv(uViewProjLoc, false, matBuffer);
        }

        int uColorLoc = glGetUniformLocation(shaderProgram, "uTextColor");
        glUniform4f(uColorLoc, color.x, color.y, color.z, color.w);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, font.getTextureId());
        int uFontTexLoc = glGetUniformLocation(shaderProgram, "uFontTexture");
        glUniform1i(uFontTexLoc, 0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);

        glUseProgram(0);
    }

    private static void initGLResources() {
        if (vao != 0) return;

        vertexBuffer = BufferUtils.createFloatBuffer(MAX_CHARS * 6 * VERTEX_SIZE);

        String vertSrc = "#version 330 core\n" +
                "layout (location = 0) in vec2 aPos;\n" +
                "layout (location = 1) in vec2 aTexCoords;\n" +
                "uniform mat4 uViewProjection;\n" +
                "out vec2 TexCoords;\n" +
                "void main() {\n" +
                "    TexCoords = aTexCoords;\n" +
                "    gl_Position = uViewProjection * vec4(aPos, 0.0, 1.0);\n" +
                "}";

        String fragSrc = "#version 330 core\n" +
                "in vec2 TexCoords;\n" +
                "uniform sampler2D uFontTexture;\n" +
                "uniform vec4 uTextColor;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "    float alpha = texture(uFontTexture, TexCoords).r;\n" +
                "    FragColor = vec4(uTextColor.rgb, uTextColor.a * alpha);\n" +
                "}";

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vertSrc);
        glCompileShader(vs);

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fragSrc);
        glCompileShader(fs);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vs);
        glAttachShader(shaderProgram, fs);
        glLinkProgram(shaderProgram);
        glDeleteShader(vs);
        glDeleteShader(fs);

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) MAX_CHARS * 6 * VERTEX_SIZE * Float.BYTES, GL_DYNAMIC_DRAW);

        int stride = VERTEX_SIZE * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    // Getters y Setters
    public void setText(String text) { this.text = text; }
    public String getText() { return text; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }
    public float getFontSize() { return fontSize; }
    public void setFont(Font font) { this.font = font; }
    public Font getFont() { return font; }
    public void setFontPath(String fontPath) { this.fontPath = fontPath; }
    public String getFontPath() { return fontPath; }
    public void setColor(Vector4f color) { this.color.set(color); }
    public Vector4f getColor() { return this.color; }
    public void setOffset(Vector2f offset) { this.offset.set(offset); }
    public Vector2f getOffset() { return this.offset; }
    public void setScale(float scale) { this.scale = scale; }
    public float getScale() { return this.scale; }
    public void setVisible(boolean visible) { this.isVisible = visible; }
    public boolean isVisible() { return isVisible; }
}