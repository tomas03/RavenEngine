package org.ravenEngine.components;

import org.ravenEngine.core.DialogueManager;
import org.ravenEngine.scene.GameObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DialogueInteractable extends Interactable {

    // Lista de frases que dirá este objeto o NPC
    public List< String > dialogueLines = new ArrayList<>();
    public String speakerName = "Alquimista";

    // Ruta a la textura del retrato (ej: "textures/Frasco.png" o "textures/portraits/anciano.png")
    public String portraitPath = "";

    public DialogueInteractable() {
        this.interactionRadius = 32.0f;
        this.speakerName = "Alquimista";
        this.dialogueLines.add("¡Hola RavenTom! Bienvenido a la tienda.");
        this.dialogueLines.add("Cuidado con los reactivos rojos, son bastante inestables.");
    }

    public DialogueInteractable(String speakerName, String portraitPath, String... lines) {
        this.interactionRadius = 32.0f;
        this.speakerName = speakerName;
        this.portraitPath = portraitPath;
        this.dialogueLines = new ArrayList<>(Arrays.asList(lines));
    }

    @Override
    public void onInteract(GameObject player) {
        String name = (speakerName != null && !speakerName.isEmpty())
                ? speakerName
                : (gameObject != null ? gameObject.getName() : "Desconocido");

        DialogueManager.startDialogue(name, portraitPath, dialogueLines);
    }
}