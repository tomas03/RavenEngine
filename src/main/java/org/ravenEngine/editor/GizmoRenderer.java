package org.ravenEngine.editor;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.core.Engine;
import org.ravenEngine.renderer.debug.DebugDraw;
import org.ravenEngine.scene.GameObject;

public class GizmoRenderer {

    public enum GizmoType { TRANSLATE, ROTATE, SCALE }
    public enum GizmoMode { NONE, AXIS_X, AXIS_Y, FREE, ROTATE_RING }

    // Colores base
    private static final Vector3f COLOR_BOX_SELECTED = new Vector3f(1.0f, 0.85f, 0.2f); // Amarillo dorado
    private static final Vector3f COLOR_AXIS_X       = new Vector3f(1.0f, 0.25f, 0.25f); // Rojo
    private static final Vector3f COLOR_AXIS_Y       = new Vector3f(0.25f, 1.0f, 0.25f); // Verde
    private static final Vector3f COLOR_RING_ROTATE  = new Vector3f(0.25f, 0.75f, 1.0f); // Celeste cian
    private static final Vector3f COLOR_HOVERED      = new Vector3f(1.0f, 1.0f, 0.2f);   // Amarillo activo
    private static final Vector3f COLOR_PIVOT        = new Vector3f(1.0f, 1.0f, 1.0f);   // Blanco

    public static final float AXIS_LENGTH = 40.0f;
    public static final float ARROW_HEAD_SIZE = 6.0f;
    public static final float SCALE_HANDLE_SIZE = 4.0f;
    public static final float ROTATE_RADIUS = 42.0f;
    private static final float ROTATE_THICKNESS = 6.0f;
    private static final float AXIS_THICKNESS = 6.0f;
    private static final int ROTATE_SEGMENTS = 40;

    public static void renderGizmos(GameObject selectedGo, GizmoMode activeAxis, GizmoType type) {
        if (Engine.IsPlaying() && !Engine.IsPaused()) return;
        if (selectedGo == null || selectedGo.transform == null) return;

        Vector2f scale = selectedGo.transform.scale;
        Vector2f center = getGizmoCenter(selectedGo);

        // 1. Recuadro amarillo delimitador del Transform (gira con el sprite)
        float hw = scale.x * 0.5f;
        float hh = scale.y * 0.5f;
        float rad = (float) Math.toRadians(selectedGo.transform.rotation);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        Vector2f p0 = new Vector2f(center.x + (-hw * cos - (-hh) * sin), center.y + (-hw * sin + (-hh) * cos));
        Vector2f p1 = new Vector2f(center.x + ( hw * cos - (-hh) * sin), center.y + ( hw * sin + (-hh) * cos));
        Vector2f p2 = new Vector2f(center.x + ( hw * cos -   hh  * sin), center.y + ( hw * sin +   hh  * cos));
        Vector2f p3 = new Vector2f(center.x + (-hw * cos -   hh  * sin), center.y + (-hw * sin +   hh  * cos));

        DebugDraw.addLine2D(p0, p1, COLOR_BOX_SELECTED);
        DebugDraw.addLine2D(p1, p2, COLOR_BOX_SELECTED);
        DebugDraw.addLine2D(p2, p3, COLOR_BOX_SELECTED);
        DebugDraw.addLine2D(p3, p0, COLOR_BOX_SELECTED);

        // 2. Herramientas según el GizmoType
        if (type == GizmoType.TRANSLATE) {
            Vector3f colX = (activeAxis == GizmoMode.AXIS_X) ? COLOR_HOVERED : COLOR_AXIS_X;
            Vector3f colY = (activeAxis == GizmoMode.AXIS_Y) ? COLOR_HOVERED : COLOR_AXIS_Y;
            Vector2f toX = new Vector2f(center.x + AXIS_LENGTH, center.y);
            Vector2f toY = new Vector2f(center.x, center.y + AXIS_LENGTH);

            DebugDraw.addArrow2D(center, toX, colX, ARROW_HEAD_SIZE);
            DebugDraw.addArrow2D(center, toY, colY, ARROW_HEAD_SIZE);

        } else if (type == GizmoType.SCALE) {
            Vector3f colX = (activeAxis == GizmoMode.AXIS_X) ? COLOR_HOVERED : COLOR_AXIS_X;
            Vector3f colY = (activeAxis == GizmoMode.AXIS_Y) ? COLOR_HOVERED : COLOR_AXIS_Y;
            Vector2f toX = new Vector2f(center.x + AXIS_LENGTH, center.y);
            Vector2f toY = new Vector2f(center.x, center.y + AXIS_LENGTH);

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

        } else if (type == GizmoType.ROTATE) {
            Vector3f ringCol = (activeAxis == GizmoMode.ROTATE_RING) ? COLOR_HOVERED : COLOR_RING_ROTATE;
            drawCircleGizmo(center, ROTATE_RADIUS, ringCol);

            Vector2f pointer = new Vector2f(
                    center.x + (float) Math.cos(rad) * ROTATE_RADIUS,
                    center.y + (float) Math.sin(rad) * ROTATE_RADIUS
            );
            DebugDraw.addLine2D(center, pointer, ringCol);
        }

        // 3. Pivote central
        Vector3f colCenter = (activeAxis == GizmoMode.FREE) ? COLOR_HOVERED : COLOR_PIVOT;
        DebugDraw.addBox2D(
                new Vector2f(center.x - 2.5f, center.y - 2.5f),
                new Vector2f(center.x + 2.5f, center.y + 2.5f),
                colCenter
        );
    }

    private static void drawCircleGizmo(Vector2f center, float radius, Vector3f color) {
        float step = (float) (2.0 * Math.PI / ROTATE_SEGMENTS);
        for (int i = 0; i < ROTATE_SEGMENTS; i++) {
            float a1 = i * step;
            float a2 = (i + 1) * step;

            Vector2f p1 = new Vector2f(center.x + (float) Math.cos(a1) * radius, center.y + (float) Math.sin(a1) * radius);
            Vector2f p2 = new Vector2f(center.x + (float) Math.cos(a2) * radius, center.y + (float) Math.sin(a2) * radius);

            DebugDraw.addLine2D(p1, p2, color);
        }
    }

    public static Vector2f getGizmoCenter(GameObject go) {
        return new Vector2f(
                go.transform.pos.x + (go.transform.scale.x * 0.5f),
                go.transform.pos.y + (go.transform.scale.y * 0.5f)
        );
    }

    public static GizmoMode testHit(GameObject selectedGo, Vector2f worldMouse, GizmoType type) {
        if (selectedGo == null || selectedGo.transform == null) return GizmoMode.NONE;

        Vector2f center = getGizmoCenter(selectedGo);

        if (Math.abs(worldMouse.x - center.x) <= 4.0f && Math.abs(worldMouse.y - center.y) <= 4.0f) {
            return GizmoMode.FREE;
        }

        if (type == GizmoType.ROTATE) {
            float distToCenter = center.distance(worldMouse);
            if (Math.abs(distToCenter - ROTATE_RADIUS) <= ROTATE_THICKNESS) {
                return GizmoMode.ROTATE_RING;
            }
            return GizmoMode.NONE;
        }

        if (worldMouse.x >= center.x && worldMouse.x <= center.x + AXIS_LENGTH + ARROW_HEAD_SIZE) {
            if (Math.abs(worldMouse.y - center.y) <= AXIS_THICKNESS) {
                return GizmoMode.AXIS_X;
            }
        }

        if (worldMouse.y >= center.y && worldMouse.y <= center.y + AXIS_LENGTH + ARROW_HEAD_SIZE) {
            if (Math.abs(worldMouse.x - center.x) <= AXIS_THICKNESS) {
                return GizmoMode.AXIS_Y;
            }
        }

        return GizmoMode.NONE;
    }
}