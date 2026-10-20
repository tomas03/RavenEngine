package org.ravenEngine.renderer.animation;

import org.ravenEngine.renderer.Sprite;

import java.util.ArrayList;
import java.util.List;

public class Animation {
    public String title;
    public List<Frame> frames = new ArrayList<>();
    public boolean doesLoop = true;

    private int currentFrameIndex = 0;
    private float timeTracker = 0.0f;

    public Animation(String title, boolean doesLoop) {
        this.title = title;
        this.doesLoop = doesLoop;
    }

    public void addFrame(Sprite sprite, float frameTime){
        this.frames.add(new Frame(sprite, frameTime));
    }

    public void update(float dt) {
        if (frames.isEmpty()) return;

        timeTracker += dt;
        Frame currentFrame = frames.get(currentFrameIndex);

        if (timeTracker >= currentFrame.frameTime) {
            timeTracker -= currentFrame.frameTime; // <-- RESTAR EL TIEMPO TRANSCURRIDO
            currentFrameIndex++;
            if (currentFrameIndex >= frames.size()) {
                if (doesLoop) {
                    currentFrameIndex = 0;
                } else {
                    currentFrameIndex = frames.size() - 1;
                }
            }
        }
    }

    public Sprite getCurrentSprite(){
        if (frames.isEmpty()) return null;
        return frames.get(currentFrameIndex).sprite;
    }

    public void reset(){
        this.currentFrameIndex=0;
        this.timeTracker = 0.0f;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public void setFrames(List<Frame> frames) {
        this.frames = frames;
    }

    public boolean isDoesLoop() {
        return doesLoop;
    }

    public void setDoesLoop(boolean doesLoop) {
        this.doesLoop = doesLoop;
    }
}
