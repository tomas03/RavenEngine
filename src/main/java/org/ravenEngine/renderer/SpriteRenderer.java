package org.ravenEngine.renderer;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.ravenEngine.components.Component;
import org.ravenEngine.util.AssetPool;

public class SpriteRenderer extends Component {
    private Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private Sprite sprite = new Sprite(null);
    private transient boolean isDirty = true; // transient para que no dependa del JSON

    // 1. Constructor vacío OBLIGATORIO para Gson
    public SpriteRenderer() {
    }

    // Solo color plano
    public SpriteRenderer(Vector4f color) {
        this.color = color;
        this.sprite = new Sprite(null);
        this.isDirty = true;
    }

    // Textura entera
    public SpriteRenderer(Texture texture) {
        this.sprite = new Sprite(texture);
        this.color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.isDirty = true;
    }

    // Sprite recortado de un Spritesheet
    public SpriteRenderer(Sprite sprite) {
        this.sprite = sprite;
        this.color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.isDirty = true;
    }

    @Override
    public void start() {
        // Al recargar desde JSON, reconectamos la textura con la GPU mediante AssetPool
        if (this.sprite != null && this.sprite.getTexture() != null) {
            String path = this.sprite.getTexture().getFilepath();
            if (path != null && !path.isEmpty()) {
                this.sprite.setTexture(AssetPool.getTexture(path));
            }
        }
        this.isDirty = true;
    }

    public Vector4f getColor() {
        return color;
    }

    public void setColor(Vector4f color) {
        if (!this.color.equals(color)) {
            this.color.set(color);
            this.isDirty = true;
        }
    }

    public Texture getTexture() {
        return sprite != null ? sprite.getTexture() : null;
    }

    public Vector2f[] getTexCoords() {
        return sprite != null ? sprite.getTexCoords() : new Vector2f[0];
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
        this.isDirty = true;
    }

    public boolean isDirty() {
        return this.isDirty;
    }
    public void setDirty() {this.isDirty = true;}
    public void setClean() {
        this.isDirty = false;
    }
}