package org.ravenEngine.editor.codeEditor;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.lwjgl.glfw.GLFW;
import org.ravenEngine.editor.compiler.ScriptHotReloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEditorWindow {

    public static class OpenFile {
        public String title;
        public String fullPath;
        public ImString content;
        public boolean isModified = false;
        public int lineCount = 1;

        public OpenFile(String title, String fullPath, String initialContent) {
            this.title = title;
            this.fullPath = fullPath;
            this.content = new ImString(initialContent, 262144);
            calculateLineCount();
        }

        public void calculateLineCount() {
            String text = content.get();
            int lines = 1;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') lines++;
            }
            this.lineCount = lines;
        }
    }

    private final List< OpenFile > openFiles = new ArrayList<>();
    private int activeFileIndex = -1;
    private boolean isVisible = true;

    // Estado del buscador
    private boolean showSearchModal = false;
    private final ImString searchBuffer = new ImString(128);
    private int lastSearchMatchIndex = -1;
    private String lastSearchQuery = "";

    // Parámetros de salto del buscador
    private boolean pendingSearchJump = false;
    private boolean focusEditorNextFrame = false;
    private int searchTargetPos = -1;
    private int searchTargetLength = 0;
    private int searchTargetLine = 1;

    // Estado de compilación
    private String compilationStatus = "Listo";
    private boolean compilationError = false;

    // Colores IntelliJ IDEA (Darcula)
    private static final int BG_EDITOR = ImColor.rgb(43, 43, 43);
    private static final int BG_GUTTER = ImColor.rgb(49, 51, 53);
    private static final int TEXT_GUTTER = ImColor.rgb(96, 99, 102);
    private static final int TEXT_CODE = ImColor.rgb(169, 183, 198);
    private static final int STATUS_BAR_BG = ImColor.rgb(33, 37, 43);
    private static final int STATUS_BAR_ERROR = ImColor.rgb(115, 30, 30);
    private static final int STATUS_BAR_SUCCESS = ImColor.rgb(35, 95, 45);

    public CodeEditorWindow() {
        openScript("PlayerController.java",
                "package org.alchemistShop.scripts;\n\n" +
                        "import org.ravenEngine.components.Component;\n" +
                        "import org.ravenEngine.core.Input;\n\n" +
                        "public class PlayerController extends Component {\n" +
                        "    private float speed = 120.0f;\n\n" +
                        "    @Override\n" +
                        "    public void update(float dt) {\n" +
                        "        if (Input.isKeyPressed(87)) {\n" +
                        "            gameObject.transform.pos.y -= speed * dt;\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n"
        );
    }

    public void openScript(String filename, String content) {
        for (int i = 0; i < openFiles.size(); i++) {
            if (openFiles.get(i).title.equals(filename)) {
                activeFileIndex = i;
                return;
            }
        }
        openFiles.add(new OpenFile(filename, "src/main/java/" + filename, content));
        activeFileIndex = openFiles.size() - 1;
    }

    public void openScriptFromFile(File file) {
        if (!file.exists()) return;
        try {
            String text = Files.readString(file.toPath());
            for (int i = 0; i < openFiles.size(); i++) {
                if (openFiles.get(i).fullPath.equals(file.getAbsolutePath())) {
                    activeFileIndex = i;
                    return;
                }
            }
            openFiles.add(new OpenFile(file.getName(), file.getAbsolutePath(), text));
            activeFileIndex = openFiles.size() - 1;
        } catch (IOException e) {
            System.err.println("[Editor] No se pudo abrir: " + file.getPath());
        }
    }

    public void update() {
        if (!isVisible) return;

        handleShortcuts();

        ImGui.pushStyleColor(ImGuiCol.WindowBg, BG_EDITOR);
        ImGui.pushStyleColor(ImGuiCol.TitleBg, ImColor.rgb(30, 31, 34));
        ImGui.pushStyleColor(ImGuiCol.TitleBgActive, ImColor.rgb(38, 40, 46));

        int windowFlags = ImGuiWindowFlags.MenuBar;
        ImGui.begin("Editor de Codigo", windowFlags);

        renderMenuBar();

        if (openFiles.isEmpty() || activeFileIndex < 0 || activeFileIndex >= openFiles.size()) {
            ImGui.textDisabled("No hay scripts abiertos.");
            ImGui.end();
            ImGui.popStyleColor(3);
            return;
        }

        OpenFile activeFile = openFiles.get(activeFileIndex);

        renderTabs();

        ImGui.pushStyleColor(ImGuiCol.Text, ImColor.rgb(120, 124, 130));
        ImGui.text(" " + activeFile.fullPath.replace("\\", " > ").replace("/", " > "));
        ImGui.popStyleColor();
        ImGui.separator();

        float contentHeight = ImGui.getContentRegionAvailY() - 26.0f;
        float availWidth = ImGui.getContentRegionAvailX();
        float gutterWidth = 48.0f;
        float editorWidth = availWidth - gutterWidth - 20.0f; // Espacio libre para la barra de scroll

        float lineHeight = ImGui.getTextLineHeightWithSpacing();
        // Altura total del texto para que el contenedor calcule el scrollbar real
        float totalTextHeight = Math.max(contentHeight, (activeFile.lineCount + 6) * lineHeight);

        // =========================================================================
        // CONTENEDOR PRINCIPAL DE SCROLL UNIFICADO
        // =========================================================================
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0.0f, 0.0f);
        ImGui.beginChild("##EditorScrollRegion", availWidth, contentHeight, false, ImGuiWindowFlags.HorizontalScrollbar);

        // Si el buscador pidió saltar a una línea, scrolleamos el contenedor directamente
        if (pendingSearchJump && searchTargetLine > 0) {
            float targetScrollY = Math.max(0.0f, (searchTargetLine - 5) * lineHeight);
            ImGui.setScrollY(targetScrollY);
        }

        // Pintamos el fondo oscuro de la canaleta de números (Gutter)
        ImVec2 startPos = ImGui.getCursorScreenPos();
        ImGui.getWindowDrawList().addRectFilled(
                startPos.x, startPos.y,
                startPos.x + gutterWidth, startPos.y + totalTextHeight,
                BG_GUTTER
        );

        // A. Columna de Números de Línea
        ImGui.beginGroup();
        ImGui.pushStyleColor(ImGuiCol.Text, TEXT_GUTTER);
        for (int i = 1; i <= activeFile.lineCount; i++) {
            ImGui.text(String.format("%3d ", i));
        }
        ImGui.popStyleColor();
        ImGui.endGroup();

        ImGui.sameLine();

        // B. Línea divisoria vertical
        ImVec2 divPos = ImGui.getCursorScreenPos();
        ImGui.getWindowDrawList().addLine(
                divPos.x, startPos.y,
                divPos.x, startPos.y + totalTextHeight,
                ImColor.rgb(60, 63, 65), 1.0f
        );

        ImGui.spacing();
        ImGui.sameLine();

        // C. Lienzo de Código Editable
        ImGui.pushStyleColor(ImGuiCol.FrameBg, BG_EDITOR);
        ImGui.pushStyleColor(ImGuiCol.Text, TEXT_CODE);
        ImGui.pushStyleColor(ImGuiCol.TextSelectedBg, ImColor.rgb(33, 66, 131));

        int inputFlags = ImGuiInputTextFlags.CallbackCompletion
                | ImGuiInputTextFlags.CallbackAlways;

        // Le devolvemos el foco al editor si el buscador encontró algo
        if (focusEditorNextFrame) {
            ImGui.setKeyboardFocusHere();
            focusEditorNextFrame = false;
        }

        if (ImGui.inputTextMultiline("##CodeBuffer", activeFile.content, editorWidth, totalTextHeight, inputFlags, editorCallback)) {
            activeFile.isModified = true;
            activeFile.calculateLineCount();
        }

        ImGui.popStyleColor(3);

        ImGui.endChild(); // Fin de ##EditorScrollRegion
        ImGui.popStyleVar();

        // Ventana flotante de búsqueda y barra de estado
        renderSearchBar(activeFile);
        renderStatusBar(activeFile);

        ImGui.end();
        ImGui.popStyleColor(3);
    }

    private void handleShortcuts() {
        boolean ctrlDown = ImGui.getIO().getKeyCtrl();

        if (ctrlDown && ImGui.isKeyPressed(GLFW.GLFW_KEY_S)) {
            saveCurrentFile();
        }

        if (ctrlDown && (ImGui.isKeyPressed(GLFW.GLFW_KEY_B) || ImGui.isKeyPressed(GLFW.GLFW_KEY_R))) {
            recompileCurrentScript();
        }

        if (ctrlDown && ImGui.isKeyPressed(GLFW.GLFW_KEY_F)) {
            showSearchModal = true;
        }

        if (showSearchModal && ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            showSearchModal = false;
        }
    }

    private void renderSearchBar(OpenFile activeFile) {
        if (!showSearchModal) return;

        ImVec2 pos = ImGui.getWindowPos();
        ImVec2 size = ImGui.getWindowSize();
        ImGui.setNextWindowPos(pos.x + size.x - 360.0f, pos.y + 60.0f);
        ImGui.setNextWindowSize(340.0f, 0.0f);

        ImGui.pushStyleColor(ImGuiCol.WindowBg, ImColor.rgb(38, 40, 46));
        ImGui.pushStyleColor(ImGuiCol.Border, ImColor.rgb(70, 75, 82));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 6.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8.0f, 8.0f);

        int flags = ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoDocking;

        if (ImGui.begin("Buscar##SearchOverlay", flags)) {
            ImGui.setNextItemWidth(180.0f);

            if (ImGui.isWindowAppearing()) {
                ImGui.setKeyboardFocusHere();
            }

            boolean searchSubmitted = ImGui.inputText("##SearchInput", searchBuffer, ImGuiInputTextFlags.EnterReturnsTrue);

            ImGui.sameLine();
            if (ImGui.button("Sig.") || searchSubmitted) {
                findNextMatch(activeFile, searchBuffer.get());
            }

            ImGui.sameLine();
            if (ImGui.button("x")) {
                showSearchModal = false;
            }

            if (lastSearchMatchIndex == -2) {
                ImGui.textColored(1.0f, 0.4f, 0.4f, 1.0f, "Sin coincidencias.");
            } else if (lastSearchMatchIndex >= 0) {
                ImGui.textColored(0.4f, 1.0f, 0.4f, 1.0f, "Línea: " + searchTargetLine + " (Pos: " + lastSearchMatchIndex + ")");
            }

            ImGui.end();
        }

        ImGui.popStyleVar(2);
        ImGui.popStyleColor(2);
    }

    private void findNextMatch(OpenFile activeFile, String query) {
        if (query == null || query.trim().isEmpty()) return;

        String content = activeFile.content.get();

        if (!query.equalsIgnoreCase(lastSearchQuery)) {
            lastSearchMatchIndex = -1;
            lastSearchQuery = query;
        }

        int startIndex = lastSearchMatchIndex + 1;
        if (startIndex >= content.length()) {
            startIndex = 0;
        }

        int match = content.toLowerCase().indexOf(query.toLowerCase(), startIndex);

        if (match == -1 && startIndex > 0) {
            match = content.toLowerCase().indexOf(query.toLowerCase(), 0);
        }

        if (match != -1) {
            lastSearchMatchIndex = match;
            searchTargetPos = match;
            searchTargetLength = query.length();
            pendingSearchJump = true;
            focusEditorNextFrame = true;

            // Calculamos exactamente la línea donde está la coincidencia
            int targetLine = 1;
            for (int i = 0; i < match && i < content.length(); i++) {
                if (content.charAt(i) == '\n') targetLine++;
            }
            searchTargetLine = targetLine;

            System.out.println("[Buscar] '" + query + "' encontrado en linea " + targetLine + " (offset: " + match + ")");
        } else {
            lastSearchMatchIndex = -2;
        }
    }

    private void renderMenuBar() {
        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("Archivo")) {
                if (ImGui.menuItem("Guardar", "Ctrl+S")) {
                    saveCurrentFile();
                }
                if (ImGui.menuItem("Cerrar Script", "Ctrl+W")) {
                    closeCurrentFile();
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Editar")) {
                if (ImGui.menuItem("Buscar...", "Ctrl+F")) {
                    showSearchModal = true;
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Compilacion")) {
                if (ImGui.menuItem("Recompilar Script en Caliente", "Ctrl+B")) {
                    recompileCurrentScript();
                }
                ImGui.endMenu();
            }
            ImGui.endMenuBar();
        }
    }

    private void renderTabs() {
        ImGui.pushStyleColor(ImGuiCol.Tab, ImColor.rgb(38, 40, 46));
        ImGui.pushStyleColor(ImGuiCol.TabActive, BG_EDITOR);
        ImGui.pushStyleColor(ImGuiCol.TabHovered, ImColor.rgb(46, 50, 56));

        for (int i = 0; i < openFiles.size(); i++) {
            OpenFile file = openFiles.get(i);
            String tabLabel = file.title + (file.isModified ? " * " : " ") + "##tab_" + i;

            if (i > 0) ImGui.sameLine();

            boolean isCurrent = (i == activeFileIndex);
            if (isCurrent) {
                ImGui.pushStyleColor(ImGuiCol.Button, BG_EDITOR);
            } else {
                ImGui.pushStyleColor(ImGuiCol.Button, ImColor.rgb(38, 40, 46));
            }

            if (ImGui.button(tabLabel)) {
                activeFileIndex = i;
            }

            ImGui.popStyleColor();

            ImGui.sameLine();
            ImGui.pushID(i);
            if (ImGui.smallButton("x")) {
                closeFileAtIndex(i);
                ImGui.popID();
                break;
            }
            ImGui.popID();
        }

        ImGui.popStyleColor(3);
    }

    private void renderStatusBar(OpenFile activeFile) {
        int barBg = STATUS_BAR_BG;
        if (compilationError) {
            barBg = STATUS_BAR_ERROR;
        } else if (!"Listo".equals(compilationStatus)) {
            barBg = STATUS_BAR_SUCCESS;
        }

        ImGui.pushStyleColor(ImGuiCol.ChildBg, barBg);
        ImGui.beginChild("##StatusBar", ImGui.getContentRegionAvailX(), 22.0f, false,
                ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoMove);

        ImGui.pushStyleColor(ImGuiCol.Text, ImColor.rgb(230, 230, 230));
        ImGui.text(String.format(" Lineas: %d  |  4 spaces  |  UTF-8  |  Estado: %s", activeFile.lineCount, compilationStatus));
        ImGui.popStyleColor();

        ImGui.endChild();
        ImGui.popStyleColor();
    }

    public void saveCurrentFile() {
        if (activeFileIndex < 0 || activeFileIndex >= openFiles.size()) return;
        OpenFile file = openFiles.get(activeFileIndex);
        try {
            Path path = Path.of(file.fullPath);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, file.content.get());
            file.isModified = false;
            System.out.println("[Editor] Guardado con exito: " + file.fullPath);
        } catch (Exception e) {
            System.err.println("[Editor ERROR] No se pudo guardar: " + e.getMessage());
        }
    }

    public void recompileCurrentScript() {
        if (activeFileIndex < 0 || activeFileIndex >= openFiles.size()) return;

        saveCurrentFile();

        OpenFile file = openFiles.get(activeFileIndex);
        File sourceFile = new File(file.fullPath);

        if (!sourceFile.exists()) {
            compilationStatus = "Error: El archivo no existe en disco.";
            compilationError = true;
            return;
        }

        String fullClassName = resolveFullClassName(file);
        boolean ok = ScriptHotReloader.reloadScript(sourceFile, fullClassName);

        if (ok) {
            compilationStatus = "Compilado & Recargado (" + file.title + ")";
            compilationError = false;
        } else {
            compilationStatus = "Fallo en compilacion. Revisa la consola.";
            compilationError = true;
        }
    }

    private String resolveFullClassName(OpenFile file) {
        String code = file.content.get();

        Pattern packagePattern = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;", Pattern.MULTILINE);
        Matcher matcher = packagePattern.matcher(code);

        String className = file.title.replace(".java", "");

        if (matcher.find()) {
            String packageName = matcher.group(1);
            return packageName + "." + className;
        }

        String path = file.fullPath.replace("\\", "/");
        int srcIndex = path.indexOf("src/main/java/");
        if (srcIndex != -1) {
            String classPath = path.substring(srcIndex + "src/main/java/".length());
            return classPath.replace(".java", "").replace("/", ".");
        }

        return className;
    }

    public void closeCurrentFile() {
        if (activeFileIndex >= 0 && activeFileIndex < openFiles.size()) {
            closeFileAtIndex(activeFileIndex);
        }
    }

    private void closeFileAtIndex(int index) {
        openFiles.remove(index);
        if (activeFileIndex >= openFiles.size()) {
            activeFileIndex = openFiles.size() - 1;
        }
    }

    private void handleEditorCallback(imgui.ImGuiInputTextCallbackData data) {
        // 1. Resaltar coincidencia encontrada por el buscador
        if (pendingSearchJump) {
            data.setSelectionStart(searchTargetPos);
            data.setSelectionEnd(searchTargetPos + searchTargetLength);
            data.setCursorPos(searchTargetPos + searchTargetLength);
            pendingSearchJump = false;
            return;
        }

        // 2. Autocompletado con TAB
        if (data.getEventFlag() == imgui.flag.ImGuiInputTextFlags.CallbackCompletion) {
            int cursorPos = data.getCursorPos();
            String text = data.getBuf();
            if (text == null) text = "";

            int wordStart = cursorPos;
            while (wordStart > 0) {
                char c = text.charAt(wordStart - 1);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
                wordStart--;
            }

            int wordLength = cursorPos - wordStart;

            if (wordLength <= 0) {
                data.insertChars(cursorPos, "    ");
                data.setCursorPos(cursorPos + 4);
                return;
            }

            String prefix = text.substring(wordStart, cursorPos);
            List< String > matches = AutocompleteDictionary.getMatches(prefix);

            if (matches.isEmpty()) {
                data.insertChars(cursorPos, "    ");
                data.setCursorPos(cursorPos + 4);
                return;
            }

            String match = matches.get(0);
            data.deleteChars(wordStart, wordLength);
            data.insertChars(wordStart, match);
            data.setCursorPos(wordStart + match.length());
        }
    }

    private final imgui.callback.ImGuiInputTextCallback editorCallback = new imgui.callback.ImGuiInputTextCallback() {
        @Override
        public void accept(imgui.ImGuiInputTextCallbackData data) {
            handleEditorCallback(data);
        }
    };

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { this.isVisible = visible; }
}