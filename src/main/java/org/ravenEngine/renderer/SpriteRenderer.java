package org.ravenEngine.renderer;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.ravenEngine.scene.Component;

public class SpriteRenderer extends Component {
    private final Vector4f color;
    private Sprite sprite;
    private boolean isDirty = true;

    //CONSTRUCTORES
    // Solo color plano
    public SpriteRenderer(Vector4f color) {
        this.color = color;
        this.sprite = new Sprite(null);
    }

    // Textura entera
    public SpriteRenderer(Texture texture) {
        this.sprite = new Sprite(texture);
        this.color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // Sprite recortado de un Spritesheet
    public SpriteRenderer(Sprite sprite) {
        this.sprite = sprite;
        this.color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    //GETTERS Y SETTERS
    public Vector4f getColor() {
        return color;
    }

    public Texture getTexture() {
        return sprite.getTexture();
    }

    public Vector2f[] getTexCoords() {
        return sprite.getTexCoords();
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    public boolean isDirty() {
        return this.isDirty;
    }
    public void setClean(){
        this.isDirty = false;
    }
}