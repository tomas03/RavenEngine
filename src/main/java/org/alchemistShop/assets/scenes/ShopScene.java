package org.alchemistShop.assets.scenes;

import org.alchemistShop.assets.entities.ItemPickup;
import org.alchemistShop.assets.entities.PlayerController;
import org.joml.Vector2f;
import org.ravenEngine.components.*;
import org.ravenEngine.renderer.SpriteRenderer;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.renderer.Texture;
import org.ravenEngine.renderer.animation.Animation;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;
import org.ravenEngine.util.AssetPool;

public class ShopScene extends Scene {
    @Override
    public void init() {

        this.load("alchemistShop/assets/level1.json");

        if (this.gameObjects.isEmpty()) {
            // Obtenemos la textura única desde el AssetPool
            Texture charSheetTex = AssetPool.getTexture("/textures/Personaje.png");
            Spritesheet playerSprites = new Spritesheet(charSheetTex, 32, 32, 16, 0);

            // Si los ítems de alquimia son de 16x16:
            Texture shopSheetTex = AssetPool.getTexture("/textures/items_alquimia.png");
            Spritesheet propsSprites = new Spritesheet(shopSheetTex, 167, 186, 8, 0);




            Vector2f viewportSize = new Vector2f(640.0f,360.0f);



            //GENERACION DE PISO DE PRUEBA
            //1. crear el GO contenedor del mapa del piso
            //poner z-index en -1 para que quede atras de todoo
            GameObject tilemapObj = new GameObject("tilemap_piso",new Transform(new Vector2f(0,0)),-1);
            //2 Parametros columns,rows,tileWidth,tileHeight,path , numsprites y spacing
            // Pruebá con tiles de 32x32 o 16x16 en un mapa proporcional de 20x11
            Tilemap tilemap = new Tilemap(20, 11, 32, 32, "textures/wood_floor.png", 1, 0);

            // Marcamos el tile 1 como pared sólida
            tilemap.settileSolid(1, true);

// Pintamos una pared de 3 tiles al lado del mostrador
            tilemap.setTile(5, 5, 1);
            tilemap.setTile(6, 5, 1);
            tilemap.setTile(7, 5, 1);

            //un for que llena con el tile0(en este caso el piso)
            for (int x=0;x<15;x++){
                for (int y = 0;y<10;y++){
                    tilemap.setTile(x,y,0);
                }
            }


            tilemapObj.addComponent(tilemap);
            this.addGameObjectToScene(tilemapObj);




            // --- FILA DE POCIONES ---
            GameObject pocionRoja = new GameObject("PocionRoja", new Transform(new org.joml.Vector2f(100, 150), new org.joml.Vector2f(48, 48),1));
            pocionRoja.addComponent(new SpriteRenderer(propsSprites.getSprite(0)));
            pocionRoja.addComponent(new ItemPickup("Poción de Vida Menor",1));
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

            // --- CREAR GO DEL PJ ---
            GameObject jugador = new GameObject("RavenTom", new Transform(new org.joml.Vector2f(360, 170), new org.joml.Vector2f(48, 54), 1));
            jugador.addComponent(new SpriteRenderer(playerSprites.getSprite(1))); // Frame quieto mirando al frente

            // --- SISTEMA DE ANIMACIONES (3 frames por ciclo según tu spritesheet) ---
            SpriteAnimator animator = new SpriteAnimator();
            float frameTime = 0.15f; // Velocidad del paso

            // Caminar hacia abajo (sprites 0, 1, 2)
            Animation walkDown = new Animation("walk_down", true);
            walkDown.addFrame(playerSprites.getSprite(0), frameTime);
            walkDown.addFrame(playerSprites.getSprite(1), frameTime);
            walkDown.addFrame(playerSprites.getSprite(2), frameTime);
            animator.addAnimation(walkDown);

            // Caminar hacia la izquierda (sprites 3, 4, 5)
            Animation walkLeft = new Animation("walk_left", true);
            walkLeft.addFrame(playerSprites.getSprite(3), frameTime);
            walkLeft.addFrame(playerSprites.getSprite(4), frameTime);
            walkLeft.addFrame(playerSprites.getSprite(5), frameTime);
            animator.addAnimation(walkLeft);

            // Caminar hacia la derecha (sprites 6, 7, 8)
            Animation walkRight = new Animation("walk_right", true);
            walkRight.addFrame(playerSprites.getSprite(6), frameTime);
            walkRight.addFrame(playerSprites.getSprite(7), frameTime);
            walkRight.addFrame(playerSprites.getSprite(8), frameTime);
            animator.addAnimation(walkRight);

            // Caminar hacia arriba (sprites 9, 10, 11)
            Animation walkUp = new Animation("walk_up", true);
            walkUp.addFrame(playerSprites.getSprite(9), frameTime);
            walkUp.addFrame(playerSprites.getSprite(10), frameTime);
            walkUp.addFrame(playerSprites.getSprite(11), frameTime);
            animator.addAnimation(walkUp);

            // --- IDLE (Cuadro en reposo según la última dirección) ---
            Animation idleDown = new Animation("idle_down", true);
            idleDown.addFrame(playerSprites.getSprite(1), 1.0f);
            animator.addAnimation(idleDown);

            Animation idleLeft = new Animation("idle_left", true);
            idleLeft.addFrame(playerSprites.getSprite(4), 1.0f);
            animator.addAnimation(idleLeft);

            Animation idleRight = new Animation("idle_right", true);
            idleRight.addFrame(playerSprites.getSprite(7), 1.0f);
            animator.addAnimation(idleRight);

            Animation idleUp = new Animation("idle_up", true);
            idleUp.addFrame(playerSprites.getSprite(10), 1.0f);
            animator.addAnimation(idleUp);

            // Agregamos el animador al jugador
            jugador.addComponent(animator);

            // Controladores y cámara (PlayerController ahora va sin parámetros)
            jugador.addComponent(new PlayerController());
            jugador.addComponent(new CameraFollow(this.camera, viewportSize, 6.0f));

            // --- COLISIONES ---
            BoxCollider2D playerCollider = new BoxCollider2D();
            playerCollider.setSize(new Vector2f(30.0f,14.0f));
            playerCollider.setOffset(new Vector2f(9.0f,2.0f));
            jugador.addComponent(playerCollider);
            this.addGameObjectToScene(jugador);
        }
        //FORZAR PRUEBA DE COLISIÓN(corre siempre, incluso cargando desde JSON)
        for (GameObject go : this.gameObjects) {
            Tilemap tm = go.getComponent(Tilemap.class);
            if (tm != null) {
                // Marcamos el piso común (tile 0) como transitable y el 1 como sólido
                tm.settileSolid(1, true);

                // Plantamos 4 baldosas de pared justo a la derecha de RavenTom
                tm.setTile(12, 2, 1);
                tm.setTile(12, 3, 1);
                tm.setTile(12, 4, 1);
                break;
            }
        }
        // --- CALIBRAR PIES DE RAVENTOM (corre siempre) ---
        for (GameObject go : this.gameObjects) {
            if (go.getName().equals("RavenTom")) {
                BoxCollider2D col = go.getComponent(BoxCollider2D.class);
                if (col != null) {
                    col.setSize(new Vector2f(30.0f, 14.0f));
                    col.setOffset(new Vector2f(9.0f, 2.0f));
                }
                break;
            }
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);
    }
}