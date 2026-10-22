package org.ravenEngine.components;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.ravenEngine.renderer.camera.UICamera;
import org.ravenEngine.renderer.debug.DebugDraw;

public class UIProgressBar extends UIElement{
    private float currentValue = 100.0f;
    private float maxValue = 100.0f;

    private Vector3f fillColor = new Vector3f(0.85f, 0.15f, 0.15f);   // Rojo vida por defecto
    private Vector3f backColor = new Vector3f(0.15f, 0.15f, 0.15f);   // Gris oscuro de fondo
    private Vector3f borderColor = new Vector3f(1.0f, 1.0f, 1.0f);    // Borde blanco

    public UIProgressBar() {
        this.size.set(200.0f, 22.0f);
    }

    public UIProgressBar(Vector2f screenPos, Vector2f size, float maxVal) {
        this.screenPos.set(screenPos);
        this.size.set(size);
        this.maxValue = Math.max(0.001f, maxVal);
        this.currentValue = this.maxValue;
    }

    @Override
    public void renderUI(UICamera uiCamera) {
        if (!isVisible) return;

        float ratio = Math.max(0.0f, Math.min(1.0f, currentValue / maxValue));
        float fillWidth = size.x * ratio;

        // 1. Fondo de la barra
        Vector2f min = new Vector2f(screenPos.x, screenPos.y);
        Vector2f max = new Vector2f(screenPos.x + size.x, screenPos.y + size.y);

        for (float y = screenPos.y + 1; y < screenPos.y + size.y; y += 1.0f) {
            DebugDraw.addLine2D(new Vector2f(screenPos.x + 1, y), new Vector2f(screenPos.x + size.x - 1, y), backColor);
        }

        // 2. Relleno proporcional
        if (fillWidth > 2.0f) {
            for (float y = screenPos.y + 1; y < screenPos.y + size.y; y += 1.0f) {
                DebugDraw.addLine2D(new Vector2f(screenPos.x + 1, y), new Vector2f(screenPos.x + fillWidth - 1, y), fillColor);
            }
        }

        // 3. Contorno exterior
        DebugDraw.addBox2D(min, max, borderColor);
    }

    public void setProgress(float current, float max) {
        this.currentValue = current;
        this.maxValue = Math.max(0.001f, max);
    }

    public float getCurrentValue() { return currentValue; }
    public void setCurrentValue(float val) { this.currentValue = Math.max(0.0f, Math.min(val, maxValue)); }
    public float getMaxValue() { return maxValue; }
    public void setMaxValue(float val) { this.maxValue = Math.max(0.001f, val); }
    public Vector3f getFillColor() { return fillColor; }
    public void setFillColor(Vector3f color) { this.fillColor = color; }
}
