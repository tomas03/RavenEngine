package org.ravenEngine.core;

import org.ravenEngine.io.KeyListener;
import org.ravenEngine.io.MouseListener;
import org.ravenEngine.scene.Scene;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public class Engine {
    private final Window window;
    private boolean isRunning;
    private static Scene currentScene;

    public Engine(String title, int width, int height) {
        this.window = new Window("RavenEngine - AlquimistShop", 1280, 720);
    }

    public void start(Scene initialScene) {
        window.init();
        isRunning = true;
        changeScene(initialScene);
        runLoop();
        cleanup();
    }

    public static void changeScene(Scene newScene) {
        currentScene = newScene;
        currentScene.init(); // Arranca la carga de esa escena específica
    }

    private void runLoop() {
        double lastFrameTime = glfwGetTime();

        while (isRunning && !window.shouldClose()) {
            double currentFrameTime = glfwGetTime();
            float deltaTime = (float) (currentFrameTime - lastFrameTime);
            lastFrameTime = currentFrameTime;

            // Limpieza de pantalla (color oscuro de fondo)
            glClearColor(0.08f, 0.09f, 0.12f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            if (currentScene != null) {
                currentScene.update(deltaTime);
                currentScene.render();
            }

            window.update();
            MouseListener.endFrame();
        }
    }

    private void update(float dt) {
        // Aca van a actualizarse las escenas
    }

    private void render() {
        // Aca van a dibujarse las escenas
    }

    private void cleanup() {
        window.cleanup();
    }
}