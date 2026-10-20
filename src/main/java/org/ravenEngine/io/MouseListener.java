package org.ravenEngine.io;

import static org.lwjgl.glfw.GLFW.*;

public class MouseListener {
    private static MouseListener instance;
    private double scrollX, scrollY;
    private double xPos,yPos,lastX,lastY;
    private final boolean[] mouseButtonPressed = new boolean[9];
    private boolean isDragging;

    public MouseListener() {
        this.lastY = 0.0;
        this.lastX = 0.0;
        this.yPos = 0.0;
        this.xPos = 0.0;
        this.scrollY = 0.0;
        this.scrollX = 0.0;
    }

    public static MouseListener get(){
        if(MouseListener.instance == null){
            MouseListener.instance = new MouseListener();
        }
        return MouseListener.instance;
    }

    //posición X,Y del puntero en píxeles de la ventana
    public static void mousePosCallback(long window,double xpos, double ypos){
        get().lastX = get().xPos;
        get().lastY = get().yPos;
        get().xPos = xpos;
        get().yPos = ypos;

        //si se mueve con boton apretado significa que está arrastrando(arrastre de ingredientes)
        get().isDragging = get().mouseButtonPressed[0] || get().mouseButtonPressed[1] || get().mouseButtonPressed[2];

    }

    //clicks(0=izq,1=der,2=rueda)
    public static void mouseButtonCallback(long window, int button, int action, int mods){
        if(button<get().mouseButtonPressed.length){
            if(action == GLFW_PRESS){
                get().mouseButtonPressed[button] = true;
            }else if (action== GLFW_RELEASE){
                get().mouseButtonPressed[button] = false;
                get().isDragging = false;
            }
        }
    }

    //scroll de rueda
    public static void mouseScrollCallback(long window, double xOffset,double yOffset){
        get().scrollX = xOffset;
        get().scrollY = yOffset;
    }

    public static void endFrame(){
        get().scrollY = 0;
        get().scrollX = 0;
        get().lastX = get().xPos;
        get().lastY = get().yPos;
    }

    // Getters para consultar en el juego
    public static float getX() { return (float) get().xPos; }
    public static float getY() { return (float) get().yPos; }
    public static float getDx() { return (float) (get().lastX - get().xPos); }
    public static float getDy() { return (float) (get().lastY - get().yPos); }
    public static boolean isDragging() { return get().isDragging; }
    public static boolean mouseButtonDown(int button) {
        if (button < get().mouseButtonPressed.length) {
            return get().mouseButtonPressed[button];
        }
        return false;
    }
}
