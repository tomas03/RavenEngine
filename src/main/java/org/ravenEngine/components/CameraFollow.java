package org.ravenEngine.components;

import org.joml.Vector2f;
import org.ravenEngine.core.Engine;
import org.ravenEngine.renderer.Camera;
import org.ravenEngine.scene.Component;
import org.ravenEngine.util.MathUtils;

public class CameraFollow extends Component {
    private transient Camera camera;
    private Vector2f viewportSize = new Vector2f(640.0f, 360.0f);
    private float smoothSpeed = 5.0f;

    private float minX = 0.0f;
    private float maxX = 640.0f;
    private float minY = 0.0f;
    private float maxY = 360.0f;

    // 1. Constructor vacío OBLIGATORIO para Gson
    public CameraFollow() {
    }

    public CameraFollow(Camera camera, Vector2f viewportSize) {
        this.camera = camera;
        this.viewportSize = viewportSize;
    }

    public CameraFollow(Camera camera, Vector2f viewportSize, float smoothSpeed) {
        this.camera = camera;
        this.viewportSize = viewportSize;
        this.smoothSpeed = smoothSpeed;
    }

    private void findCamera() {
        if (this.camera == null && Engine.getCurrentScene() != null) {
            this.camera = Engine.getCurrentScene().getCamera();
        }
    }

    @Override
    public void start() {
        findCamera();
    }

    @Override
    public void update(float dt) {
        // Si no hay cámara o no está en simulación activa, no interferir con la vista del editor
        if (this.camera == null || !Engine.IsPlaying() || Engine.IsPaused()) {
            return;
        }

        Vector2f projSize = camera.getProjectionSize() != null ? camera.getProjectionSize() : this.viewportSize;

        float targetX = gameObject.transform.pos.x - (projSize.x / 2.0f) + (gameObject.transform.scale.x / 2.0f);
        float targetY = gameObject.transform.pos.y - (projSize.y / 2.0f) + (gameObject.transform.scale.y / 2.0f);

        float t = 1.0f - (float) Math.exp(-smoothSpeed * dt);

        camera.position.x = MathUtils.lerp(camera.position.x, targetX, t);
        camera.position.y = MathUtils.lerp(camera.position.y, targetY, t);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Vector2f getViewportSize() {
        return viewportSize;
    }
}