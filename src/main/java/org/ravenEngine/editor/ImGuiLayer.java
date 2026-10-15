package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.joml.Vector2f;
import org.ravenEngine.core.Engine;
import org.ravenEngine.renderer.FrameBuffer;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import java.util.List;

public class ImGuiLayer {
    private final long glfwWindow;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private final SceneHierarchy sceneHierarchy = new SceneHierarchy();
    private final InspectorWindow inspectorWindow = new InspectorWindow();

    //CONSTRUCTORES
    public ImGuiLayer(long glfwWindow) {
        this.glfwWindow = glfwWindow;
    }

    //FUNCIONES
    public void init() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();

        // Habilitar soporte de ventanas acoplables (Docking) y navegación por teclado
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);

        imGuiGlfw.init(glfwWindow, true);
        imGuiGl3.init("#version 330 core");
    }

    public void update(float dt, FrameBuffer framebuffer, Scene currentScene) {
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        // 1. CREAR EL DOCK SPACE PRINCIPAL (Cubre toda la pantalla)
        setupDockSpace(currentScene);

        // 2. VENTANA DE HERRAMIENTAS / CONTROL
        ImGui.begin("Herramientas");
        ImGui.text("RavenEngine IDE");
        ImGui.separator();
        ImGui.text(String.format("FPS: %.1f", 1.0f / dt));
        ImGui.text(String.format("Frame Time: %.3f ms", dt * 1000.0f));
        ImGui.separator();
        if (ImGui.button("Guardar Escena")) {
            if (currentScene!=null){
                currentScene.save("assets/scenes/level1.json");
            }
        }
        ImGui.end();

        // --- 4. VIEWPORT DEL JUEGO (FRAMEBUFFER) ---
        ImGui.begin("Scene Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        if (framebuffer != null) {
            ImVec2 windowSize = new ImVec2();
            ImGui.getContentRegionAvail(windowSize);

            // Obtenemos la posición de inicio del contenido (esquina superior izquierda de la imagen)
            ImVec2 cursorPos = new ImVec2();
            ImGui.getCursorScreenPos(cursorPos);

            // Dibujar la textura del Framebuffer
            ImGui.image(framebuffer.getTextureId(), windowSize.x, windowSize.y, 0, 1, 1, 0);

            // ==========================================
            // MOUSE PICKING 2D
            // ==========================================
            if (ImGui.isWindowHovered() && ImGui.isMouseClicked(0)) {
                ImVec2 mousePos = ImGui.getMousePos();

                // 1. Mouse relativo al Viewport (de 0 a windowSize)
                float localX = mousePos.x - cursorPos.x;
                float localY = mousePos.y - cursorPos.y;

                // 2. Normalizar coordenadas de 0.0 a 1.0
                float u = localX / windowSize.x;
                float v = localY / windowSize.y;

                // 3. Convertir a coordenadas normalizadas de OpenGL (-1 a 1)
                // Invertimos uve porque en ImGui Y crece hacia abajo y en OpenGL hacia arriba
                float ndcX = (u * 2.0f) - 1.0f;
                float ndcY = -((v * 2.0f) - 1.0f);

                if (currentScene != null && currentScene.getCamera() != null) {
                    Vector2f worldCords = currentScene.getCamera().screenToWorld(ndcX, ndcY);
                    pickObjectAt(worldCords.x, worldCords.y, currentScene);
                }
            }
        }

        ImGui.end();

        // --- BARRA DE CONTROL DE SIMULACIÓN (PLAY / PAUSE / STOP) ---
        ImGui.begin("Control de Juego");

        boolean isPlaying = Engine.IsPlaying();
        boolean isPaused = Engine.IsPaused();

        // BOTÓN PLAY
        if (isPlaying && !isPaused) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f); // Verde brillante si está corriendo
        }
        if (ImGui.button(" > Play ")) {
            Engine.setPlaying(true);
            Engine.setPaused(false);
            System.out.println("Simulación iniciada.");
        }
        if (isPlaying && !isPaused) {
            ImGui.popStyleColor();
        }

        ImGui.sameLine();

        // BOTÓN PAUSE
        if (isPaused) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0.8f, 0.8f, 0.2f, 1.0f); // Amarillo si está en pausa
        }
        if (ImGui.button(" || Pause ")) {
            if (isPlaying) {
                Engine.setPaused(!isPaused);
                System.out.println("Pausa: " + !isPaused);
            }
        }
        if (isPaused) {
            ImGui.popStyleColor();
        }

        ImGui.sameLine();

        // BOTÓN STOP
        if (!isPlaying) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.2f, 0.2f, 1.0f); // Rojo suave seh está detenido
        }
        if (ImGui.button(" [] Stop ")) {
            Engine.setPlaying(false);
            System.out.println("[Motor] Simulación detenida.");
        }
        if (!isPlaying) {
            ImGui.popStyleColor();
        }


        ImGui.separator();
        ImGui.text(String.format("Estado: %s", !isPlaying ? "Edición (Detenido)" : (isPaused ? "Pausado" : "Simulando (Play)")));
        ImGui.text(String.format("FPS: %.1f", 1.0f / dt));

        ImGui.end();

        sceneHierarchy.update(currentScene);
        inspectorWindow.update(sceneHierarchy.getSelectedGameObject());

        // 4. Renderizado final a OpenGL
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void setupDockSpace(Scene currentScene) {
        int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking;

        // Forzar a que ocupe exactamente la resolución de la ventana principal
        ImGuiViewport mainViewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(mainViewport.getWorkPosX(), mainViewport.getWorkPosY());
        ImGui.setNextWindowSize(mainViewport.getWorkSizeX(), mainViewport.getWorkSizeY());
        ImGui.setNextWindowViewport(mainViewport.getID());

        // Quitar bordes, títulos y comportamiento de ventana común
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);
        ImGui.begin("DockSpaceDemo", windowFlags);
        ImGui.popStyleVar(3);

        // Generar el espacio de acople central
        int dockSpaceId = ImGui.getID("RavenEngineDockSpace");
        ImGui.dockSpace(dockSpaceId, 0.0f, 0.0f, ImGuiDockNodeFlags.None);

        // Barra de menú superior (File, Edit, etc.)
        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("Archivo")) {
                if (ImGui.menuItem("Guardar Escena(ctrl+S)")) {
                    if (currentScene!=null){
                        currentScene.save("assets/scenes/level1.json");
                    }
                }
                // OJO: currentScene.load() TIENE que estar adentro de las llaves del if
                if (ImGui.menuItem("Recargar Escena")) {
                    if (currentScene != null) {
                        currentScene.load("assets/scenes/level1.json");
                    }
                }
                if (ImGui.menuItem("Salir")) {
                    // Cierre controlado
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
        List< GameObject > objects = scene.getGameObjects();

        // Recorremos de atrás hacia adelante para seleccionar el objeto superior
        for (int i = objects.size() - 1; i >= 0; i--) {
            GameObject go = objects.get(i);
            if (go.transform == null) continue;

            float minX = go.transform.pos.x;
            float minY = go.transform.pos.y;
            float maxX = minX + go.transform.scale.x;
            float maxY = minY + go.transform.scale.y;

            // Chequeo AABB (Punto adentro de la caja)
            if (worldX >= minX && worldX <= maxX && worldY >= minY && worldY <= maxY) {
                hitObject = go;
                break;
            }
        }

        // Si tocó un objeto lo selecciona, si tocó el fondo vacío des-selecciona
        sceneHierarchy.setSelectedGameObject(hitObject);
    }

    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }

    //GETTERS Y SETTERS
    public GameObject getSelectedGameObject() {
        return sceneHierarchy.getSelectedGameObject();
    }


}