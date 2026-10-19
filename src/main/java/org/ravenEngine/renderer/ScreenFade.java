package org.ravenEngine.renderer;

import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ScreenFade {

    private enum FadeState { IDLE, FADING_IN, FADING_OUT }

    private FadeState state = FadeState.IDLE;
    private float currentAlpha = 0.0f;
    private float fadeSpeed = 1.0f;
    private final Vector4f fadeColor = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);

    private static int vao = 0;
    private static int vbo = 0;
    private static int shaderProgram = 0;

    public ScreenFade() {
        // Dejamos el constructor vacío sin llamadas a GL
    }

    public void update(float dt) {
        if (state == FadeState.FADING_OUT) {
            currentAlpha += fadeSpeed * dt;
            if (currentAlpha >= 1.0f) {
                currentAlpha = 1.0f;
                state = FadeState.IDLE;
            }
        } else if (state == FadeState.FADING_IN) {
            currentAlpha -= fadeSpeed * dt;
            if (currentAlpha <= 0.0f) {
                currentAlpha = 0.0f;
                state = FadeState.IDLE;
            }
        }
    }

    public void startFadeOut(float durationSeconds) {
        this.fadeSpeed = 1.0f / Math.max(durationSeconds, 0.01f);
        this.state = FadeState.FADING_OUT;
    }

    public void startFadeIn(float durationSeconds) {
        this.fadeSpeed = 1.0f / Math.max(durationSeconds, 0.01f);
        this.state = FadeState.FADING_IN;
    }

    public void render() {
        // Aseguramos que los recursos de GPU existan antes de dibujar
        if (vao == 0) {
            initGL();
        }

        if (currentAlpha <= 0.001f) return;

        glUseProgram(shaderProgram);

        int uColorLoc = glGetUniformLocation(shaderProgram, "uColor");
        glUniform4f(uColorLoc, fadeColor.x, fadeColor.y, fadeColor.z, currentAlpha);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        glUseProgram(0);
    }

    private static void initGL() {
        if (vao != 0) return;

        float[] quadVertices = {
                -1.0f,  1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,

                -1.0f,  1.0f,
                1.0f, -1.0f,
                1.0f,  1.0f
        };

        FloatBuffer buffer = BufferUtils.createFloatBuffer(quadVertices.length);
        buffer.put(quadVertices).flip();

        String vertSrc = "#version 330 core\n" +
                "layout (location = 0) in vec2 aPos;\n" +
                "void main() {\n" +
                "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
                "}";

        String fragSrc = "#version 330 core\n" +
                "uniform vec4 uColor;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "    FragColor = uColor;\n" +
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
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
    }

    public boolean isBusy() {
        return state != FadeState.IDLE;
    }
}