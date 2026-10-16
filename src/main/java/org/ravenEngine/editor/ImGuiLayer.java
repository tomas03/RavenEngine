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
import org.ravenEngine.core.Engine;
import org.ravenEngine.renderer.FrameBuffer;
import org.ravenEngine.renderer.Sprite;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import java.util.List;

public class ImGuiLayer {
    private final long glfwWindow;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private final SceneHierarchy sceneHierarchy = new SceneHierarchy();
    private final InspectorWindow inspectorWindow = new InspectorWindow();

    private String sceneSnapshot = null;

    // --- VARIABLES DEL SISTEMA DE PINCEL (TILEMAP) ---
    private boolean tileEditMode = false;
    private int selectedTileIndex = 0;

    // CONSTRUCTORES
    public ImGuiLayer(long glfwWindow) {
        this.glfwWindow = glfwWindow;
    }

    // FUNCIONES
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
        //verificar si se mantiene presionado el ctrl y acaba de tocar la S
        boolean ctrlPressed = io.getKeyCtrl();
        boolean sPressed = ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_S);

        if (ctrlPressed&&sPressed){
            if (currentScene!=null){
                currentScene.save("alchemistShop/assets/level1.json");
                System.out.println("[IDE] Escena guardada con Ctrl + S");
            }
        }

        // 2. ATAJO DEBUG: F1 (isKeyPressed solo dispara 1 vez por pulsación, sin rebote)
        if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_F1, false)) {
            if (currentScene != null) {
                currentScene.toggleDebugMode();
            }
        }

        // 1. DOCKSPACE
        setupDockSpace(currentScene);

        // 2. BUSCAR TILEMAP ACTIVO
        Tilemap activeTilemap = findActiveTilemap(currentScene);

        // 3. VENTANA DE HERRAMIENTAS
        ImGui.begin("Herramientas");
        ImGui.text("RavenEngine IDE");
        ImGui.separator();
        ImGui.text(String.format("FPS: %.1f", 1.0f / dt));
        ImGui.text(String.format("Frame Time: %.3f ms", dt * 1000.0f));
        ImGui.separator();

        // Selector de Modo: Inspección vs. Pincel
        if (ImGui.checkbox("Modo Pincel de Tiles", tileEditMode)) {
            tileEditMode = !tileEditMode;
        }

        if (tileEditMode) {
            ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, "[Pincel Activo]");
            ImGui.text("Clic Izq: Pintar | Clic Der: Borrar");
        } else {
            ImGui.textColored(0.8f, 0.8f, 0.2f, 1.0f, "[Modo Selección]");
        }
        ImGui.end();

        // 4. VENTANA DE PALETA DE TILES
        if (tileEditMode && activeTilemap != null) {
            renderTilePaletteWindow(activeTilemap);
        }

        // 5. VIEWPORT DEL JUEGO
        ImGui.begin("Scene Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        if (framebuffer != null) {
            ImVec2 windowSize = new ImVec2();
            ImGui.getContentRegionAvail(windowSize);

            ImVec2 cursorPos = new ImVec2();
            ImGui.getCursorScreenPos(cursorPos);

            // Dibujar Framebuffer invertido para OpenGL
            ImGui.image(framebuffer.getTextureId(), windowSize.x, windowSize.y, 0, 1, 1, 0);

            // ==========================================
            // ENTRADA EN EL VIEWPORT (MOUSE)
            // ==========================================
            if (ImGui.isWindowHovered()) {
                ImVec2 mousePos = ImGui.getMousePos();

                float localX = mousePos.x - cursorPos.x;
                float localY = mousePos.y - cursorPos.y;

                float u = localX / windowSize.x;
                float v = localY / windowSize.y;

                float ndcX = (u * 2.0f) - 1.0f;
                float ndcY = -((v * 2.0f) - 1.0f);

                if (currentScene != null && currentScene.getCamera() != null) {
                    Vector2f worldCoords = currentScene.getCamera().screenToWorld(localX, localY, windowSize.x, windowSize.y);

                    if (tileEditMode && activeTilemap != null) {
                        // --- MODO PINTAR / BORRAR TILES (Arrastre continuo) ---
                        Vector2f grid = activeTilemap.worldToGrid(worldCoords.x, worldCoords.y);
                        int gx = (int) grid.x;
                        int gy = (int) grid.y;

                        // Clic Izquierdo -> Pintar
                        if (ImGui.isMouseDown(0)) {
                            if (activeTilemap.getTile(gx, gy) != selectedTileIndex) {
                                activeTilemap.setTile(gx, gy, selectedTileIndex);
                            }
                        }
                        // Clic Derecho -> Borrar
                        else if (ImGui.isMouseDown(1)) {
                            if (activeTilemap.getTile(gx, gy) != -1) {
                                activeTilemap.setTile(gx, gy, -1);
                            }
                        }
                    } else {
                        // --- MODO SELECCIÓN CLÁSICO (Picking de GameObjects) ---
                        if (ImGui.isMouseClicked(0)) {
                            pickObjectAt(worldCoords.x, worldCoords.y, currentScene);
                        }
                    }
                }
            }
        }

        ImGui.end();

        // 6. BARRA DE CONTROL DE SIMULACIÓN
        renderSimulationControls(dt, currentScene);

        // 7. JERARQUÍA E INSPECTOR
        sceneHierarchy.update(currentScene);
        inspectorWindow.update(sceneHierarchy.getSelectedGameObject());

        // 8. RENDERIZADO FINAL
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private Tilemap findActiveTilemap(Scene scene) {
        if (scene == null) return null;

        // Si tenemos un GameObject seleccionado que contiene Tilemap, usamos ese
        GameObject selected = sceneHierarchy.getSelectedGameObject();
        if (selected != null && selected.getComponent(Tilemap.class) != null) {
            return selected.getComponent(Tilemap.class);
        }

        // Si no, buscamos el primer Tilemap disponible en la escena
        for (GameObject go : scene.getGameObjects()) {
            Tilemap tm = go.getComponent(Tilemap.class);
            if (tm != null) return tm;
        }
        return null;
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
            // Botón con la miniatura del sprite
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
                    System.out.println("[IDE] Snapshot de escena guardado en memoria.");
                }
                Engine.setPlaying(true);
                Engine.setPaused(false);
                System.out.println("Simulación iniciada.");
            }
        }
        if (isPlaying && !isPaused) ImGui.popStyleColor();

        ImGui.sameLine();

        if (isPaused) ImGui.pushStyleColor(ImGuiCol.Button, 0.8f, 0.8f, 0.2f, 1.0f);
        if (ImGui.button(" || Pause ")) {
            if (isPlaying) {
                Engine.setPaused(!isPaused);
                System.out.println("Pausa: " + !isPaused);
            }
        }
        if (isPaused) ImGui.popStyleColor();

        ImGui.sameLine();

        if (!isPlaying) ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button(" [] Stop ")) {
            if (isPlaying || isPaused) {
                Engine.setPlaying(false);
                Engine.setPaused(false);

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

        ImGui.separator();
        ImGui.text(String.format("Estado: %s", !isPlaying ? "Edición (Detenido)" : (isPaused ? "Pausado" : "Simulando (Play)")));
        ImGui.text(String.format("FPS: %.1f", 1.0f / dt));

        ImGui.end();
    }

    private void setupDockSpace(Scene currentScene) {
        int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking;

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

        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("Archivo")) {
                if (ImGui.menuItem("Guardar Escena(ctrl+S)")) {
                    if (currentScene != null) {
                        currentScene.save("alchemistShop/assets/level1.json");
                    }
                }
                if (ImGui.menuItem("Recargar Escena")) {
                    if (currentScene != null) {
                        currentScene.load("alchemistShop/assets/level1.json");
                    }
                }
                if (ImGui.menuItem("Salir")) {
                    System.exit(0);
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Ventanas")) {
                ImGui.textDisabled("Opciones de layout");
                ImGui.endMenu();
            }
            ImGui.endMenuBar();
        }

        ImGui.end();
    }

    private void pickObjectAt(float worldX, float worldY, Scene scene) {
        GameObject hitObject = null;
        List<GameObject> objects = scene.getGameObjects();

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

    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }
    // GETTERS Y SETTERS
    public GameObject getSelectedGameObject() {
        return sceneHierarchy.getSelectedGameObject();
    }


}