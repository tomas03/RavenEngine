package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

public class SceneHierarchy {
    private GameObject selectedGameObject = null;

    public void update(Scene currentScene) {
        ImGui.begin("Jerarquia");

        if (currentScene == null) {
            ImGui.textDisabled("No hay escena activa");
            ImGui.end();
            return;
        }

        // Recorremos los objetos registrados en la escena actual
        for (GameObject go : currentScene.getGameObjects()) {
            int nodeFlags = ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
            if (go == selectedGameObject) {
                nodeFlags |= ImGuiTreeNodeFlags.Selected;
            }

            // Dibuja cada objeto en la lista
            boolean nodeOpen = ImGui.treeNodeEx(go.getName(), nodeFlags);
            if (ImGui.isItemClicked()) {
                selectedGameObject = go;
            }
        }

        // Si haces clic en el fondo vacío del panel, deselecciona
        if (ImGui.isMouseDown(0) && ImGui.isWindowHovered() && !ImGui.isAnyItemHovered()) {
            selectedGameObject = null;
        }

        ImGui.end();
    }

    public GameObject getSelectedGameObject() {
        return selectedGameObject;
    }

    public void setSelectedGameObject(GameObject go) {
        this.selectedGameObject = go;
    }
}