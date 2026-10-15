package org.ravenEngine.editor;

import imgui.ImGui;
import org.ravenEngine.components.BoxCollider2D;
import org.ravenEngine.components.Transform;
import org.ravenEngine.scene.GameObject;

public class InspectorWindow {

    public void update(GameObject selectedGameObject) {
        ImGui.begin("Inspector");

        if (selectedGameObject == null) {
            ImGui.textDisabled("Selecciona un objeto en la Jerarquía");
            ImGui.end();
            return;
        }

        // Nombre del objeto
        ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "GameObject: " + selectedGameObject.getName());
        ImGui.separator();

        // 1. COMPONENTE: TRANSFORM
        Transform transform = selectedGameObject.transform;
        if (transform != null) {
            if (ImGui.collapsingHeader("Transform")) {
                // Buffer para posición (X, Y)
                float[] pos = new float[]{ transform.pos.x, transform.pos.y };
                if (ImGui.dragFloat2("Posición", pos, 1.0f)) {
                    transform.pos.set(pos[0], pos[1]);
                }

                // Buffer para escala (ancho, alto)
                float[] scale = new float[]{ transform.scale.x, transform.scale.y };
                if (ImGui.dragFloat2("Escala", scale, 1.0f)) {
                    transform.scale.set(scale[0], scale[1]);
                }
            }
        }

        // 2. COMPONENTE: BOX COLLIDER 2D
        BoxCollider2D collider = selectedGameObject.getComponent(BoxCollider2D.class);
        if (collider != null) {
            if (ImGui.collapsingHeader("BoxCollider2D")) {
                // Buffer para el tamaño de la caja de colisión
                float[] colSize = new float[]{ collider.getSize().x, collider.getSize().y };
                if (ImGui.dragFloat2("Tamaño Collider", colSize, 1.0f)) {
                    collider.getSize().set(colSize[0], colSize[1]);
                }

                // Buffer para el offset respecto al transform
                float[] colOffset = new float[]{ collider.getOffset().x, collider.getOffset().y };
                if (ImGui.dragFloat2("Offset", colOffset, 1.0f)) {
                    collider.getOffset().set(colOffset[0], colOffset[1]);
                }
            }
        }

        ImGui.end();
    }
}