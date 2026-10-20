package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiTableFlags;
import org.ravenEngine.core.Engine;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.util.FileOpener;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class ContentBrowserPanel {
    private final File rootDirectory;
    private File currentDirectory;
    private CodeEditorWindow codeEditorWindow;
    private ImGuiLayer imGuiLayer;
    // Estados para creación, renombrado y borrado
    private final imgui.type.ImString inputBuffer = new imgui.type.ImString(128);
    private String fileCreationType = ""; // "DIR", "JSON", "JAVA", "GLSL"
    private File targetFileForAction = null;
    private boolean openCreateModal = false;
    private boolean openDeleteModal = false;
    // Estado para renombrado
    private boolean openRenameModal = false;
    private final imgui.type.ImString renameBuffer = new imgui.type.ImString(128);

    public ContentBrowserPanel() {
        this(null, null);
    }

    public ContentBrowserPanel(CodeEditorWindow codeEditorWindow) {
        this(codeEditorWindow, null);
    }

    public ContentBrowserPanel(CodeEditorWindow codeEditorWindow, ImGuiLayer imGuiLayer) {
        this.codeEditorWindow = codeEditorWindow;
        this.imGuiLayer = imGuiLayer;

        // Raíz agnóstica: busca la carpeta assets del proyecto actual o la raíz de ejecución
        File assetsFolder = new File("assets");
        File srcAssetsFolder = new File("src/main/java/org/alchemistShop/assets");

        if (assetsFolder.exists() && assetsFolder.isDirectory()) {
            this.rootDirectory = assetsFolder;
        } else if (srcAssetsFolder.exists() && srcAssetsFolder.isDirectory()) {
            this.rootDirectory = srcAssetsFolder;
        } else {
            this.rootDirectory = new File(".");
        }

        this.currentDirectory = this.rootDirectory;
    }

    public void setImGuiLayer(ImGuiLayer imGuiLayer) {
        this.imGuiLayer = imGuiLayer;
    }

    public void render() {
        ImGui.begin("Explorador de Archivos");

        ImGui.textDisabled("Ruta actual: " + currentDirectory.getAbsolutePath());
        ImGui.separator();

        renderHeader();
        ImGui.separator();

        // Región scrolleable para los archivos
        ImGui.beginChild("FileListRegion", 0.0f, -ImGui.getFrameHeightWithSpacing(), false);
        renderFileTree();

        // 1. Clic derecho en espacio vacío de la carpeta actual
        if (ImGui.beginPopupContextWindow("BrowserBlankContextMenu", imgui.flag.ImGuiPopupFlags.MouseButtonRight | imgui.flag.ImGuiPopupFlags.NoOpenOverItems)) {
            if (ImGui.menuItem("Nueva Carpeta")) {
                openCreateDialog("DIR");
            }
            ImGui.separator();
            if (ImGui.menuItem("Nueva Escena (.json)")) {
                openCreateDialog("JSON");
            }
            if (ImGui.menuItem("Nuevo Script (.java)")) {
                openCreateDialog("JAVA");
            }
            if (ImGui.menuItem("Nuevo Shader (.glsl)")) {
                openCreateDialog("GLSL");
            }
            ImGui.endPopup();
        }
        ImGui.endChild();

        // 2. Modales emergentes
        renderModals();

        // Recibir un GameObject arrastrado desde la Jerarquía para convertirlo en Prefab
        if (ImGui.beginDragDropTarget()) {
            Object payload = ImGui.acceptDragDropPayload("HIERARCHY_GAMEOBJECT");
            if (payload != null && imGuiLayer != null) {
                GameObject selected = imGuiLayer.getSelectedGameObject();
                if (selected != null) {
                    saveGameObjectAsPrefab(selected, currentDirectory);
                }
            }
            ImGui.endDragDropTarget();
        }

        ImGui.end();
    }

    private void renderHeader() {
        boolean canGoBack = currentDirectory.getParentFile() != null &&
                !currentDirectory.equals(rootDirectory);

        if (!canGoBack) {
            ImGui.beginDisabled();
        }

        if (ImGui.button("<-Atrás")) {
            if (currentDirectory.getParentFile() != null) {
                currentDirectory = currentDirectory.getParentFile();
            }
        }

        if (!canGoBack) {
            ImGui.endDisabled();
        }

        ImGui.sameLine();
        ImGui.textColored(0.7f, 0.7f, 0.7f, 1.0f, "Ruta: " + currentDirectory.getPath());
    }

    private void renderFileTree() {
        File[] files = currentDirectory.listFiles();

        if (files == null || files.length == 0) {
            ImGui.textDisabled("Carpeta vacía");
            return;
        }

        // Carpetas primero, luego archivos alfabéticamente
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        File directoryToOpen = null;

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isHidden()) continue;

            String fileName = file.getName();
            boolean isDir = file.isDirectory();

            ImGui.pushID(i);

            // 1. Color y prefijo por tipo de archivo
            String label;
            if (isDir) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 1.0f, 0.85f, 0.3f, 1.0f);
                label = "[DIR] " + fileName;
            } else if (fileName.endsWith(".json")) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.4f, 0.8f, 1.0f, 1.0f);
                label = "[ESCENA] " + fileName;
            } else if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.5f, 1.0f, 0.5f, 1.0f);
                label = "[TEX] " + fileName;
            } else if (fileName.endsWith(".glsl") || fileName.endsWith(".vert") || fileName.endsWith(".frag")) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.9f, 0.4f, 0.8f, 1.0f);
                label = "[SHADER] " + fileName;
            } else if (fileName.endsWith(".java")) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.9f, 0.5f, 0.2f, 1.0f);
                label = "[CODE] " + fileName;
            } else if (fileName.endsWith(".prefab")) {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.95f, 0.75f, 0.2f, 1.0f); // Dorado
            label = "[PREFAB] " + fileName;
            }else {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.85f, 0.85f, 0.85f, 1.0f);
                label = fileName;
            }

            // 2. Elemento clickeable que abarca todo el ancho
            ImGui.selectable(label, false);
            // Menú contextual específico sobre el archivo o carpeta
            if (ImGui.beginPopupContextItem("ItemContextMenu_" + i)) {
                if (isDir) {
                    if (ImGui.menuItem("Abrir")) {
                        directoryToOpen = file;
                    }
                } else if (fileName.endsWith(".json")) {
                    if (ImGui.menuItem("Cargar Escena")) {
                        loadScene(file.getPath());
                    }
                }

                if (ImGui.menuItem("Eliminar", "Supr")) {
                    targetFileForAction = file;
                    openDeleteModal = true;
                }
                ImGui.endPopup();
            }

            // Atajo con teclado: si está hovered y apretás Delete
            if (ImGui.isItemHovered() && ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE, false)) {
                targetFileForAction = file;
                openDeleteModal = true;
            }

            ImGui.popStyleColor();

            // 3. Doble clic intuitivo según el tipo
            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
                if (isDir) {
                    directoryToOpen = file;
                } else if (fileName.endsWith(".json")) {
                    loadScene(file.getPath());
                } else if (codeEditorWindow != null && FileOpener.isEditable(fileName)) {
                    codeEditorWindow.openFile(file);
                }
            }

            // 4. Drag & Drop hacia el Viewport (Texturas y Prefabs)
            if (!isDir && (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".prefab"))) {
                if (ImGui.beginDragDropSource()) {
                    ImGui.setDragDropPayload("CONTENT_BROWSER_ITEM", file.getPath());
                    ImGui.text("Instanciar: " + fileName);
                    ImGui.endDragDropSource();
                }
            }

            // Menú contextual sobre el archivo o carpeta
            if (ImGui.beginPopupContextItem("ItemContextMenu_" + i)) {
                if (isDir) {
                    if (ImGui.menuItem("Abrir")) {
                        directoryToOpen = file;
                    }
                } else if (fileName.endsWith(".json")) {
                    if (ImGui.menuItem("Cargar Escena")) {
                        loadScene(file.getPath());
                    }
                }

                if (ImGui.menuItem("Renombrar", "F2")) {
                    targetFileForAction = file;
                    renameBuffer.set(file.getName());
                    openRenameModal = true;
                }

                if (ImGui.menuItem("Eliminar", "Supr")) {
                    targetFileForAction = file;
                    openDeleteModal = true;
                }
                ImGui.endPopup();
            }

            // Atajos rápidos con teclado sobre el ítem hovered
            if (ImGui.isItemHovered()) {
                if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_F2, false)) {
                    targetFileForAction = file;
                    renameBuffer.set(file.getName());
                    openRenameModal = true;
                } else if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE, false)) {
                    targetFileForAction = file;
                    openDeleteModal = true;
                }
            }

            ImGui.popID();
        }

        if (directoryToOpen != null) {
            currentDirectory = directoryToOpen;
        }
    }

    private void loadScene(String scenePath) {
        if (imGuiLayer != null) {
            imGuiLayer.loadSceneFromPath(scenePath, Engine.getCurrentScene());
        } else if (Engine.getCurrentScene() != null) {
            Engine.getCurrentScene().load(scenePath);
        }
    }
    //========================
    // AUXILIARES
    //========================
    private void openCreateDialog(String type) {
        this.fileCreationType = type;
        this.inputBuffer.set("");
        this.openCreateModal = true;
    }

    private void renderModals() {
        // ==========================================
        // MODAL 1: CREACIÓN DE ARCHIVOS / CARPETAS
        // ==========================================
        if (openCreateModal) {
            ImGui.openPopup("Crear Nuevo Elemento");
            openCreateModal = false;
        }

        if (ImGui.beginPopupModal("Crear Nuevo Elemento", imgui.flag.ImGuiWindowFlags.AlwaysAutoResize)) {
            String titleHint = switch (fileCreationType) {
                case "DIR" -> "Nombre de la Carpeta:";
                case "JSON" -> "Nombre de la Escena (sin extensión):";
                case "JAVA" -> "Nombre de la Clase (sin extensión):";
                case "GLSL" -> "Nombre del Shader (sin extensión):";
                default -> "Nombre:";
            };

            ImGui.text(titleHint);
            ImGui.inputText("##NewItemInput", inputBuffer);

            ImGui.separator();

            if (ImGui.button("Crear", 120, 0) || ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, false)) {
                String inputStr = inputBuffer.get().trim();
                if (!inputStr.isEmpty()) {
                    executeCreation(inputStr);
                    ImGui.closeCurrentPopup();
                }
            }
            ImGui.sameLine();
            if (ImGui.button("Cancelar", 120, 0) || ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, false)) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }

        // ==========================================
        // MODAL 2: CONFIRMACIÓN DE BORRADO
        // ==========================================
        if (openDeleteModal) {
            ImGui.openPopup("Confirmar Eliminación");
            openDeleteModal = false;
        }

        if (ImGui.beginPopupModal("Confirmar Eliminación", imgui.flag.ImGuiWindowFlags.AlwaysAutoResize)) {
            if (targetFileForAction != null) {
                ImGui.textColored(1.0f, 0.4f, 0.4f, 1.0f, "¿Estás seguro de que deseas eliminar permanentemente:");
                ImGui.text(targetFileForAction.getName() + "?");
                if (targetFileForAction.isDirectory()) {
                    ImGui.textDisabled("(Se eliminarán todos los archivos y subcarpetas contenidos)");
                }
            }

            ImGui.separator();

            if (ImGui.button("Eliminar", 120, 0)) {
                if (targetFileForAction != null) {
                    deleteRecursively(targetFileForAction);
                    System.out.println("[ContentBrowser] Eliminado: " + targetFileForAction.getName());
                    targetFileForAction = null;
                }
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button("Cancelar", 120, 0)) {
                targetFileForAction = null;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
        // ==========================================
        // MODAL 3: RENOMBRAR ARCHIVO O CARPETA
        // ==========================================
        if (openRenameModal) {
            ImGui.openPopup("Renombrar Elemento");
            openRenameModal = false;
        }

        if (ImGui.beginPopupModal("Renombrar Elemento", imgui.flag.ImGuiWindowFlags.AlwaysAutoResize)) {
            if (targetFileForAction != null) {
                ImGui.text("Nuevo nombre para: " + targetFileForAction.getName());
                ImGui.inputText("##RenameInput", renameBuffer);

                ImGui.separator();

                if (ImGui.button("Guardar", 120, 0) || ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, false)) {
                    String newName = renameBuffer.get().trim();
                    if (!newName.isEmpty() && !newName.equals(targetFileForAction.getName())) {
                        executeRename(newName);
                    }
                    ImGui.closeCurrentPopup();
                }
                ImGui.sameLine();
                if (ImGui.button("Cancelar", 120, 0) || ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE, false)) {
                    targetFileForAction = null;
                    ImGui.closeCurrentPopup();
                }
            } else {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    private void executeCreation(String baseName) {
        try {
            switch (fileCreationType) {
                case "DIR" -> {
                    File newDir = new File(currentDirectory, baseName);
                    if (!newDir.exists()) newDir.mkdirs();
                }
                case "JSON" -> {
                    String fileName = baseName.endsWith(".json") ? baseName : baseName + ".json";
                    File newScene = new File(currentDirectory, fileName);
                    if (newScene.createNewFile()) {
                        // Inicializamos una escena vacía válida
                        java.nio.file.Files.writeString(newScene.toPath(), "[]", java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
                case "JAVA" -> {
                    String fileName = baseName.endsWith(".java") ? baseName : baseName + ".java";
                    File newClass = new File(currentDirectory, fileName);
                    if (newClass.createNewFile()) {
                        String className = fileName.replace(".java", "");
                        String template = "package org.alchemistShop;\n\npublic class " + className + " {\n\n}\n";
                        java.nio.file.Files.writeString(newClass.toPath(), template, java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
                case "GLSL" -> {
                    String fileName = baseName.endsWith(".glsl") ? baseName : baseName + ".glsl";
                    File newShader = new File(currentDirectory, fileName);
                    newShader.createNewFile();
                }
            }
        } catch (Exception e) {
            System.err.println("[ContentBrowser] Error al crear elemento: " + e.getMessage());
        }
    }
    private void executeRename(String newName) {
        if (targetFileForAction == null) return;

        File destination = new File(targetFileForAction.getParentFile(), newName);

        if (destination.exists()) {
            System.err.println("[ContentBrowser] Ya existe un archivo o carpeta llamado: " + newName);
            targetFileForAction = null;
            return;
        }

        boolean success = targetFileForAction.renameTo(destination);
        if (success) {
            System.out.println("[ContentBrowser] Renombrado exitoso: " + targetFileForAction.getName() + " -> " + newName);
            // Si la escena activa renombrada era la que estaba abierta, actualizamos la ruta en el IDE
            if (imGuiLayer != null && destination.getName().endsWith(".json")) {
                imGuiLayer.loadSceneFromPath(destination.getPath(), Engine.getCurrentScene());
            }
        } else {
            System.err.println("[ContentBrowser] No se pudo renombrar el archivo en disco.");
        }

        targetFileForAction = null;
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteRecursively(f);
                }
            }
        }
        file.delete();
    }
    public void saveGameObjectAsPrefab(GameObject go, File targetDir) {
        if (go == null) return;

        String prefabName = go.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".prefab";
        File prefabFile = new File(targetDir, prefabName);

        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(prefabFile), java.nio.charset.StandardCharsets.UTF_8))) {

            String json = org.ravenEngine.util.ComponentSerializer.getGson().toJson(go);
            writer.write(json);
            writer.flush();
            System.out.println("[Prefabs] Prefab guardado con éxito: " + prefabFile.getPath());
        } catch (Exception e) {
            System.err.println("[Prefabs] Error al guardar prefab: " + e.getMessage());
        }
    }
    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public void setCodeEditorWindow(CodeEditorWindow codeEditorWindow) {
        this.codeEditorWindow = codeEditorWindow;
    }
}