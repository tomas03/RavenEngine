package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import java.util.List;

public class SceneHierarchy {

    private GameObject selectedGameObject = null;
    private GameObject objectToDelete = null;

    public void update(Scene currentScene) {
        ImGui.begin("Jerarquia");

        if (currentScene == null) {
            ImGui.textDisabled("No hay escena activa");
            ImGui.end();
            return;
        }

        List<GameObject> gameObjects = currentScene.getGameObjects();
        objectToDelete = null;

        // Renderizamos únicamente los objetos raíz (los hijos se dibujan recursivamente)
        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            if (go.getParent() == null) {
                renderNode(go, currentScene);
            }
        }

        // Destrucción limpia diferida
        if (objectToDelete != null) {
            // Si tiene padre, lo desvinculamos primero
            if (objectToDelete.getParent() != null) {
                objectToDelete.getParent().removeChild(objectToDelete);
            }
            currentScene.destroyGameObject(objectToDelete);
            if (selectedGameObject == objectToDelete) {
                selectedGameObject = null;
            }
        }

        // Clic derecho en espacio vacío de la ventana para crear nuevo objeto raíz
        if (ImGui.beginPopupContextWindow("HierarchyEmptyContextMenu", 1)) {
            if (ImGui.menuItem("Crear GameObject Vacio")) {
                GameObject nuevo = new GameObject("Nuevo Objeto");
                currentScene.addGameObjectToScene(nuevo);
                selectedGameObject = nuevo;
            }
            ImGui.endPopup();
        }

        // Deselección al cliquear en el fondo vacío
        if (ImGui.isMouseDown(0) && ImGui.isWindowHovered() && !ImGui.isAnyItemHovered()) {
            selectedGameObject = null;
        }

        ImGui.end();
    }

    private void renderNode(GameObject go, Scene currentScene) {
        int nodeFlags = ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.SpanAvailWidth;

        if (go == selectedGameObject) {
            nodeFlags |= ImGuiTreeNodeFlags.Selected;
        }
        if (!go.hasChildren()) {
            nodeFlags |= ImGuiTreeNodeFlags.Leaf;
        }

        ImGui.pushID(go.hashCode());
        boolean nodeOpen = ImGui.treeNodeEx(go.getName(), nodeFlags);

        // Selección con clic izquierdo
        if (ImGui.isItemClicked() && !ImGui.isItemToggledOpen()) {
            selectedGameObject = go;
        }

        // ==========================================
        // 1. DRAG & DROP SOURCE (Arrastrar este objeto)
        // ==========================================
        if (ImGui.beginDragDropSource()) {
            selectedGameObject = go;
            ImGui.setDragDropPayload("HIERARCHY_GAMEOBJECT", go.getName());
            ImGui.text("Arrastrando: " + go.getName());
            ImGui.endDragDropSource();
        }

        // ==========================================
        // 2. DRAG & DROP TARGET (Soltar otro objeto sobre este para emparentarlo)
        // ==========================================
        if (ImGui.beginDragDropTarget()) {
            Object payload = ImGui.acceptDragDropPayload("HIERARCHY_GAMEOBJECT");
            if (payload != null && selectedGameObject != null && selectedGameObject != go) {
                // Evitamos ciclos: no emparentar un padre dentro de sus propios descendientes
                if (!isDescendant(go, selectedGameObject)) {
                    go.addChild(selectedGameObject);
                    System.out.println("[Jerarquia] " + selectedGameObject.getName() + " ahora es hijo de " + go.getName());
                }
            }
            ImGui.endDragDropTarget();
        }

        // ==========================================
        // 3. MENÚ CONTEXTUAL (Clic derecho)
        // ==========================================
        if (ImGui.beginPopupContextItem("ItemContextMenu")) {
            if (go.getParent() != null) {
                if (ImGui.menuItem("Desvincular del padre (Hacer raíz)")) {
                    go.getParent().removeChild(go);
                }
                ImGui.separator();
            }

            if (ImGui.menuItem("Crear Hijo Vacio")) {
                GameObject child = new GameObject(go.getName() + "_Child");
                currentScene.addGameObjectToScene(child);
                go.addChild(child);
                selectedGameObject = child;
            }

            ImGui.separator();

            if (ImGui.menuItem("Eliminar")) {
                objectToDelete = go;
            }
            ImGui.endPopup();
        }

        // Renderizado recursivo de hijos si el nodo está desplegado
        if (nodeOpen) {
            for (int i = 0; i < go.getChildren().size(); i++) {
                renderNode(go.getChildren().get(i), currentScene);
            }
            ImGui.treePop();
        }

        ImGui.popID();
    }

    // Previene bucles infinitos (que un objeto sea padre de su propio abuelo)
    private boolean isDescendant(GameObject potentialDescendant, GameObject ancestor) {
        GameObject current = potentialDescendant.getParent();
        while (current != null) {
            if (current == ancestor) return true;
            current = current.getParent();
        }
        return false;
    }

    public GameObject getSelectedGameObject() {
        return selectedGameObject;
    }

    public void setSelectedGameObject(GameObject go) {
        this.selectedGameObject = go;
    }
}