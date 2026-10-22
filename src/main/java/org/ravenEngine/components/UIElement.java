package org.ravenEngine.components;

import org.joml.Vector2f;
import org.ravenEngine.renderer.camera.UICamera;

public abstract class UIElement extends Component{
    // Posición en coordenadas de pantalla (píxeles)
    protected Vector2f screenPos = new Vector2f(20.0f, 20.0f);
    protected Vector2f size = new Vector2f(100.0f, 30.0f);
    protected boolean isVisible = true;

    public abstract void renderUI(UICamera uiCamera);

    // Getters y Setters
    public Vector2f getScreenPos() { return screenPos; }
    public void setScreenPos(Vector2f pos) { this.screenPos.set(pos); }
    public void setScreenPos(float x, float y) { this.screenPos.set(x, y); }

    public Vector2f getSize() { return size; }
    public void setSize(Vector2f size) { this.size.set(size); }
    public void setSize(float w, float h) { this.size.set(w, h); }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { this.isVisible = visible; }
}
