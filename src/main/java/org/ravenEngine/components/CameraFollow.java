package org.ravenEngine.components;

import org.joml.Vector2f;
import org.ravenEngine.core.Engine;
import org.ravenEngine.renderer.camera.Camera;
import org.ravenEngine.util.MathUtils;

public class CameraFollow extends Component {
    private transient Camera camera;
    private Vector2f viewportSize = new Vector2f(640.0f, 360.0f);
    private float smoothSpeed = 5.0f;

    private float minX = 0.0f;
    private float maxX = 640.0f;
    private float minY = 0.0f;
    private float maxY = 360.0f;

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
        if (Engine.getCurrentScene() != null) {
            // Asegura agarrar la cámara del mundo
            this.camera = Engine.getCurrentScene().getCamera();
        }
    }

    @Override
    public void start() {
        findCamera();
    }

    @Override
    public void update(float dt) {
        // 1. Si se perdió la referencia tras deserializar o cambiar de escena, la busca de nuevo
        if (this.camera == null) {
            findCamera();
        }

        // 2. Solo seguir si estamos en modo Play (no interferir con el editor/paneo)
        if (this.camera == null || !Engine.IsPlaying() || Engine.IsPaused()) {
            return;
        }

        if (gameObject == null || gameObject.transform == null) return;

        Vector2f projSize = camera.getProjectionSize() != null ? camera.getProjectionSize() : this.viewportSize;

        // Centro del GameObject
        float targetX = gameObject.transform.pos.x - (projSize.x / 2.0f) + (gameObject.transform.scale.x / 2.0f);
        float targetY = gameObject.transform.pos.y - (projSize.y / 2.0f) + (gameObject.transform.scale.y / 2.0f);

        // Suavizado con delta time independiente de framerate
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