package org.ravenEngine.editor;

import imgui.ImGui;
import org.ravenEngine.components.Tilemap;

public class ToolsPanel {

    private boolean tileEditMode = false;
    private boolean showGrid = true;
    private boolean snapToGrid = false;
    private float snapGridSize = 16.0f;
    private GizmoRenderer.GizmoType currentGizmoType = GizmoRenderer.GizmoType.TRANSLATE;

    public void update(Tilemap activeTilemap) {
        ImGui.begin("Herramientas");
        ImGui.text("RavenEngine IDE");
        ImGui.separator();

        if (ImGui.button("Modo Pincel de Tiles")) {
            tileEditMode = !tileEditMode;
        }

        if (tileEditMode) {
            ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, "[Pincel Activo]");
            ImGui.text("Clic Izq: Pintar | Clic Der: Borrar");
        } else {
            ImGui.textColored(0.8f, 0.8f, 0.2f, 1.0f, "[Modo Selección]");
        }

        ImGui.separator();
        ImGui.text("Herramienta Gizmo:");
        if (ImGui.radioButton("Mover (W)", currentGizmoType == GizmoRenderer.GizmoType.TRANSLATE)) {
            currentGizmoType = GizmoRenderer.GizmoType.TRANSLATE;
        }
        ImGui.sameLine();
        if (ImGui.radioButton("Escalar (R)", currentGizmoType == GizmoRenderer.GizmoType.SCALE)) {
            currentGizmoType = GizmoRenderer.GizmoType.SCALE;
        }

        ImGui.separator();
        ImGui.text("Grilla & Snap:");
        if (ImGui.checkbox("Mostrar Grilla (G)", showGrid)) {
            showGrid = !showGrid;
        }

        if (ImGui.checkbox("Activar Snap", snapToGrid)) {
            snapToGrid = !snapToGrid;
        }
        ImGui.sameLine();
        ImGui.textDisabled("(o mantener Ctrl)");

        float[] gridStep = { snapGridSize };
        ImGui.text("Paso de celda (px):");
        if (ImGui.dragFloat("##SnapGridStep", gridStep, 1.0f, 4.0f, 128.0f, "%.0f px")) {
            snapGridSize = Math.max(4.0f, gridStep[0]);
        }

        if (ImGui.button("8px")) snapGridSize = 8.0f;
        ImGui.sameLine();
        if (ImGui.button("16px")) snapGridSize = 16.0f;
        ImGui.sameLine();
        if (ImGui.button("32px")) snapGridSize = 32.0f;

        ImGui.end();
    }

    // Getters y Setters
    public boolean isTileEditMode() { return tileEditMode; }
    public void setTileEditMode(boolean mode) { this.tileEditMode = mode; }
    public boolean isShowGrid() { return showGrid; }
    public void setShowGrid(boolean show) { this.showGrid = show; }
    public boolean isSnapToGrid() { return snapToGrid; }
    public float getSnapGridSize() { return snapGridSize; }
    public GizmoRenderer.GizmoType getCurrentGizmoType() { return currentGizmoType; }
    public void setCurrentGizmoType(GizmoRenderer.GizmoType type) { this.currentGizmoType = type; }
}