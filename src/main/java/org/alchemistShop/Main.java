package org.alchemistShop;

import org.ravenEngine.core.Engine;
import org.alchemistShop.assets.scenes.ShopScene; // O la escena base que uses de contenedor

public class Main {
    public static void main(String[] args) {
        Engine engine = new Engine();
        engine.start(new ShopScene());
    }
}