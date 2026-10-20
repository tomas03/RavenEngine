package org.ravenEngine.editor;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.components.BoxCollider2D;
import org.ravenEngine.core.Engine;
import org.ravenEngine.renderer.DebugDraw;
import org.ravenEngine.scene.GameObject;

public class GizmoRenderer {

    public enum GizmoType { TRANSLATE, SCALE }
    public enum GizmoMode { NONE, AXIS_X, AXIS_Y, FREE }

    // Colores base
    private static final Vector3f COLOR_BOX_SELECTED = new Vector3f(1.0f, 0.85f, 0.2f); // Amarillo dorado
    private static final Vector3f COLOR_AXIS_X       = new Vector3f(1.0f, 0.25f, 0.25f); // Rojo
    private static final Vector3f COLOR_AXIS_Y       = new Vector3f(0.25f, 1.0f, 0.25f); // Verde
    private static final Vector3f COLOR_HOVERED      = new Vector3f(1.0f, 1.0f, 0.2f);   // Amarillo brillante (activo)
    private static final Vector3f COLOR_PIVOT        = new Vector3f(1.0f, 1.0f, 1.0f);   // Blanco

    public static final float AXIS_LENGTH = 36.0f;
    public static final float ARROW_HEAD_SIZE = 6.0f;
    public static final float SCALE_HANDLE_SIZE = 3.5f;
    private static final float AXIS_THICKNESS = 6.0f; // Margen de tolerancia para hacer clic

    /**
     * Dibuja los gizmos según el tipo (Traslación o Escala) y aplica resaltado al eje activo o hovered
     */
    public static void renderGizmos(GameObject selectedGo, GizmoMode activeAxis, GizmoType type) {
        if (Engine.IsPlaying() && !Engine.IsPaused()) return;
        if (selectedGo == null || selectedGo.transform == null) return;

        Vector2f pos = selectedGo.transform.pos;
        Vector2f scale = selectedGo.transform.scale;

        // 1. Recuadro delimitador (Bounding Box)
        Vector2f min = new Vector2f(pos.x, pos.y);
        Vector2f max = new Vector2f(pos.x + scale.x, pos.y + scale.y);

        BoxCollider2D col = selectedGo.getComponent(BoxCollider2D.class);
        if (col != null) {
            min.set(pos.x + col.getOffset().x, pos.y + col.getOffset().y);
            max.set(min.x + col.getSize().x, min.y + col.getSize().y);
        }
        DebugDraw.addBox2D(min, max, COLOR_BOX_SELECTED);

        // 2. Centro / Pivote del objeto
        Vector2f center = getGizmoCenter(selectedGo);

        Vector3f colX = (activeAxis == GizmoMode.AXIS_X) ? COLOR_HOVERED : COLOR_AXIS_X;
        Vector3f colY = (activeAxis == GizmoMode.AXIS_Y) ? COLOR_HOVERED : COLOR_AXIS_Y;

        Vector2f toX = new Vector2f(center.x + AXIS_LENGTH, center.y);
        Vector2f toY = new Vector2f(center.x, center.y + AXIS_LENGTH);

        // 3. Dibujo condicional según el modo activo
        if (type == GizmoType.TRANSLATE) {
            // Flechas de traslación
            DebugDraw.addArrow2D(center, toX, colX, ARROW_HEAD_SIZE);
            DebugDraw.addArrow2D(center, toY, colY, ARROW_HEAD_SIZE);
        } else if (type == GizmoType.SCALE) {
            // Líneas con cubos/manijas cuadradas para escalado
            DebugDraw.addLine2D(center, toX, colX);
            DebugDraw.addBox2D(
                    new Vector2f(toX.x - SCALE_HANDLE_SIZE, toX.y - SCALE_HANDLE_SIZE),
                    new Vector2f(toX.x + SCALE_HANDLE_SIZE, toX.y + SCALE_HANDLE_SIZE),
                    colX
            );

            DebugDraw.addLine2D(center, toY, colY);
            DebugDraw.addBox2D(
                    new Vector2f(toY.x - SCALE_HANDLE_SIZE, toY.y - SCALE_HANDLE_SIZE),
                    new Vector2f(toY.x + SCALE_HANDLE_SIZE, toY.y + SCALE_HANDLE_SIZE),
                    colY
            );
        }

        // 4. Pivote central (para transformación libre o uniforme)
        Vector3f colCenter = (activeAxis == GizmoMode.FREE) ? COLOR_HOVERED : COLOR_PIVOT;
        DebugDraw.addBox2D(
                new Vector2f(center.x - 2.5f, center.y - 2.5f),
                new Vector2f(center.x + 2.5f, center.y + 2.5f),
                colCenter
        );
    }

    /**
     * Devuelve el centro del gizmo
     */
    public static Vector2f getGizmoCenter(GameObject go) {
        return new Vector2f(
                go.transform.pos.x + (go.transform.scale.x * 0.5f),
                go.transform.pos.y + (go.transform.scale.y * 0.5f)
        );
    }

    /**
     * Evalúa qué parte del Gizmo está bajo el cursor del mouse
     */
    public static GizmoMode testHit(GameObject selectedGo, Vector2f worldMouse) {
        if (selectedGo == null || selectedGo.transform == null) return GizmoMode.NONE;

        Vector2f center = getGizmoCenter(selectedGo);

        // 1. Chequeo del pivote central
        if (Math.abs(worldMouse.x - center.x) <= 4.0f && Math.abs(worldMouse.y - center.y) <= 4.0f) {
            return GizmoMode.FREE;
        }

        // 2. Chequeo eje X (Horizontal hacia la derecha)
        if (worldMouse.x >= center.x && worldMouse.x <= center.x + AXIS_LENGTH + ARROW_HEAD_SIZE) {
            if (Math.abs(worldMouse.y - center.y) <= AXIS_THICKNESS) {
                return GizmoMode.AXIS_X;
            }
        }

        // 3. Chequeo eje Y (Vertical hacia arriba)
        if (worldMouse.y >= center.y && worldMouse.y <= center.y + AXIS_LENGTH + ARROW_HEAD_SIZE) {
            if (Math.abs(worldMouse.x - center.x) <= AXIS_THICKNESS) {
                return GizmoMode.AXIS_Y;
            }
        }

        return GizmoMode.NONE;
    }
}