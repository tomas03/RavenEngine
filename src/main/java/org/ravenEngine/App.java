package org.ravenEngine;

import org.ravenEngine.core.Engine;
import org.alchemistShop.assets.scenes.ShopScene;
import org.ravenEngine.editor.EngineConsole;

public class App {
    public static void main(String[] args) {
        boolean isRelease = false;
        for (String arg : args) {
            if ("--release".equalsIgnoreCase(arg)) {
                isRelease = true;
                break;
            }
        }

        // Instanciamos el motor configurado para desarrollo
        Engine engine = new Engine("RavenEngine - Editor IDE", 1280, 720);
        EngineConsole.init();
        engine.start(new ShopScene());
    }
}