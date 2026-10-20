package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import java.util.List;

public class SceneHierarchy {

    private GameObject selectedGameObject = null;

    public void update(Scene currentScene) {
        ImGui.begin("Jerarquia");

        if (currentScene == null) {
            ImGui.textDisabled("No hay escena activa");
            ImGui.end();
            return;
        }

        List<GameObject> gameObjects = currentScene.getGameObjects();
        GameObject objectToDelete = null;

        // Recorremos los objetos de la escena
        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);

            int nodeFlags = ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen;
            if (go == selectedGameObject) {
                nodeFlags |= ImGuiTreeNodeFlags.Selected;
            }

            // Evitamos choque de nombres repetidos usando el índice en ImGui
            ImGui.pushID(i);
            boolean nodeOpen = ImGui.treeNodeEx(go.getName(), nodeFlags);

            if (ImGui.isItemClicked()) {
                selectedGameObject = go;
            }

            // ==========================================
            // DRAG & DROP SOURCE (Arrastrar hacia el Content Browser)
            // ==========================================
            if (ImGui.beginDragDropSource()) {
                selectedGameObject = go; // Lo dejamos seleccionado al iniciar arrastre
                ImGui.setDragDropPayload("HIERARCHY_GAMEOBJECT", go.getName());
                ImGui.text("Crear Prefab: " + go.getName());
                ImGui.endDragDropSource();
            }

            // Menú contextual con clic derecho sobre el objeto
            if (ImGui.beginPopupContextItem("ItemContextMenu")) {
                if (ImGui.menuItem("Eliminar")) {
                    objectToDelete = go;
                }
                ImGui.endPopup();
            }

            ImGui.popID();
        }

        // Destrucción inmediata y limpia en GPU, físicas y escena
        if (objectToDelete != null) {
            currentScene.destroyGameObject(objectToDelete);
            if (selectedGameObject == objectToDelete) {
                selectedGameObject = null;
            }
        }

        // Clic derecho en el área vacía del panel para crear entidades
        if (ImGui.beginPopupContextWindow("HierarchyEmptyContextMenu", 1)) {
            if (ImGui.menuItem("Crear GameObject Vacio")) {
                GameObject nuevo = new GameObject("Nuevo Objeto");
                currentScene.addGameObjectToScene(nuevo);
                selectedGameObject = nuevo;
            }
            ImGui.endPopup();
        }

        // Deselección al cliquear en el fondo sin tocar ningún ítem
        if (ImGui.isMouseDown(0) && ImGui.isWindowHovered() && !ImGui.isAnyItemHovered()) {
            selectedGameObject = null;
        }

        ImGui.end();
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public GameObject getSelectedGameObject() {
        return selectedGameObject;
    }

    public void setSelectedGameObject(GameObject go) {
        this.selectedGameObject = go;
    }
}