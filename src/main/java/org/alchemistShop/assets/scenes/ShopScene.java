package org.alchemistShop.assets.scenes;

import imgui.internal.ImGui;
import org.alchemistShop.assets.entities.ItemPickup;
import org.alchemistShop.assets.entities.PlayerController;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.ravenEngine.components.*;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.renderer.Sprite;
import org.ravenEngine.renderer.SpriteRenderer;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.renderer.Texture;
import org.ravenEngine.renderer.animation.Animation;
import org.ravenEngine.renderer.text.Font;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;
import org.ravenEngine.util.AssetPool;

public class ShopScene extends Scene {

    private final Vector2f viewportSize = new Vector2f(640.0f, 360.0f);

    // ==========================================
    // INICIALIZACIÓN DE LA ESCENA
    // ==========================================
    @Override
    public void init() {
        // 1. Intentamos cargar desde el archivo serializado
        //this.load("src/main/java/org/alchemistShop/assets/level1.json");

        // 2. Cargamos la fuente en memoria
        Font pixelFont = new Font("/usr/share/fonts/noto/NotoSans-Regular.ttf", 20.0f);

        // 3. Si no había archivo guardado, creamos todo de cero
        if (this.gameObjects.isEmpty()) {
            buildDefaultScene();
        }

        // 4. SIEMPRE reasignamos la fuente a los textos (tanto si vino de código como de JSON)
        for (GameObject go : this.gameObjects) {
            TextRenderer tr = go.getComponent(TextRenderer.class);
            if (tr != null) {
                tr.setFont(pixelFont);
            }
        }

        calibratePlayerCollider();
    }

    @Override
    public void update(float dt) {
        super.update(dt);

    }



    // ==========================================
    // CONSTRUCCIÓN DE ESCENA BASE
    // ==========================================
    private void buildDefaultScene() {
        // --- RECURSOS (SPRITESHEETS) ---
        Texture charSheetTex = AssetPool.getTexture("/textures/Personaje.png");
        Spritesheet playerSprites = new Spritesheet(charSheetTex, 32, 32, 16, 0);

        Texture shopSheetTex = AssetPool.getTexture("/textures/items_alquimia.png");
        Spritesheet propsSprites = new Spritesheet(shopSheetTex, 167, 186, 8, 0);
        AssetPool.addSpritesheet("/textures/items_alquimia.png", propsSprites);

        // --- SUB-GENERADORES ---
        createTilemap();
        createProps(propsSprites);
        createPlayer(playerSprites);
        createPushableTestBox(); // <-- Objeto de prueba para físicas dinámicas
        createNPC();
    }

    // ==========================================
    // GENERACIÓN DE ENTIDADES
    // ==========================================
    private void createTilemap() {
        GameObject tilemapObj = new GameObject("tilemap_piso", new Transform(new Vector2f(0, 0)), -1);
        Tilemap tilemap = new Tilemap(20, 11, 32, 32, "textures/wood_floor.png", 1, 0);

        tilemap.settileSolid(1, true);

        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 10; y++) {
                tilemap.setTile(x, y, 0);
            }
        }

        tilemapObj.addComponent(tilemap);
        this.addGameObjectToScene(tilemapObj);
    }

    private void createProps(Spritesheet propsSprites) {
        Font pixelFont = new Font("/usr/share/fonts/noto/NotoSans-Regular.ttf",20f);
        // --- FILA DE POCIONES ---
        GameObject redpoti = createItem("PocionRoja", 100, 150, propsSprites.getSprite(0), "Poción de Vida Menor");
        TextRenderer prompt = new TextRenderer(
                pixelFont,
                "[E] Recoger",
                new Vector4f(0.3f,1.0f,0.5f,1.0f),
                new Vector2f(-15.0f,50.0f)
        );
        prompt.setScale(0.5f);
        redpoti.addComponent(prompt);

        createItem("VialVerde", 160, 150, propsSprites.getSprite(1), null);
        createItem("MatrazAzul", 220, 150, propsSprites.getSprite(2), null);

        GameObject elixir = createItem("ElixirPurpura", 280, 150, propsSprites.getSprite(3), null);
        ParticleSystem vaporMagico = new ParticleSystem();
        vaporMagico.setColors(
                new Vector4f(0.8f,0.2f,1.0f,0.9f),
                new Vector4f(0.2f,0.6f,1.0f,0.0f)
        );
        vaporMagico.setSizes(10.0f,2.0f);
        vaporMagico.setVelocity(new Vector2f(0.0f,40.0f),new Vector2f(15.0f,10.0f));
        elixir.addComponent(vaporMagico);

        // --- FILA DE INGREDIENTES ---
        createItem("Hierbas", 100, 95, propsSprites.getSprite(4), null);
        createItem("Raiz", 160, 95, propsSprites.getSprite(5), null);
        createItem("Hongo", 220, 95, propsSprites.getSprite(6), null);
        createItem("FrascoVacio", 280, 95, propsSprites.getSprite(7), null);

        // --- MOSTRADOR ESTÁTICO ---
        GameObject mostrador = new GameObject("Mostrador", new Transform(new Vector2f(80, 50), new Vector2f(260, 45), 1));
        mostrador.addComponent(new SpriteRenderer(new Vector4f(0.50f, 0.25f, 0.1f, 1.0f)));

        BoxCollider2D mostradorCollider = new BoxCollider2D(
                new Vector2f(mostrador.transform.scale.x, 40),
                new Vector2f(0.0f, 0.0f),
                false,
                true // Estático: bloquea el paso sin moverse
        );
        mostrador.addComponent(mostradorCollider);
        this.addGameObjectToScene(mostrador);
    }

    private GameObject createItem(String name, float x, float y, org.ravenEngine.renderer.Sprite sprite, String pickupName) {
        GameObject item = new GameObject(name, new Transform(new Vector2f(x, y), new Vector2f(48, 48), 1));
        item.addComponent(new SpriteRenderer(sprite));
        if (pickupName != null) {
            item.addComponent(new ItemPickup(pickupName, 1));
        }
        this.addGameObjectToScene(item);
        return item;
    }

    private void createPlayer(Spritesheet playerSprites) {
        GameObject jugador = new GameObject("RavenTom", new Transform(new Vector2f(360, 170), new Vector2f(48, 54), 1));
        jugador.addComponent(new SpriteRenderer(playerSprites.getSprite(1)));

        // Animador
        SpriteAnimator animator = new SpriteAnimator();
        float frameTime = 0.15f;

        Animation walkDown = new Animation("walk_down", true);
        walkDown.addFrame(playerSprites.getSprite(0), frameTime);
        walkDown.addFrame(playerSprites.getSprite(1), frameTime);
        walkDown.addFrame(playerSprites.getSprite(2), frameTime);
        animator.addAnimation(walkDown);

        Animation walkLeft = new Animation("walk_left", true);
        walkLeft.addFrame(playerSprites.getSprite(3), frameTime);
        walkLeft.addFrame(playerSprites.getSprite(4), frameTime);
        walkLeft.addFrame(playerSprites.getSprite(5), frameTime);
        animator.addAnimation(walkLeft);

        Animation walkRight = new Animation("walk_right", true);
        walkRight.addFrame(playerSprites.getSprite(6), frameTime);
        walkRight.addFrame(playerSprites.getSprite(7), frameTime);
        walkRight.addFrame(playerSprites.getSprite(8), frameTime);
        animator.addAnimation(walkRight);

        Animation walkUp = new Animation("walk_up", true);
        walkUp.addFrame(playerSprites.getSprite(9), frameTime);
        walkUp.addFrame(playerSprites.getSprite(10), frameTime);
        walkUp.addFrame(playerSprites.getSprite(11), frameTime);
        animator.addAnimation(walkUp);

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

        jugador.addComponent(animator);

        // Controladores y Cámara
        jugador.addComponent(new PlayerController());
        jugador.addComponent(new CameraFollow(this.camera, viewportSize, 6.0f));
        jugador.addComponent(new Inventory());

        // Colisionador (Pies)
        BoxCollider2D playerCollider = new BoxCollider2D();
        playerCollider.setSize(new Vector2f(30.0f, 14.0f));
        playerCollider.setOffset(new Vector2f(9.0f, 2.0f));
        jugador.addComponent(playerCollider);

        // Rigidbody para físicas
        RigidBody2D playerRb = new RigidBody2D(1.0f);
        jugador.addComponent(playerRb);

        this.addGameObjectToScene(jugador);
    }
    private void createNPC(){
        // ==========================================
        // NPC DE PRUEBA: ALQUIMISTA ANCIANO
        // ==========================================
        GameObject npc = new GameObject("Viejo Alquimista");

        // 1. Ubicación cerca del spawn de RavenTom (ajustá X e Y según tu mapa)
        npc.transform.pos.set(200.0f, 150.0f);
        npc.transform.scale.set(32.0f, 32.0f);

        // 2. Le asignamos una textura (podés usar el Frasco.png o cualquier sprite que tengas)
        Texture npcTex = AssetPool.getTexture("textures/Frasco.png");
        if (npcTex != null) {
            Sprite sprite = new Sprite(npcTex);
            npc.addComponent(new SpriteRenderer(sprite));
        }

        // 3. Le clavamos el componente de diálogo interactivo
        DialogueInteractable dialogue = new DialogueInteractable(
                "Viejo Alquimista",
                "¡Epa, RavenTom! Menos mal que llegaste.",
                "El caldero del fondo empezó a largar humo violeta...",
                "Fijate en tu bolsa si tenés alguna poción para neutralizarlo."
        );

        // Ajustamos el radio de interacción (en píxeles)
        dialogue.interactionRadius = 45.0f;

        npc.addComponent(dialogue);

        // 4. Lo sumamos a la escena
        this.addGameObjectToScene(npc);

    }

    private void createPushableTestBox() {
        // Caja de prueba para verificar empuje entre dos entidades dinámicas
        GameObject caja = new GameObject("CajaEmpujable", new Transform(new Vector2f(250, 180), new Vector2f(32, 32), 1));
        caja.addComponent(new SpriteRenderer(new Vector4f(0.8f, 0.5f, 0.2f, 1.0f))); // Color madera clara

        BoxCollider2D col = new BoxCollider2D(new Vector2f(32, 32), new Vector2f(0, 0), false, false);
        caja.addComponent(col);

        RigidBody2D rb = new RigidBody2D(0.8f); // Más liviana para ser empujada
        caja.addComponent(rb);

        this.addGameObjectToScene(caja);
    }


    // ==========================================
    // AUXILIARES
    // ==========================================
    private void calibratePlayerCollider() {
        for (GameObject go : this.gameObjects) {
            if ("RavenTom".equals(go.getName())) {
                BoxCollider2D col = go.getComponent(BoxCollider2D.class);
                if (col != null) {
                    col.setSize(new Vector2f(30.0f, 14.0f));
                    col.setOffset(new Vector2f(9.0f, 2.0f));
                }
                break;
            }
        }
    }
}