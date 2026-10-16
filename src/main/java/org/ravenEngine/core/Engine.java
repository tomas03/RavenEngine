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

        // 1. Instanciar el framebuffer AHORA que OpenGL ya existe en la GPU
        this.framebuffer = new FrameBuffer(1280, 720);

        isRunning = true;
        changeScene(initialScene);

        // 2. Recién acá arranca el loop del juego
        runLoop();

        // 3. Cuando se cierra la ventana, se limpia todoo
        if (framebuffer != null) {
            framebuffer.destroy();
        }
        cleanup();
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
            //Limpiar lineas de fotograma anterior SIEMPRE
            DebugDraw.beginFrame();
            //DIBUJAR LA ESCENA ADENTRO DEL FRAMEBUFFER
            framebuffer.bind();
            glClearColor(0.08f, 0.09f, 0.12f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            //CREAR VENTANA
            ImGuiLayer gui = window.getImGuiLayer();
            // 1. Agregar la caja si hay un objeto seleccionado
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
            // 2. Renderizar sprites del juego
            if (currentScene != null) {
                if (isPlaying && !isPaused) {
                    currentScene.update(deltaTime);
                }
                currentScene.render();
            }
            framebuffer.unbind();
            // 2. LIMPIAR LA PANTALLA PRINCIPAL DEL MONITOR
            glClearColor(0.15f, 0.15f, 0.18f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            // 3. DIBUJAR IMGUI (QUE CONTIENE LA VENTANA CON LA TEXTURA)
            window.update(deltaTime, framebuffer,currentScene);
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