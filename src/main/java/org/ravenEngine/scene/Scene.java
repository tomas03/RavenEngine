package org.ravenEngine.scene;

import org.ravenEngine.renderer.Camera;

public abstract class Scene {
    protected Camera camera;
    public Scene(){
        this.camera = new Camera(new org.joml.Vector2f());
    }

    public abstract void init();
    public abstract void update(float dt);
    public abstract void render();

    public Camera getCamera() {
        return camera;
    }
}
