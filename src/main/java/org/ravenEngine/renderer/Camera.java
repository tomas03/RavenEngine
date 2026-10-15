package org.ravenEngine.renderer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Camera {
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    public Vector2f position;

    public Camera(Vector2f position) {
        this.position = position;
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        adjustProjection();
    }

    //Configurar el area visible
    public void adjustProjection() {
        projectionMatrix.identity();
        // 640x360 píxeles virtuales = escala perfecta x2 en ventana de 1280x720
        projectionMatrix.ortho(0.0f, 640.0f, 0.0f, 360.0f, 0.0f, 100.0f);
    }

    public Matrix4f getViewMatrix(){
        Vector3f cameraFront = new Vector3f(0.0f,0.0f,-1.0f);
        Vector3f cameraUp = new Vector3f(0.0f,1.0f,0.0f);
        viewMatrix.identity();
        viewMatrix.lookAt(
                new Vector3f(position.x,position.y,20.0f),
                cameraFront.add(position.x,position.y,0.0f),
                cameraUp
        );
        return viewMatrix;
    }

    public Vector2f screenToWorld(float normX, float normY) {
        // normX y normY van de -1.0 a 1.0 (NDC)
        Vector4f clipSpace = new Vector4f(normX, normY, 0.0f, 1.0f);

        // Matriz inversa View * Projection
        Matrix4f invViewProj = new Matrix4f();
        this.projectionMatrix.mul(this.viewMatrix, invViewProj);
        invViewProj.invert();

        clipSpace.mul(invViewProj);
        return new Vector2f(clipSpace.x, clipSpace.y);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
