package org.ravenEngine.editor;

import org.joml.Vector2f;
import org.ravenEngine.components.BoxCollider2D;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

import java.util.List;

public class ViewportPicker {

    public static GameObject pick(Scene scene, Vector2f worldPos) {
        if (scene == null || worldPos == null) return null;

        List<GameObject> objects = scene.getGameObjects();

        // Recorrido inverso para priorizar lo que está dibujado arriba (topmost)
        for (int i = objects.size() - 1; i >= 0; i--) {
            GameObject go = objects.get(i);
            if (go.transform == null) continue;

            float minX, minY, maxX, maxY;

            // Si tiene collider usamos su bounding box preciso; si no, el transform base
            BoxCollider2D collider = go.getComponent(BoxCollider2D.class);
            if (collider != null) {
                minX = go.transform.pos.x + collider.getOffset().x;
                minY = go.transform.pos.y + collider.getOffset().y;
                maxX = minX + collider.getSize().x;
                maxY = minY + collider.getSize().y;
            } else {
                minX = go.transform.pos.x;
                minY = go.transform.pos.y;
                maxX = minX + go.transform.scale.x;
                maxY = minY + go.transform.scale.y;
            }

            // Comprobación Point-in-AABB
            if (worldPos.x >= minX && worldPos.x <= maxX &&
                    worldPos.y >= minY && worldPos.y <= maxY) {
                return go;
            }
        }

        return null;
    }
}