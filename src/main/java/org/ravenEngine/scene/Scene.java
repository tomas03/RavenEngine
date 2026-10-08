package org.ravenEngine.scene;

import org.ravenEngine.renderer.Camera;
import org.ravenEngine.renderer.Renderer;

import java.util.ArrayList;
import java.util.List;

public abstract class Scene {
    protected Camera camera;
    protected final List< GameObject > gameObjects = new ArrayList< >();
    protected Renderer renderer;
    private boolean isRunning = false;

    public Scene() {
        this.camera = new Camera(new org.joml.Vector2f(0.0f, 0.0f));
    }

    public abstract void init();

    public void start() {
        // Inicializamos el Renderer acá, cuando OpenGL ya está listo
        if (renderer == null) {
            this.renderer = new Renderer();
        }

        for (GameObject go : gameObjects) {
            go.start();
            this.renderer.add(go);
        }
        isRunning = true;
    }

    public void addGameObjectToScene(GameObject go) {
        gameObjects.add(go);
        if (isRunning) {
            go.start();
            this.renderer.add(go);
        }
    }

    public void update(float dt) {
        for (GameObject go : gameObjects) {
            go.update(dt);
        }
    }

    public void render() {
        if (this.renderer != null) {
            this.renderer.render(camera);
        }
    }

    public Camera getCamera() {
        return camera;
    }
}