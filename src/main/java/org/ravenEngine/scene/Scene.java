package org.ravenEngine.scene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.components.*;
import org.ravenEngine.core.Engine;
import org.ravenEngine.core.Window;
import org.ravenEngine.editor.GizmoRenderer;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.physics.Physics2D;
import org.ravenEngine.renderer.*;
import org.ravenEngine.util.ComponentDeserializer;
import org.ravenEngine.util.ComponentSerializer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class Scene {
    protected Camera camera;
    protected final List< GameObject > gameObjects = new ArrayList< >();
    protected Renderer renderer;
    private boolean isRunning = false;
    private final List< GameObject > pendingToDestroy = new ArrayList< >();

    private transient ScreenFade screenFade = new ScreenFade();

    // Subsistema de físicas desacoplado
    private final Physics2D physics2D = new Physics2D();

    // Control de visualización de debug
    protected boolean debugMode = true;

    public Scene() {
        this.camera = new Camera(new Vector2f(0.0f, 0.0f));
    }

    public abstract void init();

    public void start() {
        if (renderer == null) {
            this.renderer = new Renderer();
        }

        for (GameObject go : gameObjects) {
            go.start();
            this.renderer.add(go);
            this.physics2D.add(go);
        }
        isRunning = true;
    }

    public void update(float dt) {
        if (KeyListener.keyBeginPress(org.lwjgl.glfw.GLFW.GLFW_KEY_F3)) {
            debugMode = !debugMode;
            System.out.println("[Scene] Debug Mode cambiado a: " + debugMode);
        }

        // 1. Limpieza limpia de objetos destruidos al final/inicio del frame
        if (!this.pendingToDestroy.isEmpty()) {
            for (GameObject go : this.pendingToDestroy) {
                this.gameObjects.remove(go);
                this.physics2D.remove(go);

                // Sacamos únicamente el sprite del objeto destruido sin tocar los demás
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

        // 3. Update de lógica de componentes
        for (GameObject go : gameObjects) {
            go.update(dt);
        }

        // 4. Paso de simulación de físicas desacoplado
        this.physics2D.update(dt, this.gameObjects);
    }

    // ==========================================
    // PERSISTENCIA (GUARDADO Y CARGA)
    // ==========================================
    public void save(String filePath) {
        if (org.ravenEngine.core.Engine.IsPlaying()) {
            System.err.println("[Alerta] No se puede guardar la escena mientras la simulación está activa.");
            return;
        }

        // 1. Aseguramos la existencia de las carpetas intermedias
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT | java.lang.reflect.Modifier.STATIC)
                .create();

        String jsonOutput = gson.toJson(this.gameObjects.toArray(new GameObject[0]));

        // 2. Escribimos sobre la instancia de File ya validada
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(jsonOutput);
            writer.flush();
            System.out.println("[Motor] Escena guardada correctamente en: " + filePath);
        } catch (Exception e) {
            System.err.println("[Error] Falló el guardado del archivo JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void load(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("[Scene] No se encontró el archivo de escena: " + filePath + ". Se mantendrá la escena base.");
            return;
        }

        // Vaciado de colecciones previo a la deserialización
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

                    this.addGameObjectToScene(go);
                    go.start();

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

                        if (c instanceof CameraFollow) {
                            ((CameraFollow) c).setCamera(this.camera);
                        }
                    }
                }

                this.addGameObjectToScene(go);

                if (!isRunning) {
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

        if (isRunning) {
            go.start();
            if (this.renderer != null) {
                this.renderer.add(go);
            }
        }
    }


    public void spawnGameObject(GameObject go) {
        if (go == null) return;
        this.gameObjects.add(go);
        this.physics2D.add(go);

        if (this.isRunning) {
            go.start();
            if (this.renderer != null) {
                this.renderer.add(go);
            }
        }
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
            if (a.getComponent(Tilemap.class) != null) return -1;
            if (b.getComponent(Tilemap.class) != null) return 1;

            float yA = a.transform.pos.y;
            float yB = b.transform.pos.y;
            return Float.compare(yB, yA);
        });

        // 3. Entidades ordenadas
        this.renderer.render(this.camera);

        // 4.1 SISTEMA DE PARTÍCULAS
        for (GameObject go : this.gameObjects) {
            ParticleSystem ps = go.getComponent(ParticleSystem.class);
            if (ps != null) {
                ps.render(this.camera);
            }
        }

        //4.2 RENDERIZADO DE TEXTO EN EL MUNDO
        for (GameObject go : this.gameObjects){
            TextRenderer tr = go.getComponent(TextRenderer.class);
            if (tr != null){
                tr.render(this.camera);
            }
        }

        // 5. Debug bounds y gizmos
        if (debugMode) {
            for (GameObject go : this.gameObjects) {
                Tilemap tm = go.getComponent(Tilemap.class);
                if (tm != null) {
                    tm.drawDebugBounds();
                }
            }

            for (BoxCollider2D col : this.physics2D.getColliders()) {
                Vector2f min = new Vector2f(col.getMinX(), col.getMinY());
                Vector2f max = new Vector2f(col.getMaxX(), col.getMaxY());
                DebugDraw.addBox2D(min, max, new Vector3f(0.0f, 1.0f, 0.0f));
            }

            DebugDraw.draw(this.camera);
        }
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

        // Si estamos en el editor (simulación apagada), borra al instante de la GPU
        if (!org.ravenEngine.core.Engine.IsPlaying()) {
            destroyGameObjectImmediate(go);
        } else if (!this.pendingToDestroy.contains(go)) {
            this.pendingToDestroy.add(go);
        }
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public void toggleDebugMode() {
        this.debugMode = !this.debugMode;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public Camera getCamera() {
        return camera;
    }

    public List< GameObject > getGameObjects() {
        return this.gameObjects;
    }

    public Physics2D getPhysics2D() {
        return physics2D;
    }

    public ScreenFade getScreenFade() {
        return screenFade;
    }
}