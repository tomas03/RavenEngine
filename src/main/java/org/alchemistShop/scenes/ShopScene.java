package org.alchemistShop.scenes;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.ravenEngine.components.SpriteRenderer;
import org.ravenEngine.components.Transform;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.renderer.Texture;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import static org.lwjgl.glfw.GLFW.*;

public class ShopScene extends Scene {
    @Override
    public void init() {
        Texture itemsAtlas = new Texture("/textures/items_alquimia.png");

        // Calculamos el tamaño de cada celda: son 4 columnas y 2 filas
        int tileWidth = itemsAtlas.getWidth() / 4;
        int tileHeight = itemsAtlas.getHeight() / 2;

        System.out.println("Atlas size: " + itemsAtlas.getWidth() + "x" + itemsAtlas.getHeight());
        System.out.println("Tile size calculado: " + tileWidth + "x" + tileHeight);

        Spritesheet sheet = new Spritesheet(itemsAtlas, tileWidth, tileHeight, 8, 0);


        // --- FILA DE POCIONES ---
        GameObject pocionRoja = new GameObject("PocionRoja", new Transform(new org.joml.Vector2f(100, 150), new org.joml.Vector2f(48, 48)));
        pocionRoja.addComponent(new SpriteRenderer(sheet.getSprite(0)));
        this.addGameObjectToScene(pocionRoja);

        GameObject vialVerde = new GameObject("VialVerde", new Transform(new org.joml.Vector2f(160, 150), new org.joml.Vector2f(48, 48)));
        vialVerde.addComponent(new SpriteRenderer(sheet.getSprite(1)));
        this.addGameObjectToScene(vialVerde);

        GameObject matrazAzul = new GameObject("MatrazAzul", new Transform(new org.joml.Vector2f(220, 150), new org.joml.Vector2f(48, 48)));
        matrazAzul.addComponent(new SpriteRenderer(sheet.getSprite(2)));
        this.addGameObjectToScene(matrazAzul);

        GameObject elixir = new GameObject("ElixirPurpura", new Transform(new org.joml.Vector2f(280, 150), new org.joml.Vector2f(48, 48)));
        elixir.addComponent(new SpriteRenderer(sheet.getSprite(3)));
        this.addGameObjectToScene(elixir);

        // --- FILA DE INGREDIENTES ---
        GameObject hierbas = new GameObject("Hierbas", new Transform(new org.joml.Vector2f(100, 95), new org.joml.Vector2f(48, 48)));
        hierbas.addComponent(new SpriteRenderer(sheet.getSprite(4)));
        this.addGameObjectToScene(hierbas);

        GameObject raiz = new GameObject("Raiz", new Transform(new org.joml.Vector2f(160, 95), new org.joml.Vector2f(48, 48)));
        raiz.addComponent(new SpriteRenderer(sheet.getSprite(5)));
        this.addGameObjectToScene(raiz);

        GameObject hongo = new GameObject("Hongo", new Transform(new org.joml.Vector2f(220, 95), new org.joml.Vector2f(48, 48)));
        hongo.addComponent(new SpriteRenderer(sheet.getSprite(6)));
        this.addGameObjectToScene(hongo);

        GameObject frascoVacio = new GameObject("FrascoVacio", new Transform(new org.joml.Vector2f(280, 95), new org.joml.Vector2f(48, 48)));
        frascoVacio.addComponent(new SpriteRenderer(sheet.getSprite(7)));
        this.addGameObjectToScene(frascoVacio);

        // Mostrador alargado abajo
        GameObject mostrador = new GameObject("Mostrador", new Transform(new org.joml.Vector2f(80, 50), new org.joml.Vector2f(260, 45)));
        mostrador.addComponent(new SpriteRenderer(new org.joml.Vector4f(0.45f, 0.25f, 0.1f, 1.0f)));
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