package org.ravenEngine.components;

import org.joml.Vector2f;
import org.ravenEngine.scene.GameObject;

public class Transform {

    public Vector2f pos;
    public Vector2f scale;
    public float rotation = 0.0f; // Rotación local en grados (0.0f a 360.0f)
    public int zIndex = 0;

    // Referencia al GameObject dueño (transient para Gson)
    public transient GameObject gameObject = null;

    public Transform() {
        init(new Vector2f(), new Vector2f(), 0.0f);
    }

    public Transform(Vector2f pos) {
        init(pos, new Vector2f(32.0f, 32.0f), 0.0f);
    }

    public Transform(Vector2f pos, Vector2f scale) {
        init(pos, scale, 0.0f);
    }

    public Transform(Vector2f pos, Vector2f scale, float rotation) {
        init(pos, scale, rotation);
    }

    public void init(Vector2f pos, Vector2f scale, float rotation) {
        this.pos = pos;
        this.scale = scale;
        this.rotation = rotation;
    }

    // ==========================================
    // CÁLCULO DE COORDENADAS GLOBALES (MUNDO)
    // ==========================================
    public Vector2f getWorldPos() {
        if (gameObject != null && gameObject.getParent() != null) {
            Transform parentTransform = gameObject.getParent().transform;
            if (parentTransform != null) {
                Vector2f parentWorld = parentTransform.getWorldPos();
                float parentRotRad = (float) Math.toRadians(parentTransform.getWorldRotation());

                float cos = (float) Math.cos(parentRotRad);
                float sin = (float) Math.sin(parentRotRad);

                // Rotar el offset local respecto al ángulo del padre
                float rotatedX = (pos.x * cos) - (pos.y * sin);
                float rotatedY = (pos.x * sin) + (pos.y * cos);

                return new Vector2f(parentWorld.x + rotatedX, parentWorld.y + rotatedY);
            }
        }
        return new Vector2f(pos.x, pos.y);
    }

    public float getWorldRotation() {
        if (gameObject != null && gameObject.getParent() != null && gameObject.getParent().transform != null) {
            return (gameObject.getParent().transform.getWorldRotation() + this.rotation) % 360.0f;
        }
        return this.rotation;
    }

    public Transform copy() {
        Transform t = new Transform(new Vector2f(this.pos), new Vector2f(this.scale), this.rotation);
        t.zIndex = this.zIndex;
        t.gameObject = this.gameObject;
        return t;
    }

    public void copy(Transform to) {
        to.pos.set(this.pos);
        to.scale.set(this.scale);
        to.rotation = this.rotation;
        to.zIndex = this.zIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Transform)) return false;
        Transform t = (Transform) o;
        return t.pos.equals(this.pos) && t.scale.equals(this.scale) && Float.compare(t.rotation, this.rotation) == 0 && t.zIndex == this.zIndex;
    }
}