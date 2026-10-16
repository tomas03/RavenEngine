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

    public Matrix4f getViewMatrix() {
        Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
        Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
        this.viewMatrix.identity();
        this.viewMatrix.lookAt(
                new Vector3f(position.x, position.y, 20.0f),
                cameraFront.add(position.x, position.y, 0.0f),
                cameraUp
        );
        return this.viewMatrix;
    }

    public Vector2f screenToWorld(float screenX, float screenY, float viewportWidth, float viewportHeight) {
        // 1. Normalizar las coordenadas a rango [0, 1] respecto al Viewport
        float normalizedX = screenX / viewportWidth;
        // Invertimos Y porque la pantalla tiene (0,0) arriba-izquierda y el mundo abajo-izquierda
        float normalizedY = (viewportHeight - screenY) / viewportHeight;

        // 2. Mapear a coordenadas de proyección ortográfica de la cámara
        // Asumiendo proyección ortográfica estándar (ancho y alto virtuales del viewport de la cámara)
        float worldX = this.position.x + (normalizedX * 640.0f);
        float worldY = this.position.y + (normalizedY * 360.0f);

        return new Vector2f(worldX, worldY);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
}
