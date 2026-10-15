package org.ravenEngine;

import org.ravenEngine.core.Engine;
import org.alchemistShop.scenes.ShopScene;

public class App {
    public static void main(String[] args) {
        System.out.println("Iniciando RavenEngine IDE...");

        // Instanciamos el motor configurado para desarrollo
        Engine engine = new Engine("RavenEngine - Editor IDE", 1280, 720);

        engine.start(new ShopScene());
        // Arrancamos el bucle del motor con la UI de ImGui activa
        engine.runLoop();
    }
}