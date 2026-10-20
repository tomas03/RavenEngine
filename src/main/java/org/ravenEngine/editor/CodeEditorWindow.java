package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CodeEditorWindow {

    private File currentFile = null;
    private ImString textBuffer = new ImString(1024 * 64); // Buffer de 64 KB inicial
    private boolean isModified = false;
    private boolean isOpen = true;
    private boolean editMode = false; // false = Vista con colores | true = Modo escritura

    // ==========================================
    // APERTURA Y PERSISTENCIA DE ARCHIVOS
    // ==========================================
    public void openFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) return;

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            this.currentFile = file;

            int capacity = Math.max(content.length() * 2, 1024 * 64);
            this.textBuffer = new ImString(content, capacity);
            this.isModified = false;
            this.isOpen = true;
            this.editMode = false; // Arranca por defecto en modo lectura coloreada

            System.out.println("[Editor] Archivo abierto en editor interno: " + file.getName());
        } catch (IOException e) {
            System.err.println("[Editor ERROR] No se pudo leer el archivo: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    public void saveCurrentFile() {
        if (currentFile == null) return;

        try {
            Files.writeString(currentFile.toPath(), textBuffer.get(), StandardCharsets.UTF_8);
            this.isModified = false;
            System.out.println("[Editor] Guardado exitoso: " + currentFile.getName());

            // AUTO-COMPILACIÓN SI ES CÓDIGO JAVA
            if (currentFile.getName().endsWith(".java")) {
                boolean compiled = org.ravenEngine.core.ScriptCompiler.compile(currentFile);
                if (compiled) {
                    System.out.println("[IDE] Archivo compilado a bytecode. Listo para recarga en caliente.");
                }
            }

        } catch (IOException e) {
            System.err.println("[Editor ERROR] No se pudo guardar el archivo: " + currentFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    // ==========================================
    // RENDERIZADO DE LA VENTANA
    // ==========================================
    public void update() {
        if (!isOpen) return;

        // Título dinámico. ### garantiza que el ID de la ventana sea fijo aunque cambie el texto
        String title = "Editor de Código";
        if (currentFile != null) {
            title += " - " + currentFile.getName() + (isModified ? " *" : "") + (editMode ? " [EDITANDO]" : " [LECTURA]");
        }
        title += "###RavenEngineCode";

        ImGui.begin(title, ImGuiWindowFlags.MenuBar);

        // --- BARRA SUPERIOR ---
        if (ImGui.beginMenuBar()) {
            if (currentFile != null) {
                if (ImGui.button(editMode ? "Ver Código (F2)" : "Modo Edición (F2)")) {
                    editMode = !editMode;
                }
                ImGui.sameLine();
                if (ImGui.button("Guardar (Ctrl+S)")) {
                    saveCurrentFile();
                }
                ImGui.sameLine();
                if (ImGui.button("Recargar")) {
                    openFile(currentFile);
                }
                ImGui.sameLine();
                if (ImGui.button("Cerrar")) {
                    currentFile = null;
                    textBuffer.set(""); // Limpiamos el búfer
                    isModified = false;
                }

                // Solo imprimimos la ruta si el archivo sigue abierto
                if (currentFile != null) {
                    ImGui.sameLine();
                    ImGui.textDisabled("| " + currentFile.getAbsolutePath());
                }
            } else {
                ImGui.textDisabled("Doble clic en un archivo del Content Browser para editar.");
            }
            ImGui.endMenuBar();
        }

        // --- CONTENIDO DEL ARCHIVO ---
        if (currentFile != null) {
            // Atajos de teclado
            if (ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_F2, false)) {
                editMode = !editMode;
            }

            if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_S, false)) {
                saveCurrentFile();
            }

            if (editMode) {
                // ==========================================
                // 1. MODO EDICIÓN (BÚFER INTERACTIVO)
                // ==========================================
                int flags = ImGuiInputTextFlags.AllowTabInput | ImGuiInputTextFlags.CallbackResize;
                float availX = ImGui.getContentRegionAvailX();
                float availY = ImGui.getContentRegionAvailY();

                ImGui.pushStyleColor(ImGuiCol.FrameBg, 0.11f, 0.11f, 0.12f, 1.0f);          // Fondo oscuro (#1C1C1F)
                ImGui.pushStyleColor(ImGuiCol.Text, 0.86f, 0.86f, 0.86f, 1.0f);             // Letra blanca/gris claro (#DCDCDC)
                ImGui.pushStyleColor(ImGuiCol.TextSelectedBg, 0.16f, 0.36f, 0.58f, 0.8f);   // Texto seleccionado
                ImGui.pushStyleColor(ImGuiCol.Border, 0.20f, 0.20f, 0.23f, 1.0f);           // Borde
                ImGui.pushStyleColor(ImGuiCol.ScrollbarBg, 0.11f, 0.11f, 0.12f, 1.0f);      // Fondo scrollbar
                ImGui.pushStyleColor(ImGuiCol.ScrollbarGrab, 0.28f, 0.28f, 0.32f, 1.0f);    // Barra scroll

                ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 8.0f, 8.0f);

                if (ImGui.inputTextMultiline("##source", textBuffer, availX, availY, flags)) {
                    this.isModified = true;
                }

                ImGui.popStyleVar(1);
                ImGui.popStyleColor(6);

            } else {
                // ==========================================
                // 2. MODO LECTURA (COLOREADO SINTÁCTICO)
                // ==========================================
                ImGui.beginChild("HighlightedView", 0, 0, false, ImGuiWindowFlags.HorizontalScrollbar);
                CodeHighlighter.renderHighlightedText(textBuffer.get());
                ImGui.endChild();
            }

        } else {
            ImGui.dummy(0.0f, 40.0f);
            ImGui.textColored(0.5f, 0.5f, 0.5f, 1.0f, "  No hay ningún archivo cargado.");
        }

        ImGui.end();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }
}