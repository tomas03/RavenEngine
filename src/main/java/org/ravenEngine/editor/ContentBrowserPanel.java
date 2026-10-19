package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiTableFlags;
import org.ravenEngine.core.Engine;
import org.ravenEngine.util.FileOpener;
import org.ravenEngine.editor.CodeEditorWindow;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class ContentBrowserPanel {
    private final File rootDirectory;
    private File currentDirectory;
    private CodeEditorWindow codeEditorWindow;

    public ContentBrowserPanel() {
        // Apuntamos directo a tu paquete de juego dentro de src
        File gameFolder = new File("src/main/java/org/alchemistShop");

        if (gameFolder.exists()) {
            this.rootDirectory = gameFolder;
        } else {
            // Fallback por si lo ejecutás desde otro lado
            this.rootDirectory = new File(System.getProperty("user.dir") + "/src/main/java/org/alchemistShop");
        }

        this.currentDirectory = this.rootDirectory;
    }

    public ContentBrowserPanel(CodeEditorWindow codeEditorWindow) {

        this.codeEditorWindow = codeEditorWindow;
        // Carpeta base del proyecto
        this.rootDirectory = new File("alchemistShop");

        if (this.rootDirectory.exists() && this.rootDirectory.isDirectory()) {
            this.currentDirectory = this.rootDirectory;
        } else {
            this.currentDirectory = new File(".");
        }
    }

    public void render() {
        ImGui.begin("Explorador de Archivos");

        //DEBUG
        ImGui.textDisabled("Ruta actual: " + currentDirectory.getAbsolutePath());
        ImGui.separator();

        renderHeader();
        ImGui.separator();
        renderFileTree();

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
        Arrays.sort(files, new Comparator< File >() {
            @Override
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        int flags = ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.RowBg;
        if (ImGui.beginTable("FilesTable", 2, flags)) {
            ImGui.tableSetupColumn("Nombre");
            ImGui.tableSetupColumn("Acción");
            ImGui.tableHeadersRow();

            for (File file : files) {
                if (file.isHidden()) continue;

                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);

                String fileName = file.getName();
                boolean isDir = file.isDirectory();

                // Identificación visual por color y etiqueta
                if (isDir) {
                    ImGui.textColored(1.0f, 0.85f, 0.3f, 1.0f, "[DIR] " + fileName);
                } else if (fileName.endsWith(".json")) {
                    ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "[ESCENA] " + fileName);
                } else if (fileName.endsWith(".png")) {
                    ImGui.textColored(0.5f, 1.0f, 0.5f, 1.0f, "[TEX] " + fileName);
                } else if (fileName.endsWith(".glsl") || fileName.endsWith(".vert") || fileName.endsWith(".frag")) {
                    ImGui.textColored(0.9f, 0.4f, 0.8f, 1.0f, "[SHADER] " + fileName);
                } else if (fileName.endsWith(".java")) {
                    ImGui.textColored(0.9f, 0.5f, 0.2f, 1.0f, "[CODE] " + fileName);
                } else {
                    ImGui.text(fileName);
                }

                ImGui.tableSetColumnIndex(1);

                // Acciones según el tipo de archivo
                if (isDir) {
                    if (ImGui.button("Abrir " + fileName)) {
                        currentDirectory = file;
                        ImGui.endTable();
                        return;
                    }
                } else if (fileName.endsWith(".json")) {
                    if (ImGui.button("Cargar " + fileName)) {
                        Engine.loadScene(file.getPath());
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Editar " + fileName)) {
                        codeEditorWindow.openFile(file);
                    }
                } else if (FileOpener.isEditable(fileName)) {
                    if (ImGui.button("Editar " + fileName)) {
                        codeEditorWindow.openFile(file);
                    }
                } else {
                    ImGui.textDisabled("-");
                }
            }

            ImGui.endTable();
        }
    }

    public void setCodeEditorWindow(CodeEditorWindow codeEditorWindow) {this.codeEditorWindow = codeEditorWindow;}
}