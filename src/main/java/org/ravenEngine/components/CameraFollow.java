package org.ravenEngine.components;

import org.joml.Vector2f;
import org.ravenEngine.renderer.Camera;
import org.ravenEngine.scene.Component;
import org.ravenEngine.util.MathUtils;

public class CameraFollow extends Component {
    private final Camera camera;
    private final Vector2f viewportSize;
    private float smoothSpeed = 5.0f;// que tan rapido acompaña mientras mas alto mas reactivo

    private float minX = 0.0f;
    private float maxX = 640.0f;
    private float minY = 0.0f;
    private float maxY = 360.0f;

    public CameraFollow(Camera camera, Vector2f viewportSize) {
        this.camera = camera;
        this.viewportSize = viewportSize;
    }
    public CameraFollow(Camera camera,Vector2f viewportSize,float smoothSpeed){
        this.camera = camera;
        this.viewportSize = viewportSize;
        this.smoothSpeed = smoothSpeed;
    }
    @Override
    public void update(float dt) {
        // 320 y 180 son la mitad exacta de 640x360
        float targetX = gameObject.transform.pos.x - (viewportSize.x / 2.0f) + (gameObject.transform.scale.x / 2.0f);
        float targetY = gameObject.transform.pos.y - (viewportSize.y / 2.0f) + (gameObject.transform.scale.y / 2.0f);

        float t = 1.0f - (float) Math.exp(-smoothSpeed * dt);

        camera.position.x = MathUtils.lerp(camera.position.x, targetX, t);
        camera.position.y = MathUtils.lerp(camera.position.y, targetY, t);
    }
}
