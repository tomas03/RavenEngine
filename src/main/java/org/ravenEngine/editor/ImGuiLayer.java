package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.joml.Vector2f;
import org.ravenEngine.components.Tilemap;
import org.ravenEngine.core.DialogueManager;
import org.ravenEngine.core.Engine;
import org.ravenEngine.editor.history.CommandHistory;
import org.ravenEngine.editor.history.DeleteGameObjectCommand;
import org.ravenEngine.editor.history.TransformCommand;
import org.ravenEngine.renderer.FrameBuffer;
import org.ravenEngine.renderer.Sprite;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import java.util.List;

public class ImGuiLayer {

    // Sub-sistemas y paneles desacoplados
    private final long glfwWindow;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private final SceneHierarchy sceneHierarchy = new SceneHierarchy();
    private final InspectorWindow inspectorWindow = new InspectorWindow();
    private final CodeEditorWindow codeEditorWindow = new CodeEditorWindow();
    private final ContentBrowserPanel contentBrowserPanel;
    private final ToolsPanel toolsPanel = new ToolsPanel();
    private final ConsolePanel consolePanel = new ConsolePanel();
    private final SimulationPanel simulationPanel = new SimulationPanel();

    // Visibilidad de paneles
    private boolean showConsole = true;
    private boolean showContentBrowser = true;
    private boolean showInspector = true;
    private boolean showViewport = true;
    private boolean showTools = true;
    private boolean showProjectSettings = false;

    // Configuración global (ProjectSettings)
    private final imgui.type.ImString titleBuffer = new imgui.type.ImString(128);
    private final imgui.type.ImString scenePathBuffer = new imgui.type.ImString(256);
    private String currentScenePath = "src/main/java/org/alchemistShop/assets/level1.json";

    // Manipulación en Viewport
    private final Vector2f lastMousePos = new Vector2f();
    private boolean isDraggingObject = false;
    private final Vector2f dragOffset = new Vector2f();
    private final Vector2f dragStartMouse = new Vector2f();
    private final Vector2f initialScale = new Vector2f();
    private final Vector2f transformStartPos = new Vector2f();
    private final Vector2f transformStartScale = new Vector2f();
    private GizmoRenderer.GizmoMode currentGizmoMode = GizmoRenderer.GizmoMode.NONE;
    private int selectedTileIndex = 0;

    public ImGuiLayer(long glfwWindow) {
        this.glfwWindow = glfwWindow;
        this.contentBrowserPanel = new ContentBrowserPanel(this.codeEditorWindow, this);
    }

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

        // 1. ATAJOS GLOBALES
        handleGlobalHotkeys(io, currentScene);

        // 2. DOCKSPACE Y MENÚ
        setupDockSpace();
        renderMenuBar();

        // 3. PANELES MODULARES
        if (showConsole) consolePanel.update();
        if (showContentBrowser) contentBrowserPanel.render();
        if (showInspector) renderInspector(currentScene);
        if (showTools) toolsPanel.update(findActiveTilemap(currentScene));

        Tilemap activeTilemap = findActiveTilemap(currentScene);
        if (showViewport) renderViewport(framebuffer, currentScene, activeTilemap);
        if (toolsPanel.isTileEditMode() && activeTilemap != null) renderTilePaletteWindow(activeTilemap);
        if (showProjectSettings) renderProjectSettingsWindow();

        // 4. SIMULACIÓN Y DIÁLOGOS
        simulationPanel.update(dt, currentScene, sceneHierarchy);
        DialogueManager.update(dt);
        if (DialogueManager.isActive() && org.ravenEngine.io.KeyListener.keyBeginPress(org.lwjgl.glfw.GLFW.GLFW_KEY_E)) {
            DialogueManager.advance();
        }

        codeEditorWindow.update();

        // 5. RENDER FINAL
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void handleGlobalHotkeys(ImGuiIO io, Scene currentScene) {
        if (io.getKeyCtrl() && ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_S, false)) {
            saveCurrentScene(currentScene);
        }
        if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_F1, false) && currentScene != null) {
            currentScene.toggleDebugMode();
        }
        if (!Engine.IsPlaying() && !io.getWantTextInput()) {
            if (io.getKeyCtrl() && ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_Z, false)) {
                CommandHistory.undo();
            } else if (io.getKeyCtrl() && (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_Y, false) ||
                    (io.getKeyShift() && ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_Z, false)))) {
                CommandHistory.redo();
            }
        }
    }

    private void renderInspector(Scene currentScene) {
        sceneHierarchy.update(currentScene);
        inspectorWindow.update(sceneHierarchy.getSelectedGameObject());
    }

    private void renderViewport(FrameBuffer framebuffer, Scene currentScene, Tilemap activeTilemap) {
        ImGui.begin("Scene Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);
        if (framebuffer != null) {
            ImVec2 windowSize = new ImVec2();
            ImGui.getContentRegionAvail(windowSize);
            ImVec2 cursorPos = new ImVec2();
            ImGui.getCursorScreenPos(cursorPos);

            ImGui.image(framebuffer.getTextureId(), windowSize.x, windowSize.y, 0, 1, 1, 0);
            handleViewportDragDrop(currentScene, cursorPos, windowSize);

            if (ImGui.isItemHovered()) {
                handleViewportInput(currentScene, activeTilemap, cursorPos, windowSize);
            } else {
                isDraggingObject = false;
                currentGizmoMode = GizmoRenderer.GizmoMode.NONE;
            }

            renderDialogueBox(cursorPos, windowSize);
        }
        ImGui.end();
    }

    private void handleViewportDragDrop(Scene scene, ImVec2 cursorPos, ImVec2 windowSize) {
        if (ImGui.beginDragDropTarget()) {
            Object payload = ImGui.acceptDragDropPayload("CONTENT_BROWSER_ITEM");
            if (payload != null && scene != null) {
                String path = (payload instanceof byte[]) ? new String((byte[]) payload) : payload.toString();
                ImVec2 mousePos = ImGui.getMousePos();
                Vector2f dropWorld = scene.getCamera().screenToWorld(mousePos.x - cursorPos.x, mousePos.y - cursorPos.y, windowSize.x, windowSize.y);

                if (path.endsWith(".prefab")) {
                    spawnPrefabAt(path, dropWorld, scene);
                } else if (path.endsWith(".png") || path.endsWith(".jpg")) {
                    String name = new java.io.File(path).getName().replaceFirst("[.][^.]+$", "");
                    spawnSpriteAt(name, path, dropWorld, scene);
                }
            }
            ImGui.endDragDropTarget();
        }
    }

    private void handleViewportInput(Scene scene, Tilemap tilemap, ImVec2 cursorPos, ImVec2 windowSize) {
        if (scene == null || scene.getCamera() == null) return;
        var camera = scene.getCamera();
        ImVec2 mousePos = ImGui.getMousePos();
        float localX = mousePos.x - cursorPos.x;
        float localY = mousePos.y - cursorPos.y;

        if (!Engine.IsPlaying() && toolsPanel.isShowGrid()) {
            renderEditorGrid(camera, windowSize);
        }

        Vector2f world = camera.screenToWorld(localX, localY, windowSize.x, windowSize.y);

        // Atajos en Viewport (W, R, G, Supr, Ctrl+D, F)
        if (!Engine.IsPlaying() && !ImGui.getIO().getWantTextInput()) {
            if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_W, false)) toolsPanel.setCurrentGizmoType(GizmoRenderer.GizmoType.TRANSLATE);
            if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_R, false)) toolsPanel.setCurrentGizmoType(GizmoRenderer.GizmoType.SCALE);
            if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_G, false)) toolsPanel.setShowGrid(!toolsPanel.isShowGrid());
            if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE)) deleteSelectedObject(scene);
            if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_D, false)) duplicateSelectedObject(scene);
            if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_F, false)) focusOnSelectedObject(scene);
        }

        // Paneo y Zoom
        if (!Engine.IsPlaying() && (ImGui.isMouseDown(2) || (!toolsPanel.isTileEditMode() && ImGui.isMouseDown(1)))) {
            float dx = mousePos.x - lastMousePos.x;
            float dy = mousePos.y - lastMousePos.y;
            camera.position.x -= dx * ((camera.getProjectionWidth() * camera.getZoom()) / windowSize.x);
            camera.position.y += dy * ((camera.getProjectionHeight() * camera.getZoom()) / windowSize.y);
        }
        if (!Engine.IsPlaying() || Engine.IsPaused()) {
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0.0f) camera.addZoom(wheel);
        }

        // Modo Pincel vs Manipulación con Gizmos
        if (toolsPanel.isTileEditMode() && tilemap != null) {
            Vector2f grid = tilemap.worldToGrid(world.x, world.y);
            if (ImGui.isMouseDown(0)) tilemap.setTile((int) grid.x, (int) grid.y, selectedTileIndex);
            else if (ImGui.isMouseDown(1)) tilemap.setTile((int) grid.x, (int) grid.y, -1);
        } else {
            processGizmoInteraction(scene, world);
        }

        lastMousePos.set(mousePos.x, mousePos.y);
    }

    private void processGizmoInteraction(Scene scene, Vector2f world) {
        GameObject selected = getSelectedGameObject();
        GizmoRenderer.GizmoMode hovered = (selected != null && !isDraggingObject) ? GizmoRenderer.testHit(selected, world) : GizmoRenderer.GizmoMode.NONE;

        if (ImGui.isMouseClicked(0)) {
            if (selected != null && hovered != GizmoRenderer.GizmoMode.NONE) {
                currentGizmoMode = hovered;
                isDraggingObject = true;
                startTransformDrag(selected, world);
            } else {
                pickObjectAt(world.x, world.y, scene);
                selected = getSelectedGameObject();
                if (selected != null && selected.transform != null) {
                    currentGizmoMode = GizmoRenderer.GizmoMode.FREE;
                    isDraggingObject = true;
                    startTransformDrag(selected, world);
                }
            }
        }

        if (!Engine.IsPlaying() && isDraggingObject && ImGui.isMouseDown(0) && selected != null && selected.transform != null) {
            applyGizmoDrag(selected, world);
        } else if (!ImGui.isMouseDown(0)) {
            finishTransformDrag(selected);
        }

        if (selected != null) {
            GizmoRenderer.renderGizmos(selected, isDraggingObject ? currentGizmoMode : hovered, toolsPanel.getCurrentGizmoType());
        }
    }

    private void startTransformDrag(GameObject go, Vector2f world) {
        if (go.transform == null) return;
        transformStartPos.set(go.transform.pos);
        transformStartScale.set(go.transform.scale);
        initialScale.set(go.transform.scale);
        dragStartMouse.set(world);
        dragOffset.set(world.x - go.transform.pos.x, world.y - go.transform.pos.y);
    }

    private void applyGizmoDrag(GameObject selected, Vector2f world) {
        boolean snap = toolsPanel.isSnapToGrid() || ImGui.getIO().getKeyCtrl();
        float step = toolsPanel.getSnapGridSize();

        if (toolsPanel.getCurrentGizmoType() == GizmoRenderer.GizmoType.TRANSLATE) {
            float rawX = snap ? applyGridSnap(world.x - dragOffset.x, step) : world.x - dragOffset.x;
            float rawY = snap ? applyGridSnap(world.y - dragOffset.y, step) : world.y - dragOffset.y;
            if (currentGizmoMode == GizmoRenderer.GizmoMode.AXIS_X || currentGizmoMode == GizmoRenderer.GizmoMode.FREE) selected.transform.pos.x = rawX;
            if (currentGizmoMode == GizmoRenderer.GizmoMode.AXIS_Y || currentGizmoMode == GizmoRenderer.GizmoMode.FREE) selected.transform.pos.y = rawY;
        } else {
            float dx = snap ? applyGridSnap(world.x - dragStartMouse.x, step) : world.x - dragStartMouse.x;
            float dy = snap ? applyGridSnap(world.y - dragStartMouse.y, step) : world.y - dragStartMouse.y;
            if (currentGizmoMode == GizmoRenderer.GizmoMode.AXIS_X) selected.transform.scale.x = Math.max(step, initialScale.x + dx);
            else if (currentGizmoMode == GizmoRenderer.GizmoMode.AXIS_Y) selected.transform.scale.y = Math.max(step, initialScale.y + dy);
            else if (currentGizmoMode == GizmoRenderer.GizmoMode.FREE) {
                float avg = snap ? applyGridSnap((dx + dy) * 0.5f, step) : (dx + dy) * 0.5f;
                selected.transform.scale.set(Math.max(step, initialScale.x + avg), Math.max(step, initialScale.y + avg));
            }
        }
    }

    private void finishTransformDrag(GameObject selected) {
        if (isDraggingObject && selected != null && selected.transform != null) {
            if (!transformStartPos.equals(selected.transform.pos) || !transformStartScale.equals(selected.transform.scale)) {
                CommandHistory.executeCommand(new TransformCommand(selected, transformStartPos, selected.transform.pos, transformStartScale, selected.transform.scale));
            }
        }
        isDraggingObject = false;
        currentGizmoMode = GizmoRenderer.GizmoMode.NONE;
    }

    private void renderEditorGrid(org.ravenEngine.renderer.Camera camera, ImVec2 windowSize) {
        Vector2f tl = camera.screenToWorld(0, 0, windowSize.x, windowSize.y);
        Vector2f br = camera.screenToWorld(windowSize.x, windowSize.y, windowSize.x, windowSize.y);
        float minX = Math.min(tl.x, br.x), maxX = Math.max(tl.x, br.x);
        float minY = Math.min(tl.y, br.y), maxY = Math.max(tl.y, br.y);

        float step = toolsPanel.getSnapGridSize();
        while (((maxX - minX) / step) > 140.0f) step *= 2.0f;

        float startX = (float) Math.floor(minX / step) * step;
        float startY = (float) Math.floor(minY / step) * step;

        org.joml.Vector3f gridCol = new org.joml.Vector3f(0.18f, 0.20f, 0.25f);
        for (float x = startX; x <= maxX; x += step) {
            org.ravenEngine.renderer.DebugDraw.addLine2D(new Vector2f(x, minY), new Vector2f(x, maxY), (Math.abs(x) < 0.001f) ? new org.joml.Vector3f(0.22f, 0.45f, 0.22f) : gridCol);
        }
        for (float y = startY; y <= maxY; y += step) {
            org.ravenEngine.renderer.DebugDraw.addLine2D(new Vector2f(minX, y), new Vector2f(maxX, y), (Math.abs(y) < 0.001f) ? new org.joml.Vector3f(0.45f, 0.22f, 0.22f) : gridCol);
        }
    }

    private void setupDockSpace() {
        ImGuiViewport vp = ImGui.getMainViewport();
        ImGui.setNextWindowPos(vp.getWorkPosX(), vp.getWorkPosY());
        ImGui.setNextWindowSize(vp.getWorkSizeX(), vp.getWorkSizeY());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);
        ImGui.begin("DockSpaceDemo", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus);
        ImGui.popStyleVar(3);
        ImGui.dockSpace(ImGui.getID("RavenEngineDockSpace"), 0.0f, 0.0f, ImGuiDockNodeFlags.None);
        ImGui.end();
    }

    private void renderMenuBar() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Nueva Escena")) {
                    Scene current = Engine.getCurrentScene();
                    if (current != null) {
                        current.getGameObjects().clear();
                        if (current.getPhysics2D() != null) current.getPhysics2D().clear();
                        currentScenePath = null;
                        sceneHierarchy.setSelectedGameObject(null);
                    }
                }
                if (ImGui.menuItem("Guardar Escena", "Ctrl+S")) saveCurrentScene(Engine.getCurrentScene());
                if (ImGui.menuItem("Configuración del Proyecto")) showProjectSettings = !showProjectSettings;
                if (ImGui.menuItem("Salir")) System.exit(0);
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Ventana")) {
                if (ImGui.menuItem("Terminal", null, showConsole)) showConsole = !showConsole;
                if (ImGui.menuItem("Explorador de Archivos", null, showContentBrowser)) showContentBrowser = !showContentBrowser;
                if (ImGui.menuItem("Inspector y Jerarquía", null, showInspector)) showInspector = !showInspector;
                if (ImGui.menuItem("Scene Viewport", null, showViewport)) showViewport = !showViewport;
                if (ImGui.menuItem("Herramientas", null, showTools)) showTools = !showTools;
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }
    }

    private void renderProjectSettingsWindow() {
        ImGui.begin("Configuración del Proyecto", ImGuiWindowFlags.AlwaysAutoResize);
        var settings = org.ravenEngine.core.ProjectSettings.get();
        ImGui.inputText("Nombre", titleBuffer);
        ImGui.inputText("Escena Inicial", scenePathBuffer);
        if (ImGui.button("Guardar Cambios")) {
            settings.gameTitle = titleBuffer.get().trim();
            settings.defaultScenePath = scenePathBuffer.get().trim();
            settings.save();
            showProjectSettings = false;
        }
        ImGui.end();
    }

    private void renderTilePaletteWindow(Tilemap tilemap) {
        ImGui.begin("Paleta de Tiles");
        Spritesheet sheet = tilemap.getSpritesheet();
        if (sheet != null && sheet.size() > 0) {
            int texId = sheet.getSprite(0).getTexture().getTextureId();
            for (int i = 0; i < sheet.size(); i++) {
                Vector2f[] uv = sheet.getSprite(i).getTexCoords();
                ImGui.pushID(i);
                if (ImGui.imageButton(texId, 32, 32, uv[2].x, uv[2].y, uv[0].x, uv[0].y)) selectedTileIndex = i;
                ImGui.popID();
                if ((i + 1) % 4 != 0 && (i + 1) < sheet.size()) ImGui.sameLine();
            }
        }
        ImGui.end();
    }

    private void renderDialogueBox(ImVec2 pos, ImVec2 size) {
        if (!DialogueManager.isActive()) return;
        float w = Math.min(size.x * 0.90f, 640.0f);
        ImGui.setNextWindowPos(pos.x + (size.x - w) * 0.5f, pos.y + size.y - 141.0f);
        ImGui.setNextWindowSize(w, 125.0f);
        if (ImGui.begin("##Dialogue", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove)) {
            ImGui.text(DialogueManager.getDisplayedText());
        }
        ImGui.end();
    }

    private float applyGridSnap(float v, float step) { return Math.round(v / step) * step; }

    private void pickObjectAt(float x, float y, Scene s) {
        if (s == null) return;
        List<GameObject> objs = s.getGameObjects();
        for (int i = objs.size() - 1; i >= 0; i--) {
            GameObject go = objs.get(i);
            if (go.transform == null) continue;
            if (x >= go.transform.pos.x && x <= go.transform.pos.x + go.transform.scale.x &&
                    y >= go.transform.pos.y && y <= go.transform.pos.y + go.transform.scale.y) {
                sceneHierarchy.setSelectedGameObject(go);
                return;
            }
        }
        sceneHierarchy.setSelectedGameObject(null);
    }

    private Tilemap findActiveTilemap(Scene s) {
        if (s == null) return null;
        for (GameObject go : s.getGameObjects()) {
            Tilemap tm = go.getComponent(Tilemap.class);
            if (tm != null) return tm;
        }
        return null;
    }

    private void duplicateSelectedObject(Scene scene) {
        GameObject sel = getSelectedGameObject();
        if (sel == null || scene == null) return;
        String json = org.ravenEngine.util.ComponentSerializer.getGson().toJson(sel);
        GameObject copy = org.ravenEngine.util.ComponentSerializer.getGson().fromJson(json, GameObject.class);
        if (copy != null && copy.transform != null) {
            copy.setName(sel.getName() + " (Copy)");
            copy.transform.pos.add(16.0f, 16.0f);
            for (var c : copy.getComponents()) c.gameObject = copy;
            scene.addGameObjectToScene(copy);
            sceneHierarchy.setSelectedGameObject(copy);
        }
    }

    private void deleteSelectedObject(Scene scene) {
        GameObject sel = getSelectedGameObject();
        if (sel != null && scene != null) {
            sceneHierarchy.setSelectedGameObject(null);
            CommandHistory.executeCommand(new DeleteGameObjectCommand(scene, sel));
        }
    }

    private void focusOnSelectedObject(Scene scene) {
        GameObject sel = getSelectedGameObject();
        if (sel != null && scene != null && scene.getCamera() != null) {
            var cam = scene.getCamera();
            cam.position.set(sel.transform.pos.x + sel.transform.scale.x * 0.5f - (cam.getProjectionWidth() * cam.getZoom()) * 0.5f,
                    sel.transform.pos.y + sel.transform.scale.y * 0.5f - (cam.getProjectionHeight() * cam.getZoom()) * 0.5f);
        }
    }

    public void saveCurrentScene(Scene scene) {
        if (scene != null && currentScenePath != null) scene.save(currentScenePath);
    }

    public void loadSceneFromPath(String path, Scene scene) {
        if (scene != null && path != null && path.endsWith(".json")) {
            scene.load(path);
            currentScenePath = path;
            sceneHierarchy.setSelectedGameObject(null);
        }
    }

    private void spawnSpriteAt(String name, String path, Vector2f pos, Scene s) {
        GameObject go = new GameObject(name);
        var tex = org.ravenEngine.util.AssetPool.getTexture(path);
        float w = (tex != null && tex.getWidth() > 0) ? tex.getWidth() : 32.0f;
        float h = (tex != null && tex.getHeight() > 0) ? tex.getHeight() : 32.0f;
        go.transform.pos.set(pos.x - w * 0.5f, pos.y - h * 0.5f);
        go.transform.scale.set(w, h);
        if (tex != null) go.addComponent(new org.ravenEngine.renderer.SpriteRenderer(new Sprite(tex)));
        s.addGameObjectToScene(go);
        sceneHierarchy.setSelectedGameObject(go);
    }

    private void spawnPrefabAt(String path, Vector2f pos, Scene s) {
        try (var r = new java.io.FileReader(path)) {
            GameObject go = org.ravenEngine.util.ComponentSerializer.getGson().fromJson(r, GameObject.class);
            if (go != null && go.transform != null) {
                go.transform.pos.set(pos.x - go.transform.scale.x * 0.5f, pos.y - go.transform.scale.y * 0.5f);
                for (var c : go.getComponents()) c.gameObject = go;
                s.addGameObjectToScene(go);
                sceneHierarchy.setSelectedGameObject(go);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }

    public GameObject getSelectedGameObject() { return sceneHierarchy.getSelectedGameObject(); }
    public CodeEditorWindow getCodeEditorWindow() { return codeEditorWindow; }
}