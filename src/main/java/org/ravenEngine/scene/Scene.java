package org.ravenEngine.scene;

import com.google.gson.*;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.components.BoxCollider2D;
import org.ravenEngine.components.CameraFollow;
import org.ravenEngine.components.Tilemap;
import org.ravenEngine.components.Transform;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.physics.CollisionPair;
import org.ravenEngine.renderer.Camera;
import org.ravenEngine.renderer.DebugDraw;
import org.ravenEngine.renderer.Renderer;
import org.ravenEngine.util.ComponentDeserializer;
import org.ravenEngine.util.ComponentSerializer;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;

public abstract class Scene {
    protected Camera camera;
    protected final List< GameObject > gameObjects = new ArrayList< >();
    protected Renderer renderer;
    private boolean isRunning = false;
    private  List<BoxCollider2D> colliders = new ArrayList<>();

    //debug mode
    // Control de visualización de debug
    protected boolean debugMode = true; // Arranca prendido o apagado según prefieras
    private boolean f1Debounce = false;

    // Colecciones para rastrear transiciones entre frames
    private final Set< CollisionPair > activeCollisions = new HashSet< >();
    private final Set<CollisionPair> currentCollisions = new HashSet< >();

    public Scene() {
        this.camera = new Camera(new org.joml.Vector2f(0.0f, 0.0f));
    }

    public abstract void init();

    public void start() {
        // Inicializamos el Renderer acá, cuando OpenGL ya está listo
        if (renderer == null) {
            this.renderer = new Renderer();
        }

        for (GameObject go : gameObjects) {
            go.start();
            this.renderer.add(go);
        }
        isRunning = true;
    }

    public void update(float dt) {
        // keyBeginPress solo devuelve true en el PRIMER frame que baja la tecla
        if (KeyListener.keyBeginPress(org.lwjgl.glfw.GLFW.GLFW_KEY_F3)) {
            debugMode = !debugMode;
            System.out.println("[Scene] Debug Mode cambiado a: " + debugMode);
        }

        for (GameObject go : gameObjects) {
            go.update(dt);
        }
        checkCollisions();
    }


    //FUNCIONES

    //GUARDADO
    public void save(String filePath) {
        // 1. Evitar guardar si estamos en Play (para no congelar posiciones en movimiento)
        if (org.ravenEngine.core.Engine.IsPlaying()) {
            System.err.println("[Alerta] No se puede guardar la escena mientras la simulación está activa.");
            return;
        }

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT | java.lang.reflect.Modifier.STATIC)
                .create();

        // 2. Serializamos a String en memoria primero
        String jsonOutput = gson.toJson(this.gameObjects.toArray(new GameObject[0]));

        // 3. Escribimos al disco garantizando UTF-8 y cierre inmediato del flujo
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
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

        // 1. VACIAR LISTAS ANTES DE CARGAR (Arriba, nunca al final)
        this.gameObjects.clear();
        this.colliders.clear();
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

                    // Aseguramos que los sprites queden registrados en el renderer
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

    //AGREGAR OBJETO A LA ESCENA
    public void addGameObjectToScene(GameObject go) {
        if (!this.gameObjects.contains(go)) {
            this.gameObjects.add(go);
        }

        // Solo agregamos a la lista de físicas si tiene el componente y no estaba ya
        BoxCollider2D col = go.getComponent(BoxCollider2D.class);
        if (col != null && !this.colliders.contains(col)) {
            this.colliders.add(col);
        }
        if (isRunning) {
            go.start();
            this.renderer.add(go);
        }
    }

    public void render() {
        // 1. El suelo siempre se dibuja primero y al fondo de todoo
        for (GameObject go : this.gameObjects) {
            Tilemap tm = go.getComponent(Tilemap.class);
            if (tm != null) {
                tm.render(this.camera);
            }
        }

        // 2. Y-SORTING: Ordenamos las entidades de mayor Y a menor Y
        // (Los que están más arriba en pantalla se renderizan antes; los de abajo se renderizan encima)
        this.gameObjects.sort((a, b) -> {
            // Excluimos tilemaps del ordenamiento
            if (a.getComponent(Tilemap.class) != null) return -1;
            if (b.getComponent(Tilemap.class) != null) return 1;

            // Comparamos el eje Y de la base (pies) de cada objeto
            float yA = a.transform.pos.y;
            float yB = b.transform.pos.y;

            return Float.compare(yB, yA); // Orden descendente
        });

        // 3. Renderizamos las entidades ordenadas
        this.renderer.render(this.camera);

        // 4. Debug bounds y gizmos al final
        if (debugMode) {
            for (GameObject go : this.gameObjects) {
                Tilemap tm = go.getComponent(Tilemap.class);
                if (tm != null) {
                    tm.drawDebugBounds();
                }
            }

            for (BoxCollider2D col : this.colliders) {
                Vector2f min = new Vector2f(col.getMinX(), col.getMinY());
                Vector2f max = new Vector2f(col.getMaxX(), col.getMaxY());
                DebugDraw.addBox2D(min, max, new Vector3f(0.0f, 1.0f, 0.0f));
            }

            DebugDraw.draw(this.camera);
        }

        DebugDraw.beginFrame();
    }

    private void resolveTilemapCollisions(BoxCollider2D collider, Tilemap tilemap) {
        if (collider.isStatic() || tilemap == null || tilemap.gameObject == null) return;

        Vector2f tilemapOrigin = tilemap.gameObject.transform.pos;
        // Si tus variables son privadas con getter usa getTileWidth(), si no el campo directo
        int tileW = tilemap.getTileWidth();
        int tileH = tilemap.getTileHeight();

        // Convertimos la caja del personaje a rango de celdas de la grilla.
        int minTileX = (int) Math.floor((collider.getMinX() - tilemapOrigin.x) / tileW);
        int maxTileX = (int) Math.floor((collider.getMaxX() - tilemapOrigin.x) / tileW);
        int minTileY = (int) Math.floor((collider.getMinY() - tilemapOrigin.y) / tileH);
        int maxTileY = (int) Math.floor((collider.getMaxY() - tilemapOrigin.y) / tileH);

        // Revisamos únicamente las baldosas que el personaje está pisando
        for (int tx = minTileX; tx <= maxTileX; tx++) {
            for (int ty = minTileY; ty <= maxTileY; ty++) {
                if (!tilemap.isSolid(tx, ty)) continue;

                float tileMinX = tilemapOrigin.x + (tx * tileW);
                float tileMaxX = tileMinX + tileW;
                float tileMinY = tilemapOrigin.y + (ty * tileH);
                float tileMaxY = tileMinY + tileH;

                // Recalculamos con la posición actual del collider
                float curMinX = collider.getMinX();
                float curMaxX = collider.getMaxX();
                float curMinY = collider.getMinY();
                float curMaxY = collider.getMaxY();

                float overlapLeft   = curMaxX - tileMinX;
                float overlapRight  = tileMaxX - curMinX;
                float overlapBottom = curMaxY - tileMinY;
                float overlapTop    = tileMaxY - curMinY;

                if (overlapLeft > 0 && overlapRight > 0 && overlapBottom > 0 && overlapTop > 0) {
                    float overlapX = Math.min(overlapLeft, overlapRight);
                    float overlapY = Math.min(overlapBottom, overlapTop);

                    // Empujamos en el eje de menor penetración para sacarlo de la pared
                    if (overlapX < overlapY) {
                        float resolveX = (overlapLeft < overlapRight) ? -overlapX : overlapX;
                        collider.gameObject.transform.pos.x += resolveX;
                    } else {
                        float resolveY = (overlapBottom < overlapTop) ? -overlapY : overlapY;
                        collider.gameObject.transform.pos.y += resolveY;
                    }
                }
            }
        }
    }

    //SERIALIZACIÓN EN MEMORIA
    public String serializeToString() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeAdapter(Component.class, new ComponentDeserializer())
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT | java.lang.reflect.Modifier.STATIC)
                .create();

        return gson.toJson(this.gameObjects);
    }

    // RESTAURA LA ESCENA DESDE MEMORIA (PLAY / STOP)
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
            // 1. LIMPIEZA TOTAL DE ESTADOS ANTERIORES
            this.gameObjects.clear();
            this.colliders.clear(); // <-- ¡ESTA LÍNEA LIQUIDA LOS FANTASMAS!
            if (this.renderer != null) {
                this.renderer.clear();
            }

            for (GameObject go : objects) {
                if (go.getComponents() != null) {
                    for (Component c : go.getComponents()) {
                        c.gameObject = go;

                        // Inyectamos la cámara al restaurar snapshot
                        if (c instanceof CameraFollow) {
                            ((CameraFollow) c).setCamera(this.camera);
                        }
                    }
                }

                // Registra el GO en gameObjects, colliders y renderer
                this.addGameObjectToScene(go);

                // Si isRunning no estaba en true adentro de addGameObjectToScene, aseguramos start
                if (!isRunning) {
                    go.start();
                }
            }
        }
    }

    //CHEQUEAR COLISIONES
    public void checkCollisions() {
        // -------------------------------------------------------------
        // 1. COLISIÓN CONTRA EL TILEMAP (Paredes y obstáculos del mapa)
        // -------------------------------------------------------------
        Tilemap activeTilemap = null;
        for (GameObject go : this.gameObjects) {
            Tilemap tm = go.getComponent(Tilemap.class);
            if (tm != null) {
                activeTilemap = tm;
                break;
            }
        }

        if (activeTilemap != null) {
            for (BoxCollider2D col : colliders) {
                if (!col.isStatic()) {
                    resolveTilemapCollisions(col, activeTilemap);
                }
            }
        }

        // -------------------------------------------------------------
        // 2. COLISIÓN ENTRE ENTIDADES (Mostrador, pociones, etc.)
        // -------------------------------------------------------------
        currentCollisions.clear();
        for (int i = 0; i < colliders.size(); i++) {
            BoxCollider2D colA = colliders.get(i);
            for (int j = i + 1; j < colliders.size(); j++) {
                BoxCollider2D colB = colliders.get(j);
                if (checkAABB(colA, colB)) {
                    CollisionPair pair = new CollisionPair(colA, colB);
                    currentCollisions.add(pair);
                    boolean isTriggerInteraction = colA.isTrigger() || colB.isTrigger();
                    if (!isTriggerInteraction) {
                        // Solo resolvemos la posición si ambos son sólidos
                        resolvePhysicsCollision(colA, colB);
                    }
                    // Despacho de Enter vs Stay
                    if (!activeCollisions.contains(pair)) {
                        notifyCollisionEnter(colA, colB, isTriggerInteraction);
                    } else {
                        notifyCollisionStay(colA, colB, isTriggerInteraction);
                    }
                }
            }
        }

        // Detectar los que se separaron este frame (Exit)
        for (CollisionPair pair : activeCollisions) {
            if (!currentCollisions.contains(pair)) {
                boolean isTriggerInteraction = pair.a.isTrigger() || pair.b.isTrigger();
                notifyCollisionExit(pair.a, pair.b, isTriggerInteraction);
            }
        }

        // Actualizamos las colisiones activas para el próximo cuadro
        activeCollisions.clear();
        activeCollisions.addAll(currentCollisions);
    }

    //FISICAS Y COLISIONES
    private boolean checkAABB(BoxCollider2D a, BoxCollider2D b) {
        return a.getMinX() < b.getMaxX() &&
                a.getMaxX() > b.getMinX() &&
                a.getMinY() < b.getMaxY() &&
                a.getMaxY() > b.getMinY();
    }

    private void resolvePhysicsCollision(BoxCollider2D a, BoxCollider2D b) {
        // Si los dos son estáticos (ej.: dos paredes solapadas), nadie se mueve
        if (a.isStatic() && b.isStatic()) {
            return;
        }

        // Calculamos la penetración en los ejes
        float overlapLeft   = a.getMaxX() - b.getMinX();
        float overlapRight  = b.getMaxX() - a.getMinX();
        float overlapBottom = a.getMaxY() - b.getMinY();
        float overlapTop    = b.getMaxY() - a.getMinY();

        float overlapX = Math.min(overlapLeft, overlapRight);
        float overlapY = Math.min(overlapBottom, overlapTop);

        // Vector de corrección (hacia dónde empujar 'a' respecto a 'b')
        float resolveX = 0;
        float resolveY = 0;

        if (overlapX < overlapY) {
            // Corrección en X
            resolveX = (overlapLeft < overlapRight) ? -overlapX : overlapX;
        } else {
            // Corrección en Y
            resolveY = (overlapBottom < overlapTop) ? -overlapY : overlapY;
        }

        // Aplicar la corrección según quién pueda moverse:
        if (a.isStatic()) {
            // 'a' es una pared, empujamos a 'b' en la dirección opuesta
            b.gameObject.transform.pos.x -= resolveX;
            b.gameObject.transform.pos.y -= resolveY;
        } else if (b.isStatic()) {
            // 'b' es una pared, empujamos 'a'
            a.gameObject.transform.pos.x += resolveX;
            a.gameObject.transform.pos.y += resolveY;
        } else {
            // Si ambos son dinámicos (dos personajes chocándose), se reparten 50/50
            a.gameObject.transform.pos.x += resolveX * 0.5f;
            a.gameObject.transform.pos.y += resolveY * 0.5f;

            b.gameObject.transform.pos.x -= resolveX * 0.5f;
            b.gameObject.transform.pos.y -= resolveY * 0.5f;
        }
    }

    private void notifyCollisionEnter(BoxCollider2D a, BoxCollider2D b, boolean isTrigger) {
        for (Component c : a.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerEnter(b.gameObject);
            else c.onCollisionEnter(b.gameObject);
        }
        for (Component c : b.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerEnter(a.gameObject);
            else c.onCollisionEnter(a.gameObject);
        }
    }

    private void notifyCollisionStay(BoxCollider2D a, BoxCollider2D b, boolean isTrigger) {
        for (Component c : a.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerStay(b.gameObject);
            else c.onCollisionStay(b.gameObject);
        }
        for (Component c : b.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerStay(a.gameObject);
            else c.onCollisionStay(a.gameObject);
        }
    }

    private void notifyCollisionExit(BoxCollider2D a, BoxCollider2D b, boolean isTrigger) {
        for (Component c : a.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerExit(b.gameObject);
            else c.onCollisionExit(b.gameObject);
        }
        for (Component c : b.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerExit(a.gameObject);
            else c.onCollisionExit(a.gameObject);
        }
    }

    private void resolveTilemapCollision(BoxCollider2D collider, Tilemap tilemap){
        if (collider.isStatic()||tilemap==null||tilemap.gameObject == null)return;
        Vector2f tilemapOrigin = tilemap.gameObject.transform.pos;
        int tileW = tilemap.getTileWidth();
        int tileH = tilemap.getTileHeight();

        //obtener los límites del collider en el mundo
        float colMinX = collider.getMinX();
        float colMaxX = collider.getMaxX();
        float colMinY = collider.getMinY();
        float colMaxY = collider.getMaxY();

        //convertir el rango del collider a coordenadas de celdas
        int minTileX = (int) Math.floor((colMinX - tilemapOrigin.x) / tileW);
        int maxTileX = (int) Math.floor((colMaxX - tilemapOrigin.x) / tileW);
        int minTileY = (int) Math.floor((colMinY - tilemapOrigin.y) / tileH);
        int maxTileY = (int) Math.floor((colMaxY - tilemapOrigin.y) / tileH);

        //revisar solo las celdas que el collider está tocando
        for (int tx = minTileX;tx<=maxTileX;tx++){
            for (int ty = minTileY;ty<maxTileY;ty++){
                if (!tilemap.isSolid(tx,ty)) continue;

                //AABB de la baldosa sólida actual
                float tileMinX = tilemapOrigin.x + (tx*tileW);
                float tileMaxX = tileMinX + tileW;
                float tileMinY = tilemapOrigin.y + (ty*tileH);
                float tileMaxY = tileMinY + tileH;

                //Chequeo de penetración
                float overlapLeft = collider.getMaxX() - tileMinX;
                float overlapRight =  tileMinX - collider.getMinX();
                float overlapBottom = collider.getMaxY() - tileMinY;
                float overlapTop = tileMaxY - collider.getMinY();

                if (overlapLeft>0&&overlapRight>0&&overlapBottom>0&&overlapTop>0){
                    float overlapX = Math.min(overlapLeft,overlapRight);
                    float overlapY = Math.min(overlapBottom,overlapTop);

                    //Empujar en el eje donde la penetración sea menor
                    if (overlapX<overlapY){
                        float resolveX = (overlapLeft<overlapRight) ? -overlapX : overlapX;
                        collider.gameObject.transform.pos.x +=resolveX;
                    }else{
                        float resolveY = (overlapBottom<overlapTop) ? -overlapY : overlapY;
                        collider.gameObject.transform.pos.y += resolveY;
                    }
                }
            }
        }
    }


    //GETTERS Y SETTERS
    public void toggleDebugMode() {
        this.debugMode = !this.debugMode;
    }
    public boolean isDebugMode() {
        return this.debugMode;
    }
    public Camera getCamera() {
        return camera;
    }
    public List<GameObject> getGameObjects() {
        return this.gameObjects;
    }
}