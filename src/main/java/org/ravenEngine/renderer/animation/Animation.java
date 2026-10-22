package org.ravenEngine.renderer.animation;

import org.ravenEngine.renderer.texture.Sprite;

import java.util.ArrayList;
import java.util.List;

public class Animation {
    public String title = "default";
    public List< Frame > frames = new ArrayList< >();
    public boolean doesLoop = true;

    // Transient para que Gson no guarde el estado de reproducción en el JSON
    private transient int currentFrameIndex = 0;
    private transient float timeTracker = 0.0f;
    private transient boolean isFinished = false;

    // Constructor vacío obligatorio para Gson
    public Animation() {}

    public Animation(String title, boolean doesLoop) {
        this.title = title;
        this.doesLoop = doesLoop;
    }

    public Animation(String title, float fps, boolean doesLoop) {
        this.title = title;
        this.doesLoop = doesLoop;
        // Si ya tenés el método setFPS():
        setFPS(fps);
    }

    public void addFrame(Sprite sprite, float frameTime) {
        this.frames.add(new Frame(sprite, frameTime));
    }

    public void addFrame(Sprite sprite) {
        // Asigna el tiempo de cuadro basado en el FPS actual o usa ~0.1f (10 FPS) por defecto
        float defaultTime = (frames.isEmpty()) ? 0.1f : frames.get(0).frameTime;
        this.frames.add(new Frame(sprite, defaultTime));
    }

    public void update(float dt) {
        if (frames.isEmpty()) return;
        if (!doesLoop && isFinished) return;

        timeTracker += dt;

        // While para digerir caídas de FPS o micro-tirones sin desfasar la animación
        while (currentFrameIndex < frames.size() && timeTracker >= frames.get(currentFrameIndex).frameTime) {
            timeTracker -= frames.get(currentFrameIndex).frameTime;
            currentFrameIndex++;

            if (currentFrameIndex >= frames.size()) {
                if (doesLoop) {
                    currentFrameIndex = 0;
                } else {
                    currentFrameIndex = frames.size() - 1;
                    isFinished = true;
                    break;
                }
            }
        }
    }

    public Sprite getCurrentSprite() {
        if (frames.isEmpty()) return null;
        if (currentFrameIndex < 0 || currentFrameIndex >= frames.size()) {
            currentFrameIndex = 0;
        }
        return frames.get(currentFrameIndex).sprite;
    }

    public void reset() {
        this.currentFrameIndex = 0;
        this.timeTracker = 0.0f;
        this.isFinished = false;
    }
    public void setFPS(float fps) {
        float time = 1.0f / Math.max(0.001f, fps);
        for (Frame f : frames) {
            f.frameTime = time;
        }
    }

    public float getApproximateFPS() {
        if (frames.isEmpty() || frames.get(0).frameTime <= 0.0f) return 10.0f;
        return 1.0f / frames.get(0).frameTime;
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public void setCurrentFrameIndex(int index) {
        if (index >= 0 && index < frames.size()) {
            this.currentFrameIndex = index;
            this.timeTracker = 0.0f;
        }
    }

    public boolean isFinished() {
        return isFinished;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List< Frame > getFrames() {
        return frames;
    }

    public void setFrames(List< Frame > frames) {
        this.frames = frames;
    }

    public boolean isDoesLoop() {
        return doesLoop;
    }

    public void setDoesLoop(boolean doesLoop) {
        this.doesLoop = doesLoop;
    }
}