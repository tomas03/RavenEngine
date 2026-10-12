package org.ravenEngine.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.io.MouseListener;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private final String title;
    private int width;
    private int height;
    private Long windowHandle;

    // Control de FPS
    private double lastTime = 0.0;
    private int frameCount = 0;

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo incializar GLFW");
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

        glfwSetCursorPosCallback(windowHandle, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(windowHandle, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(windowHandle, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(windowHandle, KeyListener::keyCallback);

        glfwMakeContextCurrent(windowHandle);

        // VSync activado (topeado a la tasa de refresco del monitor)
        glfwSwapInterval(1);

        glfwShowWindow(windowHandle);

        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Guardamos el tiempo de inicio
        lastTime = glfwGetTime();
    }

    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();

        // Acumulamos frames y medimos cada 0.25 segundos
        frameCount++;
        double currentTime = glfwGetTime();
        if (currentTime - lastTime >= 0.25) {
            int fps = (int) (frameCount / (currentTime - lastTime));
            glfwSetWindowTitle(windowHandle, title + " | FPS: " + fps);
            frameCount = 0;
            lastTime = currentTime;
        }
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void cleanup() {
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }
}