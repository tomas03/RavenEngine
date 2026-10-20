package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import org.ravenEngine.audio.AudioSource;
import org.ravenEngine.core.Engine;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

public class SimulationPanel {

    private String sceneSnapshot = null;

    public void update(float dt, Scene currentScene, SceneHierarchy hierarchy) {
        ImGui.begin("Control de Juego");

        boolean isPlaying = Engine.IsPlaying();
        boolean isPaused = Engine.IsPaused();

        if (isPlaying && !isPaused) ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        if (ImGui.button(" > Play ")) {
            if (!isPlaying) {
                if (currentScene != null) {
                    sceneSnapshot = currentScene.serializeToString();
                    if (currentScene.getCamera() != null) {
                        currentScene.getCamera().resetZoom();
                    }
                }
                Engine.setPlaying(true);
                Engine.setPaused(false);
                if (currentScene != null) {
                    for (GameObject go : currentScene.getGameObjects()) {
                        AudioSource audio = go.getComponent(AudioSource.class);
                        if (audio != null) audio.play();
                    }
                }
            }
        }
        if (isPlaying && !isPaused) ImGui.popStyleColor();

        ImGui.sameLine();

        if (isPaused) ImGui.pushStyleColor(ImGuiCol.Button, 0.8f, 0.8f, 0.2f, 1.0f);
        if (ImGui.button(" || Pause ")) {
            if (isPlaying) {
                Engine.setPaused(!isPaused);
                if (currentScene != null) {
                    for (GameObject go : currentScene.getGameObjects()) {
                        AudioSource audio = go.getComponent(AudioSource.class);
                        if (audio != null) {
                            if (Engine.IsPaused()) audio.pause();
                            else audio.play();
                        }
                    }
                }
            }
        }
        if (isPaused) ImGui.popStyleColor();

        ImGui.sameLine();

        if (!isPlaying) ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.2f, 0.2f, 1.0f);
        if (ImGui.button(" [] Stop ")) {
            if (isPlaying || isPaused) {
                Engine.setPlaying(false);
                Engine.setPaused(false);

                if (currentScene != null) {
                    for (GameObject go : currentScene.getGameObjects()) {
                        AudioSource audio = go.getComponent(AudioSource.class);
                        if (audio != null) audio.stop();
                    }
                }

                if (sceneSnapshot != null && currentScene != null) {
                    currentScene.loadFromString(sceneSnapshot);
                    if (hierarchy != null) {
                        hierarchy.setSelectedGameObject(null);
                    }
                    System.out.println("[IDE] Escena restaurada al estado de edición.");
                }

                System.out.println("[Motor] Simulación detenida.");
            }
        }
        if (!isPlaying) ImGui.popStyleColor();

        ImGui.sameLine();
        ImGui.text(String.format("Estado: %s", !isPlaying ? "Edición (Detenido)" : (isPaused ? "Pausado" : "Simulando (Play)")));
        ImGui.sameLine();
        ImGui.text(String.format("FPS: %.1f", 1.0f / dt));

        ImGui.end();
    }
}