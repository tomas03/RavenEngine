package org.alchemistShop.scenes;

import org.ravenEngine.io.KeyListener;
import org.ravenEngine.renderer.Shader;
import org.ravenEngine.renderer.Texture;
import org.ravenEngine.scene.Scene;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ShopScene extends Scene {
    private Texture texture;
    private Shader shader;
    private int vaoId,vboId,eboId;

    @Override
    public void init() {
        System.out.println("Cargando la tienda...");
        shader = new Shader("/Shaders/default.vert","/Shaders/default.frag");
        shader.compile();

        texture = new Texture("/textures/Frasco.png");

        //crear cuadrado 64x64 en la posicion x=200, y=150
        float x = 200.0f;
        float y = 150.0f;
        float size = 64.0f;

        // Vértices con: Posición (3), Color blanco neutro (4), Coordenadas UV (2)
        float[] vertices = {
                // Pos (X, Y, Z)          // Color (R, G, B, A)      // UV (U, V)
                x + size, y + size, 0.0f,  1.0f, 1.0f, 1.0f, 1.0f,    1.0f, 1.0f, // Arriba der
                x + size, y,        0.0f,  1.0f, 1.0f, 1.0f, 1.0f,    1.0f, 0.0f, // Abajo der
                x,        y,        0.0f,  1.0f, 1.0f, 1.0f, 1.0f,    0.0f, 0.0f, // Abajo izq
                x,        y + size, 0.0f,  1.0f, 1.0f, 1.0f, 1.0f,    0.0f, 1.0f  // Arriba izq
        };
        int[] indices={
                0,1,3,
                1,2,3
        };

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER,vertexBuffer,GL_STATIC_DRAW);

        IntBuffer elementBuffer = BufferUtils.createIntBuffer(indices.length);
        elementBuffer.put(indices).flip();

        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW);

        // Stride total: 9 floats (3 pos + 4 color + 2 UV)
        int stride = (3 + 4 + 2) * Float.BYTES;

        // 0: Posición (3 floats)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        // 1: Color (4 floats)
        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // 2: Coordenadas UV (2 floats)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, (3 + 4) * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void update(float dt) {
        float cameraSpeed = 250.0f;

        if (KeyListener.isKeyPressed(GLFW_KEY_W)){
            camera.position.y -= cameraSpeed*dt;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_S)) {
            camera.position.y += cameraSpeed * dt;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_D)) {
            camera.position.x -= cameraSpeed * dt;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_A)) {
            camera.position.x += cameraSpeed * dt;
        }
        // Testigo: imprime la posición solo si te estás moviendo
        if (KeyListener.isKeyPressed(GLFW_KEY_W) || KeyListener.isKeyPressed(GLFW_KEY_S) ||
                KeyListener.isKeyPressed(GLFW_KEY_D) || KeyListener.isKeyPressed(GLFW_KEY_A)) {
            System.out.printf("Camara -> X: %.2f | Y: %.2f%n", camera.position.x, camera.position.y);
        }
    }

    @Override
    public void render() {
        shader.use();

        //insertar matrices de la camara al shader
        shader.uploadMat4f("uProjection",camera.getProjectionMatrix());
        shader.uploadMat4f("uView",camera.getViewMatrix());

        // Activamos y vinculamos la textura en el slot 0
        glActiveTexture(GL_TEXTURE0);
        texture.bind();
        shader.uploadInt("uTexture", 0);

        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,0);
        glBindVertexArray(0);

        texture.unbind();
        shader.detach();
    }
}
