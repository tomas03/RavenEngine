package org.ravenEngine.components;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.ravenEngine.particles.Particle;
import org.ravenEngine.renderer.Camera;

import java.nio.FloatBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ParticleSystem extends Component {

    // ==========================================
    // PARÁMETROS CONFIGURABLES (Visibles en Inspector)
    // ==========================================
    private int maxParticles = 300;
    private float emissionRate = 45.0f; // Partículas emitidas por segundo
    private float lifetime = 1.2f;

    private float startSize = 8.0f;
    private float endSize = 2.0f;

    private Vector4f startColor = new Vector4f(0.2f, 0.8f, 1.0f, 1.0f); // Celeste mágico
    private Vector4f endColor = new Vector4f(0.8f, 0.2f, 0.9f, 0.0f);   // Violeta transparente

    private Vector2f baseVelocity = new Vector2f(0.0f, 35.0f); // Hacia arriba
    private Vector2f velocityVariation = new Vector2f(20.0f, 15.0f);
    private Vector2f gravity = new Vector2f(0.0f, -5.0f);

    private boolean additiveBlend = true; // Efecto brillante de fuego/luz
    private boolean isEmitting = true;

    // ==========================================
    // POOL Y RECURSOS GRÁFICOS (Transient para no serializar)
    // ==========================================
    private transient Particle[] particlePool;
    private transient int poolIndex;
    private transient float emitCounter = 0.0f;
    private transient final Random random = new Random();

    private static transient int vao = 0;
    private static transient int vbo = 0;
    private static transient int ebo = 0;
    private static transient int shaderProgram = 0;
    private static transient float[] vertexBufferData;

    private static final int VERTICES_PER_PARTICLE = 4;
    private static final int FLOATS_PER_VERTEX = 6; // x, y, r, g, b, a
    private static final int MAX_BATCH_PARTICLES = 1000;

    public ParticleSystem() {}

    @Override
    public void start() {
        this.particlePool = new Particle[maxParticles];
        for (int i = 0; i < maxParticles; i++) {
            this.particlePool[i] = new Particle();
        }
        this.poolIndex = maxParticles - 1;

        initGLResources();
    }

    @Override
    public void update(float dt) {
        // 1. Emisión progresiva de partículas
        if (isEmitting) {
            emitCounter += dt;
            float emitInterval = 1.0f / emissionRate;
            while (emitCounter >= emitInterval) {
                emitSingleParticle();
                emitCounter -= emitInterval;
            }
        }

        // 2. Actualización de partículas activas
        if (particlePool != null) {
            for (Particle p : particlePool) {
                p.update(dt, gravity);
            }
        }
    }

    private void emitSingleParticle() {
        if (particlePool == null || gameObject == null || gameObject.transform == null) return;

        Particle p = particlePool[poolIndex];
        poolIndex = (poolIndex - 1 + maxParticles) % maxParticles;

        p.active = true;
        p.lifetime = this.lifetime;
        p.lifeRemaining = this.lifetime;

        // Centrado respecto al Transform del GameObject
        p.position.set(
                gameObject.transform.pos.x + (gameObject.transform.scale.x * 0.5f),
                gameObject.transform.pos.y + (gameObject.transform.scale.y * 0.5f)
        );

        // Velocidad con dispersión aleatoria
        float vx = baseVelocity.x + (random.nextFloat() * 2.0f - 1.0f) * velocityVariation.x;
        float vy = baseVelocity.y + (random.nextFloat() * 2.0f - 1.0f) * velocityVariation.y;
        p.velocity.set(vx, vy);

        p.startSize = this.startSize;
        p.endSize = this.endSize;
        p.currentSize = this.startSize;

        p.startColor.set(this.startColor);
        p.endColor.set(this.endColor);
        p.currentColor.set(this.startColor);
    }

    // ==========================================
    // RENDERIZADO EN OPENGL
    // ==========================================
    public void render(Camera camera) {
        if (particlePool == null) return;

        int activeCount = 0;
        int offset = 0;

        for (Particle p : particlePool) {
            if (!p.active) continue;

            float halfSize = p.currentSize * 0.5f;
            float x = p.position.x;
            float y = p.position.y;
            float r = p.currentColor.x;
            float g = p.currentColor.y;
            float b = p.currentColor.z;
            float a = p.currentColor.w;

            // Vértice 0: Abajo-Izq
            vertexBufferData[offset++] = x - halfSize;
            vertexBufferData[offset++] = y - halfSize;
            vertexBufferData[offset++] = r; vertexBufferData[offset++] = g; vertexBufferData[offset++] = b; vertexBufferData[offset++] = a;

            // Vértice 1: Arriba-Izq
            vertexBufferData[offset++] = x - halfSize;
            vertexBufferData[offset++] = y + halfSize;
            vertexBufferData[offset++] = r; vertexBufferData[offset++] = g; vertexBufferData[offset++] = b; vertexBufferData[offset++] = a;

            // Vértice 2: Arriba-Der
            vertexBufferData[offset++] = x + halfSize;
            vertexBufferData[offset++] = y + halfSize;
            vertexBufferData[offset++] = r; vertexBufferData[offset++] = g; vertexBufferData[offset++] = b; vertexBufferData[offset++] = a;

            // Vértice 3: Abajo-Der
            vertexBufferData[offset++] = x + halfSize;
            vertexBufferData[offset++] = y - halfSize;
            vertexBufferData[offset++] = r; vertexBufferData[offset++] = g; vertexBufferData[offset++] = b; vertexBufferData[offset++] = a;

            activeCount++;
            if (activeCount >= MAX_BATCH_PARTICLES) break;
        }

        if (activeCount == 0) return;

        // Subir datos a la GPU
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBufferData);

        // Activar Shader y Matrices
        glUseProgram(shaderProgram);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matBuffer = stack.mallocFloat(16);
            Matrix4f viewProj = new Matrix4f(camera.getProjectionMatrix()).mul(camera.getViewMatrix());
            viewProj.get(matBuffer);
            int uViewProjLoc = glGetUniformLocation(shaderProgram, "uViewProjection");
            glUniformMatrix4fv(uViewProjLoc, false, matBuffer);
        }

        // Configuración de Blending
        if (additiveBlend) {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE); // Blend aditivo brillante
        } else {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // Blend estándar
        }

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, activeCount * 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        // Restaurar Blend normal para no romper los sprites del juego
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glUseProgram(0);
    }

    private static void initGLResources() {
        if (vao != 0) return; // Ya inicializado de forma compartida

        vertexBufferData = new float[MAX_BATCH_PARTICLES * VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX];

        // 1. Shaders embebidos
        String vertShaderSrc = "#version 330 core\n" +
                "layout (location = 0) in vec2 aPos;\n" +
                "layout (location = 1) in vec4 aColor;\n" +
                "uniform mat4 uViewProjection;\n" +
                "out vec4 vColor;\n" +
                "void main() {\n" +
                "    vColor = aColor;\n" +
                "    gl_Position = uViewProjection * vec4(aPos, 0.0, 1.0);\n" +
                "}";

        String fragShaderSrc = "#version 330 core\n" +
                "in vec4 vColor;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "    FragColor = vColor;\n" +
                "}";

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vertShaderSrc);
        glCompileShader(vs);

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fragShaderSrc);
        glCompileShader(fs);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vs);
        glAttachShader(shaderProgram, fs);
        glLinkProgram(shaderProgram);
        glDeleteShader(vs);
        glDeleteShader(fs);

        // 2. Generación de Índices para Quads (6 índices por partícula)
        int[] indices = new int[MAX_BATCH_PARTICLES * 6];
        for (int i = 0; i < MAX_BATCH_PARTICLES; i++) {
            int offset = i * 4;
            int idx = i * 6;
            indices[idx]     = offset + 0;
            indices[idx + 1] = offset + 1;
            indices[idx + 2] = offset + 2;
            indices[idx + 3] = offset + 0;
            indices[idx + 4] = offset + 2;
            indices[idx + 5] = offset + 3;
        }

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) vertexBufferData.length * Float.BYTES, GL_DYNAMIC_DRAW);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public void setColors(Vector4f start, Vector4f end) {
        this.startColor.set(start);
        this.endColor.set(end);
    }

    public void setSizes(float start, float end) {
        this.startSize = start;
        this.endSize = end;
    }

    public void setVelocity(Vector2f base, Vector2f variation) {
        this.baseVelocity.set(base);
        this.velocityVariation.set(variation);
    }

    public void setEmitting(boolean emitting) {
        this.isEmitting = emitting;
    }
}