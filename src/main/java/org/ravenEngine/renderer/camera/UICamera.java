package org.ravenEngine.renderer.camera;

import org.joml.Matrix4f;
import org.joml.Vector2f;

public class UICamera extends Camera {
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private float uiWidth;
    private float uiHeight;

    public UICamera(float width, float height) {
        super(new Vector2f(0.0f, 0.0f));
        adjustProjection(width, height);
    }

    public void adjustProjection(float width, float height) {
        this.uiWidth = width;
        this.uiHeight = height;

        this.projectionMatrix.identity();
        // (0,0) arriba a la izquierda hasta (width, height)
        this.projectionMatrix.ortho(0.0f, width, height, 0.0f, 0.0f, 100.0f);

        // Vista siempre clavada en el origen
        this.viewMatrix.identity();
    }
    @Override
    public void update(float dt) {
        // Bloqueado: la cámara de UI no se desplaza ni sigue a nadie
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public float getUIWidth() { return uiWidth; }
    public float getUIHeight() { return uiHeight; }
}
