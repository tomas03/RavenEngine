package org.ravenEngine.core;

import java.util.ArrayList;
import java.util.List;

public class DialogueManager {

    private static boolean active = false;
    private static String speakerName = "";
    private static String portraitPath = "";
    private static final List< String > lines = new ArrayList<>();
    private static int currentLineIndex = 0;

    // Efecto máquina de escribir (Typewriter)
    private static String fullCurrentText = "";
    private static String displayedText = "";
    private static float charTimer = 0.0f;
    private static final float CHAR_SPEED = 0.03f; // Segundos por letra
    private static int charIndex = 0;

    public static void startDialogue(String speaker, String portrait, List< String > dialogueLines) {
        if (dialogueLines == null || dialogueLines.isEmpty()) return;

        lines.clear();
        lines.addAll(dialogueLines);
        speakerName = (speaker != null) ? speaker : "";
        portraitPath = (portrait != null) ? portrait : "";
        currentLineIndex = 0;
        active = true;

        loadLine(currentLineIndex);
    }

    private static void loadLine(int index) {
        fullCurrentText = lines.get(index);
        displayedText = "";
        charIndex = 0;
        charTimer = 0.0f;
    }

    public static void update(float dt) {
        if (!active) return;

        if (charIndex < fullCurrentText.length()) {
            charTimer += dt;
            while (charTimer >= CHAR_SPEED && charIndex < fullCurrentText.length()) {
                charTimer -= CHAR_SPEED;
                charIndex++;
                displayedText = fullCurrentText.substring(0, charIndex);
            }
        }
    }

    public static void advance() {
        if (!active) return;

        if (charIndex < fullCurrentText.length()) {
            charIndex = fullCurrentText.length();
            displayedText = fullCurrentText;
        } else {
            currentLineIndex++;
            if (currentLineIndex < lines.size()) {
                loadLine(currentLineIndex);
            } else {
                close();
            }
        }
    }

    public static void close() {
        active = false;
        lines.clear();
        displayedText = "";
        fullCurrentText = "";
        speakerName = "";
        portraitPath = "";
        currentLineIndex = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static String getDisplayedText() {
        return displayedText;
    }

    public static String getSpeakerName() {
        return speakerName;
    }

    public static String getPortraitPath() {
        return portraitPath;
    }

    public static boolean isLineCompleted() {
        return charIndex >= fullCurrentText.length();
    }
}