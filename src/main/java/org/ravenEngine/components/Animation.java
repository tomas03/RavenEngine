package org.ravenEngine.components;

import org.ravenEngine.renderer.Sprite;

import java.util.List;

public class Animation {
    private final List<Sprite> frames;
    private final float frameTime;
    private float timer = 0.0f;
    private int currentFrame = 0;

    public Animation(List<Sprite> frames, float frameTime) {
        this.frames = frames;
        this.frameTime = frameTime;
    }

    public void update(float dt){
        if (frames.size()<=1) return;

        timer +=dt;
        if (timer>=frameTime){
            timer = 0.0f;
            currentFrame = (currentFrame +1) % frames.size();
        }
    }

    public Sprite getCurrentSprite(){
        return frames.get(currentFrame);
    }

    public void reset(){
        this.timer = 0.0f;
        this.currentFrame = 0;
    }
}
