package org.ravenEngine.components;

import org.joml.Vector2f;

public class Transform {
    public Vector2f pos; // o position según tu nomenclatura
    public Vector2f scale;
    public int zIndex;

    //CONSTRUCTORES
    public Transform() {
        init(new Vector2f(), new Vector2f(), 0);
    }

    public Transform(Vector2f pos) {
        init(pos, new Vector2f(), 0);
    }

    public Transform(Vector2f pos, Vector2f scale) {
        init(pos, scale, 0);
    }

    public Transform(Vector2f pos, Vector2f scale, int zIndex) {
        init(pos, scale, zIndex);
    }


    //INIT
    public void init(Vector2f pos, Vector2f scale, int zIndex) {
        this.pos = pos;
        this.scale = scale;
        this.zIndex = zIndex;
    }
}