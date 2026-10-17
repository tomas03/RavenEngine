package org.ravenEngine.core;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.editor.ImGuiLayer;
import org.ravenEngine.io.MouseListener;
import org.ravenEngine.renderer.DebugDraw;
import org.ravenEngine.renderer.FrameBuffer;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public class Engine {
    private final Window window;
    private boolean isRunning;
    private static Scene currentScene;
    private FrameBuffer framebuffer;
    //Estados de simulación
    private static boolean isPlaying = false;
    private static boolean isPaused = false;

    //CONSTRUCTORES
    public Engine(String title, int width, int height) {
        this.window = new Window("RavenEngine - AlquimistShop", 1280, 720);
    }


    public static void changeScene(Scene newScene) {
        currentScene = newScene;
        currentScene.init(); // Arranca la carga de esa escena específica
        currentScene.start();

    }

    //FUNCIONES
    public void start(Scene initialScene) {
        window.init();

        this.framebuffer = new FrameBuffer(1280, 720);

        isRunning = true;
        changeScene(initialScene);

        // Bucle del motor
        runLoop();

        // Limpieza de FrameBuffer
        if (framebuffer != null) {
            framebuffer.destroy();
        }

        // Limpieza de ventana y GLFW
        cleanup();

        // Mata el proceso de Java al instante: no quedan procesos zombies en RAM
        System.exit(0);
    }

    private void update(float dt) {
        // Aca van a actualizarse las escenas
    }

    private void render() {
        // Aca van a dibujarse las escenas
    }

    public void runLoop() {
        double lastFrameTime = glfwGetTime();
        while (isRunning && !window.shouldClose()) {
            double currentFrameTime = glfwGetTime();
            float deltaTime = (float) (currentFrameTime - lastFrameTime);
            lastFrameTime = currentFrameTime;

            DebugDraw.beginFrame();

            framebuffer.bind();
            glClearColor(0.08f, 0.09f, 0.12f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            ImGuiLayer gui = window.getImGuiLayer();
            if (gui != null && gui.getSelectedGameObject() != null) {
                GameObject selected = gui.getSelectedGameObject();
                if (selected.transform != null) {
                    Vector2f min = new Vector2f(selected.transform.pos.x, selected.transform.pos.y);
                    Vector2f max = new Vector2f(
                            selected.transform.pos.x + selected.transform.scale.x,
                            selected.transform.pos.y + selected.transform.scale.y
                    );
                    DebugDraw.addBox2D(min, max, new Vector3f(1.0f, 0.84f, 0.0f));
                }
            }

            if (currentScene != null) {
                if (isPlaying && !isPaused) {
                    currentScene.update(deltaTime);
                }
                currentScene.render();
            }
            framebuffer.unbind();

            glClearColor(0.15f, 0.15f, 0.18f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            // Si la ventana pidió cerrarse mientras se procesaban las colisiones o inputs, salimos
            if (window.shouldClose()) {
                break;
            }

            window.update(deltaTime, framebuffer, currentScene);
            MouseListener.endFrame();
        }
    }

    private void cleanup() {
        window.cleanup();
    }

    //GETTER Y SETTER
    public static Scene getCurrentScene(){
        return currentScene;
    }

    public static boolean IsPlaying() {
        return isPlaying;
    }

    public static void setPlaying(boolean isPlaying) {
        Engine.isPlaying = isPlaying;
    }

    public static boolean IsPaused() {
        return isPaused;
    }

    public static void setPaused(boolean isPaused) {
        Engine.isPaused = isPaused;
    }
}