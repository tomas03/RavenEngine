package org.ravenEngine.components;

import org.joml.Vector2f;
import org.ravenEngine.renderer.camera.UICamera;
import org.ravenEngine.renderer.debug.DebugDraw;
import org.ravenEngine.renderer.texture.Sprite;

public class UIImage extends UIElement{
    private Sprite sprite;

    public UIImage() {
        this.size.set(48.0f, 48.0f);
    }

    public UIImage(Sprite sprite, Vector2f screenPos, Vector2f size) {
        this.sprite = sprite;
        this.screenPos.set(screenPos);
        this.size.set(size);
    }

    @Override
    public void renderUI(UICamera uiCamera) {
        if (!isVisible) return;

        // Si hay sprite se puede dibujar con el batch de sprites o dibujar su recuadro de guía
        Vector2f min = new Vector2f(screenPos.x, screenPos.y);
        Vector2f max = new Vector2f(screenPos.x + size.x, screenPos.y + size.y);
        DebugDraw.addBox2D(min, max, new org.joml.Vector3f(0.3f, 0.7f, 1.0f));
    }

    public Sprite getSprite() { return sprite; }
    public void setSprite(Sprite sprite) { this.sprite = sprite; }
}
