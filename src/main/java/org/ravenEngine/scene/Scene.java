package org.ravenEngine.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.components.*;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.physics.Physics2D;
import org.ravenEngine.renderer.*;
import org.ravenEngine.renderer.camera.UICamera;
import org.ravenEngine.renderer.texture.SpriteRenderer;
import org.ravenEngine.renderer.camera.Camera;
import org.ravenEngine.renderer.camera.ScreenFade;
import org.ravenEngine.renderer.debug.DebugDraw;
import org.ravenEngine.util.ComponentDeserializer;
import org.ravenEngine.util.ComponentSerializer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Scene {
    protected Camera camera;
    protected final List< GameObject > gameObjects = new ArrayList<>();
    // 1. Declarar sin instanciar inline
    protected Renderer renderer;
    private boolean isRunning = false;
    private final List< GameObject > pendingToDestroy = new ArrayList<>();

    protected transient UICamera uiCamera = new UICamera(1280.0f, 720.0f);
    private transient ScreenFade screenFade = new ScreenFade();

    // Subsistema de físicas desacoplado
    private final Physics2D physics2D = new Physics2D();

    // Control de visualización de debug
    protected boolean debugMode = true;

    public Scene() {
        this.camera = new Camera(new Vector2f(0.0f, 0.0f));
    }

    public void init(){};

    /**
     * Se llama cuando presionas PLAY en el editor o arranca el juego standalone.
     * En este punto TODOS los objetos de la escena ya existen en memoria.
     */
    public void start() {
        if (this.renderer == null) {
            this.renderer = new Renderer();
        }

        this.isRunning = true;

        for (GameObject go : this.gameObjects) {
            go.start(); // Ejecuta c.invokeStart() asegurando que todos los GameObjects existan
            this.renderer.add(go);
            this.physics2D.add(go);
        }
    }

    /**
     * Se llama cuando presionas STOP en el editor.
     */
    public void stop() {
        this.isRunning = false;
        for (GameObject go : this.gameObjects) {
            for (Component c : go.getAllComponents()) {
                c.resetState();
            }
        }
    }

    public void update(float dt) {
        if (KeyListener.keyBeginPress(org.lwjgl.glfw.GLFW.GLFW_KEY_F3)) {
            debugMode = !debugMode;
            System.out.println("[Scene] Debug Mode cambiado a: " + debugMode);
        }

        // 1. Limpieza de objetos destruidos al final/inicio del frame
        if (!this.pendingToDestroy.isEmpty()) {
            for (GameObject go : this.pendingToDestroy) {
                this.gameObjects.remove(go);
                this.physics2D.remove(go);

                if (this.renderer != null) {
                    this.renderer.destroyGameObject(go);
                }
            }

            this.pendingToDestroy.clear();
        }

        if (this.camera != null) {
            this.camera.update(dt);
        }
        if (this.screenFade != null) {
            this.screenFade.update(dt);
        }

        // 2. Solo actualizamos la lógica si la escena está activa
        if (this.isRunning) {
            for (GameObject go : this.gameObjects) {
                go.update(dt);
            }
            this.physics2D.update(dt, this.gameObjects);
        }
    }

    // ==========================================
    // PERSISTENCIA (GUARDADO Y CARGA)
    // ==========================================
    public void save(String filePath) {
        if (filePath == null || filePath.isEmpty()) return;

        try {
            java.io.File file = new java.io.File(filePath);

            // Crea las carpetas intermedias (ej: assets/) si no existen
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(Component.class, new ComponentSerializer())
                    .registerTypeAdapter(Component.class, new ComponentDeserializer())
                    .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT | java.lang.reflect.Modifier.STATIC)
                    .create();

            String json = gson.toJson(this.gameObjects);
            java.nio.file.Files.writeString(file.toPath(), json);

            System.out.println("[Scene] Escena guardada correctamente en: " + file.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[Error] Falló el guardado del archivo JSON: " + filePath + " (" + e.getMessage() + ")");
            e.printStackTrace();
        }
    }

    public void load(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("[Scene] No se encontró el archivo de escena: " + filePath + ". Se mantendrá la escena base.");
            return;
        }

        this.gameObjects.clear();
        this.physics2D.clear();
        if (this.renderer != null) {
            this.renderer.clear();
        }

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT | java.lang.reflect.Modifier.STATIC)
                .create();

        try (FileReader reader = new FileReader(file)) {
            GameObject[] objects = gson.fromJson(reader, GameObject[].class);
            if (objects != null) {
                for (GameObject go : objects) {
                    if (go.getComponents() != null) {
                        for (Component c : go.getComponents()) {
                            c.gameObject = go;

                            if (c instanceof CameraFollow) {
                                ((CameraFollow) c).setCamera(this.camera);
                            }
                        }
                    }

                    // Se agrega a la escena y a la GPU sin ejecutar start()
                    this.addGameObjectToScene(go);
                    if (this.renderer != null) {
                        this.renderer.add(go);
                    }
                }
                System.out.println("[Scene] Escena cargada con " + objects.length + " entidades desde: " + filePath);
            }
        } catch (IOException e) {
            System.err.println("[Scene] Error al cargar la escena: " + e.getMessage());
            e.printStackTrace();
        }

        // Reconstrucción de jerarquías y transforms
        for (GameObject go : this.gameObjects) {
            if (go.transform != null) {
                go.transform.gameObject = go;
            }
            if (go.getParentName() != null && !go.getParentName().isEmpty()) {
                for (GameObject potentialParent : this.gameObjects) {
                    if (potentialParent.getName().equals(go.getParentName())) {
                        potentialParent.addChild(go);
                        break;
                    }
                }
            }
        }

        // Si se cargó la escena mientras el juego ya estaba en Play, arrancamos los objetos
        if (this.isRunning) {
            for (GameObject go : this.gameObjects) {
                go.start();
            }
        }
    }

    public String serializeToString() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT | java.lang.reflect.Modifier.STATIC)
                .create();

        return gson.toJson(this.gameObjects);
    }

    public void loadFromString(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) return;

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT | java.lang.reflect.Modifier.STATIC)
                .create();

        GameObject[] objects = gson.fromJson(jsonContent, GameObject[].class);
        if (objects != null) {
            this.gameObjects.clear();
            this.physics2D.clear();
            if (this.renderer != null) {
                this.renderer.clear();
            }

            for (GameObject go : objects) {
                if (go.getComponents() != null) {
                    for (Component c : go.getComponents()) {
                        c.gameObject = go;

                        // 1. Re-conectar cámara al script de seguimiento
                        if (c instanceof CameraFollow) {
                            ((CameraFollow) c).setCamera(this.camera);
                        }

                        // 2. Reconectar textura con AssetPool (para que no quede negra)
                        if (c instanceof SpriteRenderer) {
                            ((SpriteRenderer) c).start(); // Llama a la reconexión de textura que ya tenías en start()
                        }

                        // 3. Re-crear shaders y VBOs del Tilemap para el editor
                        if (c instanceof Tilemap) {
                            ((Tilemap) c).start();
                        }
                    }
                }
                this.addGameObjectToScene(go);
                // Asegurar que el renderer del editor lo dibuje aunque isRunning sea false
                if (this.renderer != null) {
                    this.renderer.add(go);
                }
            }
            // Solo los scripts de jugabilidad (movimiento, daño, etc.) esperan al modo Play
            if (this.isRunning) {
                for (GameObject go : this.gameObjects) {
                    go.start();
                }
            }
        }
    }

    // ==========================================
    // GESTIÓN DE ENTIDADES
    // ==========================================
    public void addGameObjectToScene(GameObject go) {
        if (!this.gameObjects.contains(go)) {
            this.gameObjects.add(go);
        }

        this.physics2D.add(go);

        if (this.isRunning) {
            go.start();
            if (this.renderer != null) {
                this.renderer.add(go);
            }
        }
    }

    public void spawnGameObject(GameObject go) {
        if (go == null) return;
        this.addGameObjectToScene(go);
    }

    // ==========================================
    // RENDERIZADO
    // ==========================================
    public void render() {
        // 1. Tilemap al fondo
        for (GameObject go : this.gameObjects) {
            Tilemap tm = go.getComponent(Tilemap.class);
            if (tm != null) {
                tm.render(this.camera);
            }
        }

        // 2. Y-Sorting
        this.gameObjects.sort((a, b) -> {
            boolean aIsTilemap = a.getComponent(Tilemap.class) != null;
            boolean bIsTilemap = b.getComponent(Tilemap.class) != null;

            if (aIsTilemap && bIsTilemap) return 0;
            if (aIsTilemap) return -1;
            if (bIsTilemap) return 1;

            if (a.transform == null && b.transform == null) return 0;
            if (a.transform == null) return -1;
            if (b.transform == null) return 1;

            SpriteRenderer sprA = a.getComponent(SpriteRenderer.class);
            SpriteRenderer sprB = b.getComponent(SpriteRenderer.class);

            float depthA = (sprA != null) ? sprA.getDepthValue() : a.transform.pos.y;
            float depthB = (sprB != null) ? sprB.getDepthValue() : b.transform.pos.y;

            return Float.compare(depthB, depthA);
        });

        // 3. Entidades ordenadas
        this.renderer.render(this.camera);

        // 4.1 Partículas del mundo
        for (GameObject go : this.gameObjects) {
            ParticleSystem ps = go.getComponent(ParticleSystem.class);
            if (ps != null) {
                ps.render(this.camera);
            }
        }

        // 4.2 Texto en el mundo
        for (GameObject go : this.gameObjects) {
            TextRenderer tr = go.getComponent(TextRenderer.class);
            if (tr != null) {
                tr.render(this.camera);
            }
        }

        // 5. Pasada de debug del mundo (Colliders con la cámara del juego)
        if (debugMode) {
            for (GameObject go : this.gameObjects) {
                Tilemap tm = go.getComponent(Tilemap.class);
                if (tm != null) {
                    tm.drawDebugBounds();
                }
            }

            for (BoxCollider2D col : this.physics2D.getColliders()) {
                if (col.gameObject == null || col.gameObject.transform == null) continue;

                Vector2f center = new Vector2f(
                        col.gameObject.transform.pos.x + col.getOffset().x + (col.getSize().x * 0.5f),
                        col.gameObject.transform.pos.y + col.getOffset().y + (col.getSize().y * 0.5f)
                );

                Vector3f color = col.isTrigger() ? new Vector3f(1.0f, 0.85f, 0.0f) : new Vector3f(0.0f, 1.0f, 0.0f);
                DebugDraw.addBox2D(center, col.getSize(), col.getRotation(), color);
            }

            DebugDraw.draw(this.camera);
        }

        // 6. Pasada de interfaz (HUD con UICamera fija)
        for (GameObject go : this.gameObjects) {
            List< Component > comps = go.getAllComponents();
            if (comps == null) continue;

            for (Component c : comps) {
                if (c instanceof UIElement && ((UIElement) c).isVisible()) {
                    ((UIElement) c).renderUI(this.uiCamera);
                }
            }
        }

        DebugDraw.draw(this.uiCamera);

        // 7. Overlays finales
        if (this.screenFade != null) {
            this.screenFade.render();
        }

        DebugDraw.beginFrame();
    }

    // ==========================================
    // GESTIÓN DE ENTIDADES Y DESTRUCCIÓN
    // ==========================================
    public void destroyGameObjectImmediate(GameObject go) {
        if (go == null) return;

        this.gameObjects.remove(go);
        this.pendingToDestroy.remove(go);
        this.physics2D.remove(go);

        if (this.renderer != null) {
            this.renderer.destroyGameObject(go);
        }
    }

    public void destroyGameObject(GameObject go) {
        if (go == null) return;

        if (!this.isRunning) {
            destroyGameObjectImmediate(go);
        } else if (!this.pendingToDestroy.contains(go)) {
            this.pendingToDestroy.add(go);
        }
    }

    public void resizeUI(float width, float height) {
        if (this.uiCamera != null) {
            this.uiCamera.adjustProjection(width, height);
        }
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public boolean isRunning() { return this.isRunning; }

    public void toggleDebugMode() { this.debugMode = !this.debugMode; }

    public boolean isDebugMode() { return this.debugMode; }

    public Camera getCamera() { return camera; }

    public List< GameObject > getGameObjects() { return this.gameObjects; }

    public Physics2D getPhysics2D() { return physics2D; }

    public ScreenFade getScreenFade() { return screenFade; }

    public UICamera getUICamera() { return this.uiCamera; }
}