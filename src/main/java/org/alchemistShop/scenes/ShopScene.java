package org.alchemistShop.scenes;

import org.alchemistShop.entities.PlayerController;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.ravenEngine.components.BoxCollider2D;
import org.ravenEngine.components.CameraFollow;
import org.ravenEngine.components.SpriteRenderer;
import org.ravenEngine.components.Transform;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.renderer.Texture;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;
import org.ravenEngine.util.AssetPool;

import java.util.Vector;

import static org.lwjgl.glfw.GLFW.*;

public class ShopScene extends Scene {
    @Override
    public void init() {

        // Obtenemos la textura única desde el AssetPool
        Texture charSheetTex = AssetPool.getTexture("/textures/Personaje.png");
        Spritesheet playerSprites = new Spritesheet(charSheetTex, 32, 32, 16, 0);

        // Si los ítems de alquimia son de 16x16:
        Texture shopSheetTex = AssetPool.getTexture("/textures/items_alquimia.png");
        Spritesheet propsSprites = new Spritesheet(shopSheetTex, 167, 186, 8, 0);




        Vector2f viewportSize = new Vector2f(640.0f,360.0f);


        // --- FILA DE POCIONES ---
        GameObject pocionRoja = new GameObject("PocionRoja", new Transform(new org.joml.Vector2f(100, 150), new org.joml.Vector2f(48, 48),1));
        pocionRoja.addComponent(new SpriteRenderer(propsSprites.getSprite(0)));
        this.addGameObjectToScene(pocionRoja);

        GameObject vialVerde = new GameObject("VialVerde", new Transform(new org.joml.Vector2f(160, 150), new org.joml.Vector2f(48, 48),1));
        vialVerde.addComponent(new SpriteRenderer(propsSprites.getSprite(1)));
        this.addGameObjectToScene(vialVerde);

        GameObject matrazAzul = new GameObject("MatrazAzul", new Transform(new org.joml.Vector2f(220, 150), new org.joml.Vector2f(48, 48),1));
        matrazAzul.addComponent(new SpriteRenderer(propsSprites.getSprite(2)));
        this.addGameObjectToScene(matrazAzul);

        GameObject elixir = new GameObject("ElixirPurpura", new Transform(new org.joml.Vector2f(280, 150), new org.joml.Vector2f(48, 48),1));
        elixir.addComponent(new SpriteRenderer(propsSprites.getSprite(3)));
        this.addGameObjectToScene(elixir);

        // --- FILA DE INGREDIENTES ---
        GameObject hierbas = new GameObject("Hierbas", new Transform(new org.joml.Vector2f(100, 95), new org.joml.Vector2f(48, 48),1));
        hierbas.addComponent(new SpriteRenderer(propsSprites.getSprite(4)));
        this.addGameObjectToScene(hierbas);

        GameObject raiz = new GameObject("Raiz", new Transform(new org.joml.Vector2f(160, 95), new org.joml.Vector2f(48, 48),1));
        raiz.addComponent(new SpriteRenderer(propsSprites.getSprite(5)));
        this.addGameObjectToScene(raiz);

        GameObject hongo = new GameObject("Hongo", new Transform(new org.joml.Vector2f(220, 95), new org.joml.Vector2f(48, 48),1));
        hongo.addComponent(new SpriteRenderer(propsSprites.getSprite(6)));
        this.addGameObjectToScene(hongo);

        GameObject frascoVacio = new GameObject("FrascoVacio", new Transform(new org.joml.Vector2f(280, 95), new org.joml.Vector2f(48, 48),1));
        frascoVacio.addComponent(new SpriteRenderer(propsSprites.getSprite(7)));
        this.addGameObjectToScene(frascoVacio);

        // Mostrador alargado abajo
        GameObject mostrador = new GameObject("Mostrador", new Transform(new org.joml.Vector2f(80, 50), new org.joml.Vector2f(260, 45),1));
        mostrador.addComponent(new SpriteRenderer(new org.joml.Vector4f(0.45f, 0.25f, 0.1f, 1.0f)));
        //COLISIONES
        Vector2f mostradorColliderOffset = new Vector2f(0.0f,0.0f);
        BoxCollider2D mostradorCollider = new BoxCollider2D(
                new Vector2f(mostrador.transform.scale.x, 40),
                mostradorColliderOffset,
                false,
                true // <-- ¡ESTÁTICO!
        );
        mostrador.addComponent(mostradorCollider);
        this.addGameObjectToScene(mostrador);

        //crear GO del pj
        GameObject jugador = new GameObject("RavenTom",new Transform(new org.joml.Vector2f(360,95),new org.joml.Vector2f(48,54),1));
        jugador.addComponent(new SpriteRenderer(playerSprites.getSprite(0)));
        jugador.addComponent(new PlayerController(playerSprites));
        jugador.addComponent(new CameraFollow(this.camera,viewportSize,6.0f));
        //COLISIONES
        Vector2f playerColliderSize = new Vector2f(24.0f,34.0f);
        Vector2f playerColliderOffset = new Vector2f(12.0f,12.0f);
        jugador.addComponent(new BoxCollider2D(playerColliderSize,playerColliderOffset));
        this.addGameObjectToScene(jugador);


    }

    @Override
    public void update(float dt) {
        super.update(dt);
    }
}