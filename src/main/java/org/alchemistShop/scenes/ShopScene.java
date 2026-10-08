package org.alchemistShop.scenes;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.ravenEngine.components.SpriteRenderer;
import org.ravenEngine.components.Transform;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.renderer.Texture;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import static org.lwjgl.glfw.GLFW.*;

public class ShopScene extends Scene {
    @Override
    public void init() {
        Texture frascoTex = new Texture("/textures/Frasco.png");

        // Frasco 1 en (100, 100) tamaño 64x64
        GameObject frasco1 = new GameObject("Frasco1", new Transform(new Vector2f(100, 100), new Vector2f(64, 64)));
        frasco1.addComponent(new SpriteRenderer(frascoTex));
        this.addGameObjectToScene(frasco1);

        // Frasco 2 al lado en (200, 100) tamaño 64x64
        GameObject frasco2 = new GameObject("Frasco2", new Transform(new Vector2f(200, 100), new Vector2f(64, 64)));
        frasco2.addComponent(new SpriteRenderer(frascoTex));
        this.addGameObjectToScene(frasco2);

        // Un mostrador de madera (cuadrado marrón sin textura de 180x32)
        GameObject mostrador = new GameObject("Mostrador", new Transform(new Vector2f(80, 60), new Vector2f(190, 32)));
        mostrador.addComponent(new SpriteRenderer(new Vector4f(0.45f, 0.25f, 0.1f, 1.0f)));
        this.addGameObjectToScene(mostrador);
    }

    @Override
    public void update(float dt) {
        super.update(dt);

        float cameraSpeed = 300.0f;
        if (KeyListener.isKeyPressed(GLFW_KEY_W)) camera.position.y += cameraSpeed * dt;
        if (KeyListener.isKeyPressed(GLFW_KEY_S)) camera.position.y -= cameraSpeed * dt;
        if (KeyListener.isKeyPressed(GLFW_KEY_D)) camera.position.x += cameraSpeed * dt;
        if (KeyListener.isKeyPressed(GLFW_KEY_A)) camera.position.x -= cameraSpeed * dt;
    }
}