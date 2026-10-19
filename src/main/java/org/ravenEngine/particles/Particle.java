package org.ravenEngine.particles;

import org.joml.Vector2f;
import org.joml.Vector4f;

public class Particle {
    public Vector2f position = new Vector2f();
    public Vector2f velocity = new Vector2f();
    public Vector4f currentColor = new Vector4f();
    public Vector4f startColor = new Vector4f();
    public Vector4f endColor = new Vector4f();

    public float currentSize;
    public float startSize;
    public float endSize;

    public float lifetime;
    public float lifeRemaining;
    public boolean active = false;

    public void update(float dt, Vector2f gravity) {
        if (!active) return;

        lifeRemaining -= dt;
        if (lifeRemaining <= 0.0f) {
            active = false;
            return;
        }

        // Progreso normalizado de vida (1.0 = recién nacida, 0.0 = por morir)
        float progress = lifeRemaining / lifetime;

        // Físicas (Velocidad + Gravedad)
        velocity.x += gravity.x * dt;
        velocity.y += gravity.y * dt;
        position.x += velocity.x * dt;
        position.y += velocity.y * dt;

        // Interpolación de tamaño
        currentSize = endSize + (startSize - endSize) * progress;

        // Interpolación lineal de color (RGBA)
        currentColor.x = endColor.x + (startColor.x - endColor.x) * progress;
        currentColor.y = endColor.y + (startColor.y - endColor.y) * progress;
        currentColor.z = endColor.z + (startColor.z - endColor.z) * progress;
        currentColor.w = endColor.w + (startColor.w - endColor.w) * progress;
    }
}