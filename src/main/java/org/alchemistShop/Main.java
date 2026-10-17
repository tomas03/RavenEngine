package org.alchemistShop;

import org.alchemistShop.assets.scenes.ShopScene;
import org.ravenEngine.core.Engine;


public class Main {
    public static void main(String[] args){
        Engine engine = new Engine("Alchemist Shop",1280,720);
        engine.start(new ShopScene());
    }
}
