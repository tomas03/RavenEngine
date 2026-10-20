package org.ravenEngine.renderer.animation;

import org.ravenEngine.renderer.Sprite;

public class Frame {
    public Sprite sprite;
    public float frameTime;

    public Frame(Sprite sprite, float frameTime) {
        this.sprite = sprite;
        this.frameTime = frameTime;
    }

}
