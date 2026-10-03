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

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }
    public void init(){
        //Redirección de los errores nativos de C a la consola de Java
        GLFWErrorCallback.createPrint(System.err).set();
        //iniciar Libreria GLFW
        if(!glfwInit()){
            throw new IllegalStateException("No se pudo incializar GLFW");
        }

        //Reglas de creacion de ventana
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);//va a estar oculta hasta que termine de cargar todoo
        glfwWindowHint(GLFW_RESIZABLE,GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);//Usar openGL moderno

        //Creacion de ventana y guardado de puntero
        windowHandle = glfwCreateWindow(width,height,title,NULL,NULL);
        if(windowHandle == NULL){
            throw new RuntimeException("Fallo al crear la ventana de GLFW");
        }

        // Registrar callbacks de inputs
        glfwSetCursorPosCallback(windowHandle, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(windowHandle, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(windowHandle, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(windowHandle, KeyListener::keyCallback);

        //unir ventana a hilo de ejecución
        glfwMakeContextCurrent(windowHandle);

        //Activar VSync
        glfwSwapInterval(1);

        //Mostrar ventana
        glfwShowWindow(windowHandle);

        //detectar placa de video y cargar las funciones de OpenGL
        GL.createCapabilities();
    }
    public void update(){
        //intercambiar buffer de imagen
        glfwSwapBuffers(windowHandle);
        //Leemos los inputs
        glfwPollEvents();
    }
    public boolean shouldClose(){
        return glfwWindowShouldClose(windowHandle);
    }
    public void cleanup(){
        //rompemos todoo pero prolijo al salir
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }
}
