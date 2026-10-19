package org.ravenEngine.renderer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

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
    private final float traumaDecayRate = 1.4f; // Velocidad con la que se apaga el temblor
    private final float maxShakeOffset = 18.0f;  // Píxeles máximos de desplazamiento
    private final float maxShakeAngle = 0.04f;   // Rotación máxima en radianes (~2.3 grados)
    private float shakeAngle = 0.0f;
    private final Random random = new Random();

    // --- SEGUIMIENTO SUAVE (SMOOTH FOLLOW) ---
    private Vector2f targetToFollow = null;
    private float smoothSpeed = 5.0f; // Multiplicador de interpolación

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
        // 1. Seguimiento suave al objetivo (Lerp)
        if (targetToFollow != null) {
            float targetX = targetToFollow.x - (projectionWidth * zoom) / 2.0f;
            float targetY = targetToFollow.y - (projectionHeight * zoom) / 2.0f;

            this.position.x += (targetX - this.position.x) * smoothSpeed * dt;
            this.position.y += (targetY - this.position.y) * smoothSpeed * dt;
        }

        // 2. Procesamiento de Camera Shake
        float shakeOffsetX = 0.0f;
        float shakeOffsetY = 0.0f;
        shakeAngle = 0.0f;

        if (trauma > 0.0f) {
            // El temblor no lineal (trauma al cuadrado) da una sensación más física
            float shakeIntensity = trauma * trauma;

            shakeOffsetX = maxShakeOffset * shakeIntensity * ((random.nextFloat() * 2.0f) - 1.0f);
            shakeOffsetY = maxShakeOffset * shakeIntensity * ((random.nextFloat() * 2.0f) - 1.0f);
            shakeAngle   = maxShakeAngle  * shakeIntensity * ((random.nextFloat() * 2.0f) - 1.0f);

            // Reducción del trauma con el paso del tiempo
            trauma = Math.max(trauma - (traumaDecayRate * dt), 0.0f);
        }

        // Posición final combinada enviada a la GPU
        renderPosition.set(this.position.x + shakeOffsetX, this.position.y + shakeOffsetY);
    }

    public Matrix4f getViewMatrix() {
        viewMatrix.identity();

        // Aplicamos traslación de cámara invertida
        viewMatrix.translate(-renderPosition.x, -renderPosition.y, 0.0f);

        // Si hay rotación por shake, rotamos alrededor del centro de la pantalla
        if (shakeAngle != 0.0f) {
            float centerX = (projectionWidth * zoom) / 2.0f;
            float centerY = (projectionHeight * zoom) / 2.0f;

            viewMatrix.translate(centerX, centerY, 0.0f);
            viewMatrix.rotateZ(shakeAngle);
            viewMatrix.translate(-centerX, -centerY, 0.0f);
        }

        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /**
     * Agrega trauma entre 0.0 y 1.0 (se acumula hasta 1.0)
     */
    public void addTrauma(float amount) {
        this.trauma = Math.min(this.trauma + amount, 1.0f);
    }

    public void setTarget(Vector2f target) {
        this.targetToFollow = target;
    }

    public void setSmoothSpeed(float speed) {
        this.smoothSpeed = speed;
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
        float normalizedY = 1.0f - (screenY / viewportHeight); // Inversión eje Y

        float worldX = renderPosition.x + (normalizedX * projectionWidth * zoom);
        float worldY = renderPosition.y + (normalizedY * projectionHeight * zoom);

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