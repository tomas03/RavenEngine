package org.ravenEngine.core;

import org.ravenEngine.io.KeyListener;
import org.ravenEngine.io.MouseListener;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

public class Engine {
    private final Window window;
    private boolean isRunning;

    public Engine() {
        this.window =new Window("RavenEngine-AlquimistShop",1280,720);
    }
    public void start(){
        window.init();
        isRunning=true;
        runLoop();

        //cuando sale del loop limpiamos para que quede bonito
        window.cleanup();
    }

    private void runLoop(){
        //tomar el tiempo exacto antes de entrar al bucle
        double lastFrameTime = glfwGetTime();

        while(isRunning && !window.shouldClose()){
            //Calcular DeltaTime
            double currentFrameTime = glfwGetTime();
            float deltaTime =(float)(currentFrameTime - lastFrameTime);
            lastFrameTime = currentFrameTime;

            //Limpieza de pantalla
            //Borrar los dibujos del anterior frame y pintar de un color solido (Negro/gris oscuro)
            glClearColor(0.1f,0.1f,0.15f,1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            //LOGICA Y RENDER
            update(deltaTime);
            render();

            MouseListener.endFrame();

            //Actualizar ventana
            window.update();
        }
    }
    private void update(float dt){
        //Movimientos de pj, crecimiento de planta y paso de tiempo
        if (KeyListener.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE)) {
            System.out.println("Espacio presionado! dt: " + dt);
        }
        if (MouseListener.mouseButtonDown(0)) {
            System.out.println("Click en X: " + MouseListener.getX() + " | Y: " + MouseListener.getY());
        }
    }
    private void render(){
        //aca se hacen los dibujos con OpenGL
    }
    public static void main(String[] args){
        //Punto de entrada de todoo el programa
        Engine engine = new Engine();
        engine.start();
    }
}
