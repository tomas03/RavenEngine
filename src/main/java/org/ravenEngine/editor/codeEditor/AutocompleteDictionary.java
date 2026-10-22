package org.ravenEngine.editor.codeEditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutocompleteDictionary {

    private static final List< String > SUGGESTIONS = Arrays.asList(
            // 1. Tipos y Clases de RavenEngine
            "GameObject", "Transform", "Component", "SpriteRenderer", "SpriteAnimator",
            "BoxCollider2D", "RigidBody2D", "CameraFollow", "Tilemap", "DebugDraw",
            "UIProgressBar", "UIImage", "UIElement", "UICamera", "Input", "AssetPool",
            "Vector2f", "Vector3f", "Vector4f",

            // 2. Métodos comunes del motor
            "getComponent", "addComponent", "getWorldPos", "getWorldRotation",
            "isKeyPressed", "addLine2D", "addBox2D", "setProgress",

            // 3. Palabras clave de Java
            "public", "private", "protected", "class", "extends", "implements",
            "void", "return", "if", "else", "for", "while", "new", "this",
            "float", "int", "boolean", "String", "true", "false", "null",
            "@Override"
    );

    // Devuelve todas las palabras que empiecen con el prefijo dado
    public static List< String > getMatches(String prefix) {
        List< String > matches = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) return matches;

        String lowerPrefix = prefix.toLowerCase();
        for (String word : SUGGESTIONS) {
            if (word.toLowerCase().startsWith(lowerPrefix)) {
                matches.add(word);
            }
        }
        return matches;
    }
}