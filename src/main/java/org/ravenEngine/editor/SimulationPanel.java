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

        // ==========================================
        // BOTÓN 1: PLAY
        // ==========================================
        if (isPlaying && !isPaused) ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        if (ImGui.button(" > Play ")) {
            if (!isPlaying) {
                if (currentScene != null) {
                    // Guardamos una foto de la escena para restaurarla al tocar Stop
                    sceneSnapshot = currentScene.serializeToString();
                    if (currentScene.getCamera() != null) {
                        currentScene.getCamera().resetZoom();
                    }

                    Engine.setPlaying(true);
                    Engine.setPaused(false);

                    // 1. ARRANCAMOS EL CICLO DE VIDA DE LA ESCENA
                    // Recién acá se disparan los start() de cada componente
                    currentScene.start();

                    // 2. Disparamos los audios de la escena
                    for (GameObject go : currentScene.getGameObjects()) {
                        AudioSource audio = go.getComponent(AudioSource.class);
                        if (audio != null) audio.play();
                    }
                }
            }
        }
        if (isPlaying && !isPaused) ImGui.popStyleColor();

        ImGui.sameLine();

        // ==========================================
        // BOTÓN 2: PAUSE
        // ==========================================
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

        // ==========================================
        // BOTÓN 3: STOP
        // ==========================================
        if (!isPlaying) ImGui.pushStyleColor(ImGuiCol.Button, 0.3f, 0.3f, 0.3f, 1.0f);
        if (ImGui.button(" ■ Stop ")) {
            if (isPlaying) {
                Engine.setPlaying(false);
                Engine.setPaused(false);

                if (currentScene != null) {
                    // 1. Detenemos la simulación y reseteamos componentes
                    currentScene.stop();

                    // 2. Frenamos todos los audios en reproducción
                    for (GameObject go : currentScene.getGameObjects()) {
                        AudioSource audio = go.getComponent(AudioSource.class);
                        if (audio != null) audio.stop();
                    }

                    // 3. Restauramos la escena a su estado inicial previo al Play
                    if (sceneSnapshot != null && !sceneSnapshot.isEmpty()) {
                        currentScene.loadFromString(sceneSnapshot);
                    }
                }
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