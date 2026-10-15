package org.ravenEngine.components;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.renderer.DebugDraw;
import org.ravenEngine.scene.Component;

public class BoxCollider2D extends Component {
    private Vector2f size;
    private Vector2f offset;
    private boolean isTrigger = false;
    private boolean isStatic = false;

    // Colores de Debug: Verde para sólidos, Amarillo para triggers
    private static final Vector3f SOLID_COLOR = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Vector3f TRIGGER_COLOR = new Vector3f(1.0f, 0.85f, 0.0f);

    public BoxCollider2D(Vector2f size) {
        this(size, new Vector2f(0.0f, 0.0f), true,false);
    }

    public BoxCollider2D(Vector2f size, Vector2f offset) {
        this(size, offset, false,false);
    }

    public BoxCollider2D(Vector2f size, Vector2f offset, boolean isTrigger, boolean isStatic) {
        this.size = size;
        this.offset = offset;
        this.isTrigger = isTrigger;
        this.isStatic = isStatic;
    }

    @Override
    public void update(float dt) {
        Vector2f min = new Vector2f(getMinX(), getMinY());
        Vector2f max = new Vector2f(getMaxX(), getMaxY());

        // El gizmo cambia de color automáticamente según el tipo de collider
        Vector3f color = isTrigger ? TRIGGER_COLOR : SOLID_COLOR;
        DebugDraw.addBox2D(min, max, color);
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

    public Vector2f getSize() { return size; }
    public Vector2f getOffset() { return offset; }
}