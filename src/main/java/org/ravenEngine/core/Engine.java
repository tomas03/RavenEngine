package org.ravenEngine.core;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.audio.AudioManager;
import org.ravenEngine.editor.ImGuiLayer;
import org.ravenEngine.io.MouseListener;
import org.ravenEngine.renderer.DebugDraw;
import org.ravenEngine.renderer.FrameBuffer;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;
import org.ravenEngine.util.AssetPool;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public class Engine {
    private final Window window;
    private boolean isRunning;
    private static Scene currentScene;
    private FrameBuffer framebuffer;

    // Estados de simulación
    private static boolean isPlaying = false;
    private static boolean isPaused = false;
    private static EngineMode currentMode = EngineMode.EDITOR;

    // CONSTRUCTORES
    public Engine() {
        ProjectSettings settings = ProjectSettings.get();
        this.window = new Window(settings.gameTitle, settings.windowWidth, settings.windowHeight);
    }

    public Engine(String title, int width, int height) {
        this.window = new Window(title, width, height);
    }

    public static void changeScene(Scene newScene) {
        currentScene = newScene;
        currentScene.init();
        currentScene.start();
    }

    // FUNCIONES
    public void start(Scene initialScene) {
        ProjectSettings settings = ProjectSettings.get();

        window.init();
        AudioManager.init();

        // El FrameBuffer toma las dimensiones configuradas en el proyecto
        this.framebuffer = new FrameBuffer(settings.windowWidth, settings.windowHeight);

        isRunning = true;
        changeScene(initialScene);

        // Cargamos automáticamente la escena por defecto definida en ProjectSettings
        if (settings.defaultScenePath != null && !settings.defaultScenePath.trim().isEmpty()) {
            loadScene(settings.defaultScenePath);
        }

        // Bucle del motor
        runLoop();

        // Limpieza de FrameBuffer
        if (framebuffer != null) {
            framebuffer.destroy();
        }

        cleanup();
        System.exit(0);
    }

    public void runLoop() {
        double lastFrameTime = glfwGetTime();

        while (isRunning && !window.shouldClose()) {
            double currentFrameTime = glfwGetTime();
            float deltaTime = (float) (currentFrameTime - lastFrameTime);
            lastFrameTime = currentFrameTime;

            if (currentMode == EngineMode.EDITOR) {
                // ==========================================
                // MODO EDITOR (Con FrameBuffer e ImGui)
                // ==========================================
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

                if (window.shouldClose()) break;

                window.update(deltaTime, framebuffer, currentScene);
                MouseListener.endFrame();

            } else {
                // ==========================================
                // MODO RUNTIME / RELEASE (Juego Limpio)
                // ==========================================
                glClearColor(0.08f, 0.09f, 0.12f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);

                if (currentScene != null) {
                    currentScene.update(deltaTime);
                    currentScene.render();
                }

                window.updateRuntime(deltaTime);
                MouseListener.endFrame();
            }
        }
    }

    public static void loadScene(String filePath) {
        if (currentScene != null) {
            if (isPlaying) {
                isPlaying = false;
                isPaused = false;
            }
            currentScene.load(filePath);
        } else {
            System.err.println("[Engine] No hay escena activa para cargar: " + filePath);
        }
    }

    private void cleanup() {
        AssetPool.cleanUp();
        AudioManager.cleanup();
        window.cleanup();
    }

    // GETTERS Y SETTERS
    public static Scene getCurrentScene() {
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

    public static EngineMode getMode() {
        return currentMode;
    }

    public static void setMode(EngineMode mode) {
        currentMode = mode;
        if (mode == EngineMode.RUNTIME) {
            isPlaying = true;
            isPaused = false;
        }
    }
}