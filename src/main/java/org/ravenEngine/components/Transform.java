package org.ravenEngine.components;

import org.joml.Vector2f;

public class Transform {
    public Vector2f pos;
    public Vector2f scale;

    public Transform() {
        this.pos = new Vector2f();
        this.scale = new Vector2f();
    }

    public Transform(Vector2f pos, Vector2f scale) {
        this.pos = pos;
        this.scale = scale;
    }
}
