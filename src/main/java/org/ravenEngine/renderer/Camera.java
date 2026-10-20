package org.ravenEngine.renderer;

import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.Random;

public class Camera {

    public Vector2f position = new Vector2f();
    private final Vector2f renderPosition = new Vector2f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();

    private float zoom = 1.0f;
    private final float projectionWidth = 640.0f;
    private final float projectionHeight = 360.0f;

    // --- SISTEMA DE CAMERA SHAKE (TRAUMA-BASED) ---
    private float trauma = 0.0f;
    private final float traumaDecayRate = 1.4f;
    private final float maxShakeOffset = 18.0f;
    private final float maxShakeAngle = 0.04f;
    private float shakeAngle = 0.0f;
    private float shakeOffsetX = 0.0f;
    private float shakeOffsetY = 0.0f;
    private final Random random = new Random();

    public Camera() {
        adjustProjection();
    }

    public Camera(Vector2f position) {
        this.position = position;
        adjustProjection();
    }

    public void adjustProjection() {
        projectionMatrix.identity();
        projectionMatrix.ortho(
                0.0f, projectionWidth * zoom,
                0.0f, projectionHeight * zoom,
                0.0f, 100.0f
        );
    }

    public void update(float dt) {
        // Procesamiento de Camera Shake
        shakeOffsetX = 0.0f;
        shakeOffsetY = 0.0f;
        shakeAngle = 0.0f;

        if (trauma > 0.0f) {
            float shakeIntensity = trauma * trauma;

            shakeOffsetX = maxShakeOffset * shakeIntensity * ((random.nextFloat() * 2.0f) - 1.0f);
            shakeOffsetY = maxShakeOffset * shakeIntensity * ((random.nextFloat() * 2.0f) - 1.0f);
            shakeAngle   = maxShakeAngle  * shakeIntensity * ((random.nextFloat() * 2.0f) - 1.0f);

            trauma = Math.max(trauma - (traumaDecayRate * dt), 0.0f);
        }
    }

    public Matrix4f getViewMatrix() {
        // En cada frame sincronizamos la posición real de render
        renderPosition.set(this.position.x + shakeOffsetX, this.position.y + shakeOffsetY);

        viewMatrix.identity();

        // 1. Si hay rotación por shake, rotamos alrededor del centro de la pantalla
        if (shakeAngle != 0.0f) {
            float centerX = (projectionWidth * zoom) * 0.5f;
            float centerY = (projectionHeight * zoom) * 0.5f;

            viewMatrix.translate(centerX, centerY, 0.0f);
            viewMatrix.rotateZ(shakeAngle);
            viewMatrix.translate(-centerX, -centerY, 0.0f);
        }

        // 2. Traslación directa de cámara 2D (UNA SOLA VEZ)
        viewMatrix.translate(-renderPosition.x, -renderPosition.y, 0.0f);

        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public void addTrauma(float amount) {
        this.trauma = Math.min(this.trauma + amount, 1.0f);
    }

    public void addZoom(float value) {
        this.zoom = Math.max(0.2f, Math.min(this.zoom - (value * 0.1f), 4.0f));
        adjustProjection();
    }

    public void resetZoom() {
        this.zoom = 1.0f;
        adjustProjection();
    }

    public Vector2f screenToWorld(float screenX, float screenY, float viewportWidth, float viewportHeight) {
        float normalizedX = screenX / viewportWidth;
        float normalizedY = 1.0f - (screenY / viewportHeight); // Invertir Y para OpenGL

        float worldWidth = this.projectionWidth * this.zoom;
        float worldHeight = this.projectionHeight * this.zoom;

        // Usamos position directamente si no hay shake, o renderPosition si está temblando
        float originX = (shakeOffsetX != 0.0f) ? renderPosition.x : position.x;
        float originY = (shakeOffsetY != 0.0f) ? renderPosition.y : position.y;

        float worldX = originX + (normalizedX * worldWidth);
        float worldY = originY + (normalizedY * worldHeight);

        return new Vector2f(worldX, worldY);
    }

    public float getProjectionWidth() {
        return this.projectionWidth;
    }

    public float getProjectionHeight() {
        return this.projectionHeight;
    }

    public float getZoom() {
        return this.zoom;
    }

    public Vector2f getProjectionSize() {
        return new Vector2f(this.projectionWidth * this.zoom, this.projectionHeight * this.zoom);
    }
}