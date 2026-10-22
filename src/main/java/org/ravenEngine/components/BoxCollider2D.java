package org.ravenEngine.components;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class BoxCollider2D extends Component {
    private Vector2f size = new Vector2f(32.0f, 32.0f);
    private Vector2f offset = new Vector2f(0.0f, 0.0f);
    private float rotation = 0.0f; // <-- Esta es la variable que cambia desde el Inspector
    private boolean isTrigger = false;
    private boolean isStatic = false;

    private static final Vector3f SOLID_COLOR = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Vector3f TRIGGER_COLOR = new Vector3f(1.0f, 0.85f, 0.0f);

    public BoxCollider2D() {}

    public BoxCollider2D(Vector2f size) {
        this.size = size;
    }

    public BoxCollider2D(Vector2f size, Vector2f offset) {
        this.size = size;
        this.offset = offset;
    }

    @Override
    public void update(float dt) {
        // El dibujo de debug se centraliza en Scene.java cuando debugMode == true
    }

    // ========================================================
    // LÍMITES AABB PARA EL MOTOR DE FÍSICAS (Physics2D)
    // ========================================================
    public float getMinX() {
        return gameObject.transform.pos.x + offset.x;
    }

    public float getMaxX() {
        return gameObject.transform.pos.x + offset.x + size.x;
    }

    public float getMinY() {
        return gameObject.transform.pos.y + offset.y;
    }

    public float getMaxY() {
        return gameObject.transform.pos.y + offset.y + size.y;
    }

    // ========================================================
    // GETTERS Y SETTERS
    // ========================================================
    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public boolean isTrigger() {
        return isTrigger;
    }

    public void setTrigger(boolean trigger) {
        this.isTrigger = trigger;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public Vector2f getSize() {
        return size;
    }

    public void setSize(Vector2f size) {
        this.size = size;
    }

    public Vector2f getOffset() {
        return offset;
    }

    public void setOffset(Vector2f offset) {
        this.offset = offset;
    }
}