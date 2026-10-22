package org.ravenEngine.editor;

import imgui.ImColor;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import org.ravenEngine.editor.codeEditor.CodeEditorWindow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;

public class ProjectExplorerWindow {

    private final File rootDirectory;
    private final CodeEditorWindow codeEditor;
    private boolean isVisible = true;
    private File selectedFile = null;

    public ProjectExplorerWindow(CodeEditorWindow codeEditor) {
        this.codeEditor = codeEditor;
        this.rootDirectory = new File(".");
    }

    public ProjectExplorerWindow(CodeEditorWindow codeEditor, String rootPath) {
        this.codeEditor = codeEditor;
        this.rootDirectory = new File(rootPath);
    }

    public void update() {
        if (!isVisible) return;

        ImGui.pushStyleColor(ImGuiCol.WindowBg, ImColor.rgb(33, 37, 43));
        ImGui.pushStyleColor(ImGuiCol.Header, ImColor.rgb(44, 49, 58));
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, ImColor.rgb(53, 59, 69));
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, ImColor.rgb(62, 69, 81));

        ImGui.begin("Explorador de Proyecto", ImGuiWindowFlags.MenuBar);

        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("Ver")) {
                if (ImGui.menuItem("Abrir Raíz en Gestor de Archivos")) {
                    openInSystemFileManager(rootDirectory);
                }
                ImGui.endMenu();
            }
            ImGui.endMenuBar();
        }

        if (rootDirectory.exists() && rootDirectory.isDirectory()) {
            renderDirectoryTree(rootDirectory);
        } else {
            ImGui.textColored(ImColor.rgb(220, 80, 80), "Directorio raiz no encontrado.");
        }

        ImGui.end();
        ImGui.popStyleColor(4);
    }

    private void renderDirectoryTree(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;

        Arrays.sort(files, Comparator
                .comparing((File f) -> !f.isDirectory())
                .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        for (File file : files) {
            if (file.getName().startsWith(".") ||
                    file.getName().equals("target") ||
                    file.getName().equals("bin") ||
                    file.getName().equals("out")) {
                continue;
            }

            if (file.isDirectory()) {
                int flags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.SpanAvailWidth;
                boolean isOpen = ImGui.treeNodeEx("##dir_" + file.getAbsolutePath(), flags, "\uF07B " + file.getName());

                // =========================================================================
                // MENÚ CONTEXTUAL (CLIC DERECHO EN CARPETAS)
                // =========================================================================
                if (ImGui.beginPopupContextItem("##dir_ctx_" + file.getAbsolutePath())) {
                    if (ImGui.menuItem("Nuevo Script (.java)")) {
                        createNewScript(file);
                    }
                    if (ImGui.menuItem("Nueva Carpeta")) {
                        createNewFolder(file);
                    }
                    ImGui.separator();
                    if (ImGui.menuItem("Abrir en Gestor del Sistema")) {
                        openInSystemFileManager(file);
                    }
                    if (ImGui.menuItem("Eliminar Carpeta")) {
                        deleteRecursive(file);
                    }
                    ImGui.endPopup();
                }

                if (isOpen) {
                    renderDirectoryTree(file);
                    ImGui.treePop();
                }
            } else {
                renderFileItem(file);
            }
        }
    }

    private void renderFileItem(File file) {
        int flags = ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen | ImGuiTreeNodeFlags.SpanAvailWidth;

        if (file.equals(selectedFile)) {
            flags |= ImGuiTreeNodeFlags.Selected;
        }

        String icon = getFileIcon(file.getName());
        ImGui.treeNodeEx("##file_" + file.getAbsolutePath(), flags, icon + " " + file.getName());

        // =========================================================================
        // MENÚ CONTEXTUAL (CLIC DERECHO EN ARCHIVOS)
        // =========================================================================
        if (ImGui.beginPopupContextItem("##file_ctx_" + file.getAbsolutePath())) {
            if (ImGui.menuItem("Abrir en Editor")) {
                handleFileOpen(file);
            }
            ImGui.separator();
            if (ImGui.menuItem("Eliminar Archivo")) {
                deleteFile(file);
            }
            ImGui.endPopup();
        }

        // =========================================================================
        // EMISOR DE DRAG & DROP HACIA EL VIEWPORT
        // =========================================================================
        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload("CONTENT_BROWSER_ITEM", file.getAbsolutePath());
            ImGui.text(icon + " " + file.getName());
            ImGui.endDragDropSource();
        }

        // Selección con un clic
        if (ImGui.isItemClicked(0)) {
            selectedFile = file;
        }

        // Abrir en el CodeEditorWindow con doble clic
        if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
            handleFileOpen(file);
        }
    }

    private void handleFileOpen(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".java") || name.endsWith(".json") || name.endsWith(".vert") ||
                name.endsWith(".frag") || name.endsWith(".glsl") || name.endsWith(".txt")) {
            System.out.println("[Explorer] Abriendo en editor: " + file.getAbsolutePath());
            codeEditor.openScriptFromFile(file);
        }
    }

    private void createNewScript(File targetDir) {
        String baseName = "NewComponent";
        File newFile = new File(targetDir, baseName + ".java");
        int counter = 1;
        while (newFile.exists()) {
            newFile = new File(targetDir, baseName + counter + ".java");
            counter++;
        }

        String className = newFile.getName().replace(".java", "");

        // Deducir el package Java automáticamente según la estructura de carpetas
        String normPath = targetDir.getAbsolutePath().replace('\\', '/');
        int srcIdx = normPath.indexOf("src/main/java/");
        String packageName = "org.alchemistShop.scripts";
        if (srcIdx != -1) {
            String sub = normPath.substring(srcIdx + "src/main/java/".length());
            if (!sub.isEmpty()) {
                packageName = sub.replace('/', '.');
            }
        }

        String template = "package " + packageName + ";\n\n" +
                "import org.ravenEngine.components.Component;\n\n" +
                "public class " + className + " extends Component {\n\n" +
                "    @Override\n" +
                "    public void start() {\n" +
                "        // Inicializacion del componente\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void update(float dt) {\n" +
                "        // Bucle por frame\n" +
                "    }\n" +
                "}\n";

        try {
            Files.writeString(newFile.toPath(), template);
            System.out.println("[Explorer] Script creado con éxito: " + newFile.getAbsolutePath());
            if (codeEditor != null) {
                codeEditor.openScriptFromFile(newFile);
            }
        } catch (IOException e) {
            System.err.println("[Explorer ERROR] No se pudo crear el archivo: " + e.getMessage());
        }
    }

    private void createNewFolder(File targetDir) {
        String baseName = "NuevaCarpeta";
        File newFolder = new File(targetDir, baseName);
        int counter = 1;
        while (newFolder.exists()) {
            newFolder = new File(targetDir, baseName + counter);
            counter++;
        }
        newFolder.mkdirs();
    }

    private void deleteFile(File file) {
        try {
            Files.deleteIfExists(file.toPath());
            if (file.equals(selectedFile)) {
                selectedFile = null;
            }
            System.out.println("[Explorer] Archivo eliminado: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Explorer ERROR] No se pudo eliminar: " + e.getMessage());
        }
    }

    private void deleteRecursive(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) deleteRecursive(child);
                else child.delete();
            }
        }
        dir.delete();
    }

    private void openInSystemFileManager(File path) {
        try {
            new ProcessBuilder("xdg-open", path.getAbsolutePath()).start();
        } catch (Exception e) {
            System.err.println("[Explorer ERROR] No se pudo invocar xdg-open: " + e.getMessage());
        }
    }

    private String getFileIcon(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".java")) return "\uE738";
        if (lower.endsWith(".json")) return "\uE60B";
        if (lower.endsWith(".png") || lower.endsWith(".jpg")) return "\uF1C5";
        if (lower.endsWith(".vert") || lower.endsWith(".frag") || lower.endsWith(".glsl")) return "\uE614";
        if (lower.endsWith(".ogg") || lower.endsWith(".wav")) return "\uF001";
        return "\uF15C";
    }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { this.isVisible = visible; }
}