package org.ravenEngine.components;

import org.joml.Vector4f;
import org.ravenEngine.renderer.Texture;

public class SpriteRenderer extends Component {
    private final Vector4f color;
    private Texture texture;

    // 1. Constructor para solo color plano
    public SpriteRenderer(Vector4f color) {
        this.color = color;
        this.texture = null;
    }

    // 2. Constructor para solo textura (color blanco por defecto para no teñirla)
    public SpriteRenderer(Texture texture) {
        this.texture = texture;
        this.color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    // 3. Constructor completo (textura + tinte de color)
    public SpriteRenderer(Vector4f color, Texture texture) {
        this.color = color;
        this.texture = texture;
    }

    public Vector4f getColor() {
        return color;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }
}