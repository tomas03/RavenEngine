package org.ravenEngine.editor;

import imgui.ImGui;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CodeHighlighter {

    // Paleta de colores Darcula / IntelliJ
    private static final float[] COLOR_KEYWORD  = { 0.80f, 0.47f, 0.20f, 1.0f }; // Naranja (#CC7832)
    private static final float[] COLOR_TYPE     = { 0.30f, 0.78f, 0.69f, 1.0f }; // Verde agua (#4EC9B0)
    private static final float[] COLOR_STRING   = { 0.42f, 0.53f, 0.35f, 1.0f }; // Verde oliva (#6A8759)
    private static final float[] COLOR_COMMENT  = { 0.50f, 0.50f, 0.50f, 1.0f }; // Gris (#808080)
    private static final float[] COLOR_NUMBER   = { 0.41f, 0.59f, 0.73f, 1.0f }; // Azul (#6897BB)
    private static final float[] COLOR_DEFAULT  = { 0.86f, 0.86f, 0.86f, 1.0f }; // Gris claro (#DCDCDC)

    private static final Set< String > KEYWORDS = new HashSet< >(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null",
            // GLSL
            "vec2", "vec3", "vec4", "mat4", "uniform", "layout", "in", "out", "uniform"
    ));

    public static void renderHighlightedText(String text) {
        if (text == null || text.isEmpty()) return;

        String[] lines = text.split("\n", -1);

        for (int i = 0; i < lines.length; i++) {
            // Número de línea (Gutter)
            ImGui.textDisabled(String.format("%3d | ", i + 1));
            ImGui.sameLine(0.0f, 0.0f);

            String line = lines[i];
            renderLine(line);
        }
    }

    private static void renderLine(String line) {
        int length = line.length();
        int index = 0;

        while (index < length) {
            char c = line.charAt(index);

            // 1. Espacios e indentación
            if (Character.isWhitespace(c)) {
                StringBuilder spaces = new StringBuilder();
                while (index < length && Character.isWhitespace(line.charAt(index))) {
                    spaces.append(line.charAt(index));
                    index++;
                }
                ImGui.textUnformatted(spaces.toString());
                if (index < length) ImGui.sameLine(0.0f, 0.0f);
                continue;
            }

            // 2. Comentarios de línea (// ...)
            if (c == '/' && index + 1 < length && line.charAt(index + 1) == '/') {
                String comment = line.substring(index);
                ImGui.textColored(COLOR_COMMENT[0], COLOR_COMMENT[1], COLOR_COMMENT[2], COLOR_COMMENT[3], comment);
                break;
            }

            // 3. Cadenas de texto ("...")
            if (c == '"' || c == '\'') {
                char quote = c;
                int start = index++;
                while (index < length && line.charAt(index) != quote) {
                    if (line.charAt(index) == '\\' && index + 1 < length) {
                        index++; // Saltear carácter de escape
                    }
                    index++;
                }
                if (index < length) index++; // Incluir la comilla de cierre

                String str = line.substring(start, index);
                ImGui.textColored(COLOR_STRING[0], COLOR_STRING[1], COLOR_STRING[2], COLOR_STRING[3], str);
                if (index < length) ImGui.sameLine(0.0f, 0.0f);
                continue;
            }

            // 4. Números
            if (Character.isDigit(c)) {
                int start = index;
                while (index < length && (Character.isDigit(line.charAt(index)) || line.charAt(index) == '.' || line.charAt(index) == 'f')) {
                    index++;
                }
                String num = line.substring(start, index);
                ImGui.textColored(COLOR_NUMBER[0], COLOR_NUMBER[1], COLOR_NUMBER[2], COLOR_NUMBER[3], num);
                if (index < length) ImGui.sameLine(0.0f, 0.0f);
                continue;
            }

            // 5. Identificadores (Keywords y Tipos)
            if (Character.isJavaIdentifierStart(c)) {
                int start = index;
                while (index < length && Character.isJavaIdentifierPart(line.charAt(index))) {
                    index++;
                }
                String word = line.substring(start, index);

                if (KEYWORDS.contains(word)) {
                    ImGui.textColored(COLOR_KEYWORD[0], COLOR_KEYWORD[1], COLOR_KEYWORD[2], COLOR_KEYWORD[3], word);
                } else if (Character.isUpperCase(word.charAt(0))) {
                    // Convención: si arranca con Mayúscula es una Clase / Tipo
                    ImGui.textColored(COLOR_TYPE[0], COLOR_TYPE[1], COLOR_TYPE[2], COLOR_TYPE[3], word);
                } else {
                    ImGui.textColored(COLOR_DEFAULT[0], COLOR_DEFAULT[1], COLOR_DEFAULT[2], COLOR_DEFAULT[3], word);
                }

                if (index < length) ImGui.sameLine(0.0f, 0.0f);
                continue;
            }

            // 6. Signos y operadores
            ImGui.textColored(COLOR_DEFAULT[0], COLOR_DEFAULT[1], COLOR_DEFAULT[2], COLOR_DEFAULT[3], String.valueOf(c));
            index++;
            if (index < length) ImGui.sameLine(0.0f, 0.0f);
        }
    }
}