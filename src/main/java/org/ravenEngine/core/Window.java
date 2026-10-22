package org.ravenEngine.core;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.ravenEngine.editor.ImGuiLayer;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.io.MouseListener;
import org.ravenEngine.renderer.FrameBuffer;
import org.ravenEngine.scene.Scene;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private final String title;
    private int width;
    private int height;
    private long windowHandle;

    // Capa de interfaz del editor (ImGui)
    private ImGuiLayer imGuiLayer;

    // Control de FPS y telemetría
    private double lastTime = 0.0;
    private int frameCount = 0;

    // =========================================================================
    // CONSTRUCTORES
    // =========================================================================
    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================
    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Fallo al crear la ventana de GLFW");
        }

        // Cargar ícono de la ventana desde la configuración del proyecto
        setWindowIcon(ProjectSettings.get().iconPath);

        // Callbacks de periféricos
        glfwSetCursorPosCallback(windowHandle, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(windowHandle, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(windowHandle, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(windowHandle, KeyListener::keyCallback);

        // Actualizar dimensiones internas si el usuario redimensiona la ventana
        glfwSetFramebufferSizeCallback(windowHandle, (win, newWidth, newHeight) -> {
            this.width = newWidth;
            this.height = newHeight;
        });

        glfwMakeContextCurrent(windowHandle);

        // VSync según la configuración del proyecto
        glfwSwapInterval(ProjectSettings.get().vSync ? 1 : 0);
        glfwShowWindow(windowHandle);

        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Solo inicializamos ImGui si estamos en modo editor
        if (Engine.getMode() == EngineMode.EDITOR) {
            this.imGuiLayer = new ImGuiLayer(windowHandle);
            this.imGuiLayer.init();
        }

        lastTime = glfwGetTime();
    }

    // =========================================================================
    // CICLOS DE ACTUALIZACIÓN (EDITOR Y RUNTIME)
    // =========================================================================
    public void update(float dt, FrameBuffer framebuffer, Scene currentScene) {
        // Restaurar el viewport al tamaño total de la ventana para la UI del editor
        glViewport(0, 0, width, height);

        if (imGuiLayer != null) {
            imGuiLayer.update(dt, framebuffer, currentScene);
        }

        glfwSwapBuffers(windowHandle);
        glfwPollEvents();

        // Actualización dinámica del título de la ventana con contador de FPS
        frameCount++;
        double currentTime = glfwGetTime();
        if (currentTime - lastTime >= 0.25) {
            int fps = (int) (frameCount / (currentTime - lastTime));
            String gameName = ProjectSettings.get().gameTitle;
            String ideTitle = "RavenEngine IDE - [" + gameName + "] | FPS: " + fps;
            glfwSetWindowTitle(windowHandle, ideTitle);
            frameCount = 0;
            lastTime = currentTime;
        }
    }

    public void updateRuntime(float deltaTime) {
        // En modo runtime / standalone limpiamos y refrescamos a pantalla completa
        glViewport(0, 0, width, height);
        glfwSwapBuffers(this.windowHandle);
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    // =========================================================================
    // ÍCONOS Y RECURSOS
    // =========================================================================
    private void setWindowIcon(String path) {
        if (path == null || path.trim().isEmpty()) return;

        File file = new File(path);
        if (!file.exists()) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer comp = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);

            ByteBuffer image = STBImage.stbi_load(file.getAbsolutePath(), w, h, comp, 4);
            if (image != null) {
                GLFWImage.Buffer imageBuffer = GLFWImage.malloc(1, stack);
                imageBuffer.position(0);
                imageBuffer.width(w.get(0));
                imageBuffer.height(h.get(0));
                imageBuffer.pixels(image);

                glfwSetWindowIcon(this.windowHandle, imageBuffer);
                STBImage.stbi_image_free(image);
            }
        } catch (Exception e) {
            System.err.println("[Window] No se pudo cargar el ícono: " + e.getMessage());
        }
    }

    // =========================================================================
    // LIMPIEZA
    // =========================================================================
    public void cleanup() {
        if (imGuiLayer != null) {
            imGuiLayer.destroy();
        }
        Callbacks.glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();

        var errorCb = glfwSetErrorCallback(null);
        if (errorCb != null) {
            errorCb.free();
        }
    }

    // =========================================================================
    // GETTERS
    // =========================================================================
    public long getWindowHandle() {
        return windowHandle;
    }

    public ImGuiLayer getImGuiLayer() {
        return imGuiLayer;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}