package org.ravenEngine.io;

import static org.lwjgl.glfw.GLFW.*;

public class KeyListener {
    private static KeyListener instance;
    private final boolean[] keyPressed = new boolean[350];
    private final boolean[] keyBeginPress = new boolean[350];

    private KeyListener(){
    }

    public static KeyListener get(){
        if(KeyListener.instance == null){
            KeyListener.instance = new KeyListener();
        }
        return KeyListener.instance;
    }

    //Callback nativo que llama GLFW ante cada tecla
    public static void keyCallback(long window, int key, int scancode, int action, int mods){
        if(key>=0 && key< get().keyPressed.length){
            if(action == GLFW_PRESS){
                get().keyPressed[key] = true;
                get().keyBeginPress[key] = true;
            }else if (action == GLFW_RELEASE){
                get().keyPressed[key]=false;
            }
        }
    }

    //metodo de consulta en juego:KeyListener.isKeyPressed(GLFW_KEY_W)
    public static boolean isKeyPressed(int keyCode){
        if(keyCode>0 && keyCode<get().keyPressed.length){
            return get().keyPressed[keyCode];
        }
        return false;
    }

    //Click único para menus,diálogos, etc
    public static boolean keyBeginPress(int keyCode){
        if (keyCode >= 0 && keyCode < get().keyBeginPress.length) {
            boolean result = get().keyBeginPress[keyCode];
            if (result) {
                get().keyBeginPress[keyCode] = false; // Se consume en el frame
            }
            return result;
        }
        return false;
    }
}
