package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.alchemistShop.assets.util.ItemDB;
import org.joml.Vector2f;
import org.ravenEngine.audio.AudioSource;
import org.ravenEngine.components.Inventory;
import org.ravenEngine.components.Tilemap;
import org.ravenEngine.core.Engine;
import org.ravenEngine.renderer.FrameBuffer;
import org.ravenEngine.renderer.Sprite;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import java.util.List;
import java.util.Map;

public class ImGuiLayer {

    // ==========================================
    // ATRIBUTOS Y SUBSISTEMAS DEL EDITOR
    // ==========================================
    private final long glfwWindow;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private final SceneHierarchy sceneHierarchy = new SceneHierarchy();
    private final InspectorWindow inspectorWindow;
    private final ContentBrowserPanel contentBrowserPanel;

    //Editor de Codigo
    private CodeEditorWindow codeEditorWindow;

    // Control de scroll de la terminal
    private boolean autoScroll = true;

    // Control de visibilidad de paneles (Menú Ventana)
    private boolean showConsole = true;
    private boolean showContentBrowser = true;
    private boolean showInventory = true;
    private boolean showInspector = true;
    private boolean showViewport = true;
    private boolean showTools = true;
    private boolean showCodeEditor = true;

    // Variables de manipulación y arrastre en Viewport
    private final Vector2f lastMousePos = new Vector2f();
    private boolean isDraggingObject = false;
    private final Vector2f dragOffset = new Vector2f();

    // Snapshot para restaurar la escena al detener la simulación (Play/Stop)
    private String sceneSnapshot = null;

    // Control del sistema de pincel de tiles
    private boolean tileEditMode = false;
    private int selectedTileIndex = 0;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================
    public ImGuiLayer(long glfwWindow) {
        this.inspectorWindow = new InspectorWindow();
        this.codeEditorWindow = new CodeEditorWindow();
        this.contentBrowserPanel = new ContentBrowserPanel(this.codeEditorWindow);
        this.glfwWindow = glfwWindow;
    }


    // ==========================================
    // CICLO DE VIDA (LIFECYCLE)
    // ==========================================
    public void init() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();

        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);

        imGuiGlfw.init(glfwWindow, true);
        imGuiGl3.init("#version 330 core");
    }

    public void update(float dt, FrameBuffer framebuffer, Scene currentScene) {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        ImGuiIO io = ImGui.getIO();

        // 1. ATAJOS GLOBALES DE TECLADO
        boolean ctrlPressed = io.getKeyCtrl();
        boolean sPressed = ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_S, false);
        if (ctrlPressed && sPressed) {
            if (currentScene != null) {
                currentScene.save("src/main/java/org/alchemistShop/assets/level1.json");
                System.out.println("[IDE] Escena guardada con Ctrl + S");
            }
        }

        if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_F1, false)) {
            if (currentScene != null) {
                currentScene.toggleDebugMode();
            }
        }

        // 2. DOCKSPACE Y BARRA PRINCIPAL
        setupDockSpace();
        renderMenuBar();

        // 3. PANELES CONDICIONADOS
        if (showConsole) {
            renderConsoleWindow();
        }

        if (showContentBrowser) {
            contentBrowserPanel.render();
        }

        if (showInventory) {
            renderInventoryWindow();
        }

        if (showInspector) {
            renderInspector(currentScene);
        }

        Tilemap activeTilemap = findActiveTilemap(currentScene);

        if (showViewport) {
            renderViewport(framebuffer, currentScene, activeTilemap);
        }

        if (showTools) {
            renderToolsWindow(activeTilemap);
        }

        if (tileEditMode && activeTilemap != null) {
            renderTilePaletteWindow(activeTilemap);
        }

        // 4. CONTROLES DE SIMULACIÓN
        renderSimulationControls(dt, currentScene);

        // 5. EDITOR DE CODIGO
        codeEditorWindow.update();

        // 6. RENDERIZADO FINAL EN OPENGL
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }


    // ==========================================
    // FUNCIONES DE RENDER (PANELES E INTERFAZ)
    // ==========================================

    private void setupDockSpace() {
        int windowFlags = ImGuiWindowFlags.NoDocking;

        ImGuiViewport mainViewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(mainViewport.getWorkPosX(), mainViewport.getWorkPosY());
        ImGui.setNextWindowSize(mainViewport.getWorkSizeX(), mainViewport.getWorkSizeY());
        ImGui.setNextWindowViewport(mainViewport.getID());

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);
        ImGui.begin("DockSpaceDemo", windowFlags);
        ImGui.popStyleVar(3);

        int dockSpaceId = ImGui.getID("RavenEngineDockSpace");
        ImGui.dockSpace(dockSpaceId, 0.0f, 0.0f, ImGuiDockNodeFlags.None);

        ImGui.end();
    }

    private void renderMenuBar() {
        if (ImGui.beginMainMenuBar()) {

            // MENÚ FILE
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Nueva Escena")) {
                    Scene current = Engine.getCurrentScene();
                    if (current != null) {
                        current.getGameObjects().clear();
                        System.out.println("[IDE] Escena limpiada.");
                    }
                }

                if (ImGui.menuItem("Guardar Escena", "Ctrl+S")) {
                    Scene current = Engine.getCurrentScene();
                    if (current != null) {
                        current.save("src/main/java/org/alchemistShop/assets/level1.json");
                    }
                }

                if (ImGui.menuItem("Recargar Escena")) {
                    Scene current = Engine.getCurrentScene();
                    if (current != null) {
                        current.load("src/main/java/org/alchemistShop/assets/level1.json");
                    }
                }

                if (ImGui.menuItem("Editor de Código", null, codeEditorWindow.isOpen())) {
                    codeEditorWindow.setOpen(!codeEditorWindow.isOpen());
                }

                ImGui.separator();

                //Exportación STANDALONE
                if (ImGui.menuItem("Exportar Juego(Release)")){
                    ProjectExporter.exportBuild("src/main/java/org/alchemistShop/assets/level1.json");
                }

                if (ImGui.menuItem("Salir")) {
                    System.exit(0);
                }

                ImGui.endMenu();
            }

            // MENÚ VENTANA
            if (ImGui.beginMenu("Ventana")) {
                if (ImGui.menuItem("Terminal", null, showConsole)) {
                    showConsole = !showConsole;
                }
                if (ImGui.menuItem("Explorador de Archivos", null, showContentBrowser)) {
                    showContentBrowser = !showContentBrowser;
                }
                if (ImGui.menuItem("Inventario", null, showInventory)) {
                    showInventory = !showInventory;
                }
                if (ImGui.menuItem("Inspector y Jerarquía", null, showInspector)) {
                    showInspector = !showInspector;
                }
                if (ImGui.menuItem("Scene Viewport", null, showViewport)) {
                    showViewport = !showViewport;
                }
                if (ImGui.menuItem("Herramientas", null, showTools)) {
                    showTools = !showTools;
                }

                ImGui.separator();
                if (ImGui.menuItem("Restaurar Paneles")) {
                    showConsole = true;
                    showContentBrowser = true;
                    showInventory = true;
                    showInspector = true;
                    showViewport = true;
                    showTools = true;
                    codeEditorWindow.setOpen(true);
                }

                ImGui.endMenu();
            }

            // MENÚ HELP
            if (ImGui.beginMenu("Help")) {
                if (ImGui.menuItem("Acerca de RavenEngine")) {
                    System.out.println("[RavenEngine IDE] Motor 2D en Java + LWJGL + OpenGL");
                }
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
    }

    private void renderViewport(FrameBuffer framebuffer, Scene currentScene, Tilemap activeTilemap) {
        ImGui.begin("Scene Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        if (framebuffer != null) {
            ImVec2 windowSize = new ImVec2();
            ImGui.getContentRegionAvail(windowSize);

            ImVec2 cursorPos = new ImVec2();
            ImGui.getCursorScreenPos(cursorPos);

            // Dibujar Framebuffer invertido para el sistema de coordenadas de OpenGL
            ImGui.image(framebuffer.getTextureId(), windowSize.x, windowSize.y, 0, 1, 1, 0);

            // ENTRADA DEL MOUSE EN EL VIEWPORT
            if (ImGui.isWindowHovered()) {
                ImVec2 mousePos = ImGui.getMousePos();

                float localX = mousePos.x - cursorPos.x;
                float localY = mousePos.y - cursorPos.y;

                if (currentScene != null && currentScene.getCamera() != null) {
                    var camera = currentScene.getCamera();
                    Vector2f worldCoords = camera.screenToWorld(localX, localY, windowSize.x, windowSize.y);

                    // 1. PANEO DE CÁMARA
                    boolean panButtonDown = ImGui.isMouseDown(2) || (!tileEditMode && ImGui.isMouseDown(1));

                    if (!Engine.IsPlaying() && panButtonDown) {
                        float dx = mousePos.x - lastMousePos.x;
                        float dy = mousePos.y - lastMousePos.y;

                        float worldUnitsPerPixelX = 640.0f / windowSize.x;
                        float worldUnitsPerPixelY = 360.0f / windowSize.y;

                        camera.position.x -= dx * worldUnitsPerPixelX;
                        camera.position.y += dy * worldUnitsPerPixelY;
                    }

                    // 2. ZOOM CON LA RUEDA
                    if (!Engine.IsPlaying() || Engine.IsPaused()) {
                        float wheel = ImGui.getIO().getMouseWheel();
                        if (wheel != 0.0f) {
                            camera.addZoom(wheel);
                        }
                    }

                    // 3. EDICIÓN: PINCEL DE TILES VS ARRASTRE DE GAMEOBJECTS
                    if (tileEditMode && activeTilemap != null) {
                        Vector2f grid = activeTilemap.worldToGrid(worldCoords.x, worldCoords.y);
                        int gx = (int) grid.x;
                        int gy = (int) grid.y;

                        if (ImGui.isMouseDown(0)) {
                            if (activeTilemap.getTile(gx, gy) != selectedTileIndex) {
                                activeTilemap.setTile(gx, gy, selectedTileIndex);
                            }
                        } else if (ImGui.isMouseDown(1)) {
                            if (activeTilemap.getTile(gx, gy) != -1) {
                                activeTilemap.setTile(gx, gy, -1);
                            }
                        }
                    } else {
                        GameObject selected = getSelectedGameObject();

                        if (ImGui.isMouseClicked(0)) {
                            pickObjectAt(worldCoords.x, worldCoords.y, currentScene);
                            selected = getSelectedGameObject();

                            if (selected != null && selected.transform != null) {
                                isDraggingObject = true;
                                dragOffset.set(
                                        worldCoords.x - selected.transform.pos.x,
                                        worldCoords.y - selected.transform.pos.y
                                );
                            }
                        }

                        if (!Engine.IsPlaying() && isDraggingObject && ImGui.isMouseDown(0)) {
                            if (selected != null && selected.transform != null) {
                                selected.transform.pos.x = worldCoords.x - dragOffset.x;
                                selected.transform.pos.y = worldCoords.y - dragOffset.y;
                            }
                        } else if (!ImGui.isMouseDown(0)) {
                            isDraggingObject = false;
                        }
                    }

                    lastMousePos.set(mousePos.x, mousePos.y);
                }
            } else {
                isDraggingObject = false;
            }
        }

        ImGui.end();
    }

    private void renderInspector(Scene currentScene) {
        sceneHierarchy.update(currentScene);
        inspectorWindow.update(sceneHierarchy.getSelectedGameObject());
    }

    private void renderToolsWindow(Tilemap activeTilemap) {
        ImGui.begin("Herramientas");
        ImGui.text("RavenEngine IDE");
        ImGui.separator();

        if (ImGui.button("Modo Pincel de Tiles")) {
            tileEditMode = !tileEditMode;
        }

        if (tileEditMode) {
            ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, "[Pincel Activo]");
            ImGui.text("Clic Izq: Pintar | Clic Der: Borrar");
        } else {
            ImGui.textColored(0.8f, 0.8f, 0.2f, 1.0f, "[Modo Selección]");
        }
        ImGui.end();
    }

    private void renderInventoryWindow() {
        ImGui.begin("Bolsa del alquimista");
        Scene scene = Engine.getCurrentScene();
        if (scene == null) {
            ImGui.textDisabled("Sin escena activa");
            ImGui.end();
            return;
        }

        // Buscar a RavenTom para obtener su inventario
        GameObject player = null;
        for (GameObject go : scene.getGameObjects()) {
            if ("RavenTom".equals(go.getName())) {
                player = go;
                break;
            }
        }
        if (player == null) {
            ImGui.textDisabled("No se encontró al personaje");
            ImGui.end();
            return;
        }

        Inventory inventory = player.getComponent(Inventory.class);
        if (inventory == null) {
            ImGui.textDisabled("El personaje no tiene componente Inventory");
            ImGui.end();
            return;
        }

        Map< String, Integer > items = inventory.getItems();

        if (items == null || items.isEmpty()) {
            ImGui.textColored(0.7f, 0.7f, 0.7f, 1.0f, "La bolsa está vacía.");
        } else {
            ImGui.columns(3, "InventoryColumns", true);
            ImGui.textColored(0.9f, 0.8f, 0.2f, 1.0f, "Objeto");
            ImGui.nextColumn();
            ImGui.textColored(0.9f, 0.8f, 0.2f, 1.0f, "Cant.");
            ImGui.nextColumn();
            ImGui.textColored(0.9f, 0.8f, 0.2f, 1.0f, "Acciones");
            ImGui.nextColumn();
            ImGui.separator();

            String itemToUse = null;
            String itemToDrop = null;

            for (Map.Entry< String, Integer > entry : items.entrySet()) {
                String itemId = entry.getKey();
                int amount = entry.getValue();

                ImGui.text(itemId);
                ImGui.nextColumn();

                ImGui.text("x" + amount);
                ImGui.nextColumn();

                if (ImGui.button("Usar: " + itemId)) {
                    itemToUse = itemId;
                }
                ImGui.sameLine();
                if (ImGui.button("Soltar: " + itemId)) {
                    itemToDrop = itemId;
                }
                ImGui.nextColumn();
            }

            ImGui.columns(1);

            // Procesar acciones de forma segura
            if (itemToUse != null) {
                ItemDB.useItem(itemToUse, player);
                inventory.removeItem(itemToUse, 1);
            }

            if (itemToDrop != null) {
                Vector2f dropPos = new Vector2f(
                        player.transform.pos.x + 20.0f,
                        player.transform.pos.y
                );

                if (inventory.removeItem(itemToDrop, 1)) {
                    GameObject droppedItem = ItemDB.createWorldItem(itemToDrop, dropPos);
                    scene.spawnGameObject(droppedItem);
                    System.out.println("[Inventario] Soltaste: " + itemToDrop);
                }
            }
        }

        ImGui.end();
    }

    private void renderConsoleWindow() {
        ImGui.begin("Terminal RENGINE");

        if (ImGui.button("Limpiar")) {
            EngineConsole.clear();
        }
        ImGui.sameLine();
        if (ImGui.checkbox("Auto-Scroll", autoScroll)) {
            autoScroll = !autoScroll;
        }
        ImGui.separator();

        ImGui.beginChild("ConsoleScrollRegion", 0, 0, false, ImGuiWindowFlags.HorizontalScrollbar);
        synchronized (EngineConsole.getLogs()) {
            for (String log : EngineConsole.getLogs()) {
                if (log.startsWith("[ERROR]")) {
                    ImGui.textColored(1.0f, 0.4f, 0.4f, 1.0f, log);
                } else if (log.startsWith("[Inventario]")) {
                    ImGui.textColored(0.4f, 1.0f, 0.4f, 1.0f, log);
                } else if (log.startsWith("[Scene]")) {
                    ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, log);
                } else {
                    ImGui.textUnformatted(log);
                }
            }
        }

        if (autoScroll && ImGui.getScrollY() >= ImGui.getScrollMaxY()) {
            ImGui.setScrollHereY(1.0f);
        }

        ImGui.endChild();
        ImGui.end();
    }

    private void renderTilePaletteWindow(Tilemap tilemap) {
        ImGui.begin("Paleta de Tiles");

        Spritesheet sheet = tilemap.getSpritesheet();
        if (sheet == null || sheet.size() == 0) {
            ImGui.text("El Tilemap no posee Spritesheet cargado.");
            ImGui.end();
            return;
        }

        if (ImGui.button("Borrador (Vacío)")) {
            selectedTileIndex = -1;
        }
        ImGui.sameLine();
        ImGui.text(selectedTileIndex == -1 ? "Tile actual: [Borrador]" : "Tile actual: #" + selectedTileIndex);

        ImGui.separator();

        int textureId = sheet.getSprite(0).getTexture().getTextureId();
        int columns = 4;

        for (int i = 0; i < sheet.size(); i++) {
            Sprite sprite = sheet.getSprite(i);
            Vector2f[] uv = sprite.getTexCoords();

            ImGui.pushID(i);
            if (ImGui.imageButton(textureId, 32, 32, uv[2].x, uv[2].y, uv[0].x, uv[0].y)) {
                selectedTileIndex = i;
            }
            ImGui.popID();

            if ((i + 1) % columns != 0 && (i + 1) < sheet.size()) {
                ImGui.sameLine();
            }
        }

        ImGui.end();
    }

    private void renderSimulationControls(float dt, Scene currentScene) {
        ImGui.begin("Control de Juego");

        boolean isPlaying = Engine.IsPlaying();
        boolean isPaused = Engine.IsPaused();

        if (isPlaying && !isPaused) ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        if (ImGui.button(" > Play ")) {
            if (!isPlaying) {
                if (currentScene != null) {
                    sceneSnapshot = currentScene.serializeToString();
                    if (currentScene.getCamera() != null) {
                        currentScene.getCamera().resetZoom();
                    }
                }
                Engine.setPlaying(true);
                Engine.setPaused(false);
                if (currentScene != null){
                    for (GameObject go : currentScene.getGameObjects()){
                        AudioSource audio = go.getComponent(AudioSource.class);
                        if (audio != null) audio.play();
                    }
                }
            }
        }
        if (isPlaying && !isPaused) ImGui.popStyleColor();

        ImGui.sameLine();

        if (isPaused) ImGui.pushStyleColor(ImGuiCol.Button, 0.8f, 0.8f, 0.2f, 1.0f);
        if (ImGui.button(" || Pause ")) {
            if (isPlaying) {
                Engine.setPaused(!isPaused);
                if (currentScene != null) {
                    for (GameObject go : currentScene.getGameObjects()) {
                        AudioSource audio = go.getComponent(AudioSource.class);
                        if (audio != null) {
                            if (Engine.IsPaused()) audio.pause();
                            else audio.play();
                        }
                    }
                }
            }
        }
        if (isPaused) ImGui.popStyleColor();

        ImGui.sameLine();

        if (!isPlaying) ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button(" [] Stop ")) {
            if (isPlaying || isPaused) {
                Engine.setPlaying(false);
                Engine.setPaused(false);

                // Silenciamos todoo de raíz
                if (currentScene != null) {
                    for (GameObject go : currentScene.getGameObjects()) {
                        AudioSource audio = go.getComponent(AudioSource.class);
                        if (audio != null) audio.stop();
                    }
                }

                if (sceneSnapshot != null && currentScene != null) {
                    currentScene.loadFromString(sceneSnapshot);
                    if (sceneHierarchy != null) {
                        sceneHierarchy.setSelectedGameObject(null);
                    }
                    System.out.println("[IDE] Escena restaurada al estado de edición.");
                }

                System.out.println("[Motor] Simulación detenida.");
            }
        }
        if (!isPlaying) ImGui.popStyleColor();

        ImGui.sameLine();
        ImGui.text(String.format("Estado: %s", !isPlaying ? "Edición (Detenido)" : (isPaused ? "Pausado" : "Simulando (Play)")));
        ImGui.sameLine();
        ImGui.text(String.format("FPS: %.1f", 1.0f / dt));

        ImGui.end();
    }


    // ==========================================
    // FUNCIONES AUXILIARES Y DE SELECCIÓN
    // ==========================================

    private void pickObjectAt(float worldX, float worldY, Scene scene) {
        GameObject hitObject = null;
        List< GameObject > objects = scene.getGameObjects();

        for (int i = objects.size() - 1; i >= 0; i--) {
            GameObject go = objects.get(i);
            if (go.transform == null) continue;

            float minX = go.transform.pos.x;
            float minY = go.transform.pos.y;
            float maxX = minX + go.transform.scale.x;
            float maxY = minY + go.transform.scale.y;

            if (worldX >= minX && worldX <= maxX && worldY >= minY && worldY <= maxY) {
                hitObject = go;
                break;
            }
        }

        sceneHierarchy.setSelectedGameObject(hitObject);
    }

    private Tilemap findActiveTilemap(Scene scene) {
        if (scene == null) return null;

        GameObject selected = sceneHierarchy.getSelectedGameObject();
        if (selected != null && selected.getComponent(Tilemap.class) != null) {
            return selected.getComponent(Tilemap.class);
        }

        for (GameObject go : scene.getGameObjects()) {
            Tilemap tm = go.getComponent(Tilemap.class);
            if (tm != null) return tm;
        }
        return null;
    }


    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public GameObject getSelectedGameObject() {
        return sceneHierarchy.getSelectedGameObject();
    }
    public CodeEditorWindow getCodeEditorWindow() {return codeEditorWindow;}
}