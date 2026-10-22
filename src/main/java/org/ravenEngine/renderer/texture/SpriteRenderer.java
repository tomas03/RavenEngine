package org.ravenEngine.renderer.texture;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.ravenEngine.components.Component;
import org.ravenEngine.util.AssetPool;

public class SpriteRenderer extends Component {
    private Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private Sprite sprite = new Sprite(null);
    private transient boolean isDirty = true; // transient para que no dependa del JSON

    // Capa base: 0 = Suelo, 1 = Entidades/Muebles dinámicos, 2 = Techos/UI flotante
    private int zIndex = 1;

    // Si es true, el orden dentro de su capa se recalcula según la posición Y
    private boolean dynamicYSort = true;

    // Desplazamiento desde la base del sprite para calcular el punto de apoyo en el suelo
    private float ySortOffset = 0.0f;

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
        reinit();
    }
    // Método explícito para reconectar textura viva desde el disco/GPU
    public void reinit() {
        if (this.sprite != null && this.sprite.getTexture() != null) {
            String path = this.sprite.getTexture().getFilepath();
            if (path != null && !path.isEmpty()) {
                this.sprite.setTexture(AssetPool.getTexture(path));
            }
        }
        this.isDirty = true;
    }

    /**
     * Devuelve el valor de profundidad. A menor Y (más abajo en pantalla), mayor prioridad de dibujo.
     */
    public float getDepthValue() {
        if (!dynamicYSort || gameObject == null || gameObject.transform == null) {
            return (float) zIndex * 100000.0f;
        }

        // Base del objeto en el mundo + offset personalizado de pies
        float feetY = gameObject.transform.pos.y + ySortOffset;

        // Se invierte Y para que los pies que estén más abajo se dibujen al final (encima de todo)
        return ((float) zIndex * 100000.0f) - feetY;
    }

    public int getZIndex() { return zIndex; }
    public void setZIndex(int zIndex) { this.zIndex = zIndex; }
    public boolean isDynamicYSort() { return dynamicYSort; }
    public void setDynamicYSort(boolean dynamicYSort) { this.dynamicYSort = dynamicYSort; }
    public float getYSortOffset() { return ySortOffset; }
    public void setYSortOffset(float ySortOffset) { this.ySortOffset = ySortOffset; }

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
    public Sprite getSprite() {
        return this.sprite;
    }
}