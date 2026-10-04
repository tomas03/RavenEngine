package org.alchemistShop.scenes;

import org.ravenEngine.io.KeyListener;
import org.ravenEngine.renderer.Shader;
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
    private Shader shader;
    private int vaoId,vboId,eboId;

    @Override
    public void init() {
        System.out.println("Cargando la tienda...");
        shader = new Shader("/Shaders/default.vert","/Shaders/default.frag");
        shader.compile();

        //crear cuadrado 64x64 en la posicion x=200, y=150
        float x = 200.0f;
        float y = 150.0f;
        float size = 64.0f;

        float[] vertices={
                x+size,y+size,0.0f,1.0f, 0.6f, 0.0f, 1.0f, //Arriba der(Naranja)
                x + size, y,0.0f,1.0f, 0.2f, 0.2f, 1.0f, // Abajo der  (Rojo)
                x,y,0.0f,0.2f, 0.8f, 0.4f, 1.0f, // Abajo izq  (Verde)
                x,y + size, 0.0f,0.2f, 0.6f, 1.0f, 1.0f  // Arriba izq (Azul)
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

        int stride = 7 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

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

        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES,6,GL_UNSIGNED_INT,0);
        glBindVertexArray(0);

        shader.detach();
    }
}
