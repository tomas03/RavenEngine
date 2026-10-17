package org.ravenEngine.renderer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    public Vector2f position;

    // Control de zoom y resolución virtual
    private float zoom = 1.0f;
    private final float BASE_PROJECTION_WIDTH = 640.0f;
    private final float BASE_PROJECTION_HEIGHT = 360.0f;
    private final Vector2f projectionSize = new Vector2f(BASE_PROJECTION_WIDTH, BASE_PROJECTION_HEIGHT);

    public Camera(Vector2f position) {
        this.position = position;
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        adjustProjection();
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
        float normalizedY = (viewportHeight - screenY) / viewportHeight;

        // 2. Mapear usando el projectionSize actual (afectado por el zoom)
        float worldX = this.position.x + (normalizedX * this.projectionSize.x);
        float worldY = this.position.y + (normalizedY * this.projectionSize.y);

        return new Vector2f(worldX, worldY);
    }

    public void addZoom(float delta) {
        // delta > 0 acerca, delta < 0 aleja
        this.zoom -= delta * 0.1f;
        this.zoom = Math.max(0.25f, Math.min(this.zoom, 3.0f));
        adjustProjection();
    }

    public void resetZoom() {
        this.zoom = 1.0f;
        adjustProjection();
    }

    public void adjustProjection() {
        this.projectionSize.set(BASE_PROJECTION_WIDTH * zoom, BASE_PROJECTION_HEIGHT * zoom);

        this.projectionMatrix.identity();
        this.projectionMatrix.ortho(
                0.0f, this.projectionSize.x,
                0.0f, this.projectionSize.y,
                0.0f, 100.0f
        );
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Vector2f getProjectionSize() {
        return projectionSize;
    }

    public float getZoom() {
        return zoom;
    }
}