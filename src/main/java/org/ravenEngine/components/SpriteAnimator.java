package org.ravenEngine.components;

import org.joml.Vector2f;
import org.ravenEngine.renderer.texture.Sprite;
import org.ravenEngine.renderer.texture.SpriteRenderer;
import org.ravenEngine.renderer.texture.Spritesheet;
import org.ravenEngine.renderer.animation.Animation;

import java.util.HashMap;
import java.util.Map;

public class SpriteAnimator extends Component {

    private final Map< String, Animation > animations = new HashMap< >();
    private String defaultAnimation = "default";
    private float speed = 1.0f;
    private boolean isPlaying = true;
    private boolean flipH = false;
    private boolean flipV = false;

    private transient Animation currentAnimation = null;
    private transient SpriteRenderer spriteRenderer = null;

    @Override
    public void start() {
        this.spriteRenderer = gameObject.getComponent(SpriteRenderer.class);
        if (!defaultAnimation.isEmpty() && animations.containsKey(defaultAnimation)) {
            play(defaultAnimation);
        } else if (!animations.isEmpty()) {
            play(animations.keySet().iterator().next());
        }
    }

    public void play(String title) {
        if (currentAnimation != null && currentAnimation.title.equals(title)) {
            this.isPlaying = true;
            return;
        }

        Animation anim = animations.get(title);
        if (anim != null) {
            currentAnimation = anim;
            currentAnimation.reset();
            this.isPlaying = true;
            applyCurrentFrameToRenderer();
        }
    }

    public void pause() { this.isPlaying = false; }
    public void resume() { this.isPlaying = true; }

    public void stop() {
        this.isPlaying = false;
        if (currentAnimation != null) {
            currentAnimation.reset();
            applyCurrentFrameToRenderer();
        }
    }

    @Override
    public void update(float dt) {
        if (spriteRenderer == null) {
            spriteRenderer = gameObject.getComponent(SpriteRenderer.class);
            if (spriteRenderer == null) return;
        }

        if (isPlaying && currentAnimation != null) {
            int prevFrame = currentAnimation.getCurrentFrameIndex();
            currentAnimation.update(dt * speed);

            // Solo refrescamos el sprite si cambió de frame en este tick
            if (currentAnimation.getCurrentFrameIndex() != prevFrame) {
                applyCurrentFrameToRenderer();
            }
        }
    }

    /**
     * Aplica el sprite actual y ajusta las coordenadas UV si está volteado (flipH / flipV)
     */
    public void applyCurrentFrameToRenderer() {
        if (spriteRenderer == null || currentAnimation == null) return;

        Sprite original = currentAnimation.getCurrentSprite();
        if (original == null) return;

        if (!flipH && !flipV) {
            spriteRenderer.setSprite(original);
            return;
        }

        // Clonamos el sprite con UVs invertidas para el volteo estilo Godot
        Vector2f[] origUV = original.getTexCoords();
        Vector2f[] flippedUV = new Vector2f[]{
                new Vector2f(origUV[0]),
                new Vector2f(origUV[1]),
                new Vector2f(origUV[2]),
                new Vector2f(origUV[3])
        };

        if (flipH) {
            float tempX0 = flippedUV[0].x;
            flippedUV[0].x = flippedUV[1].x;
            flippedUV[1].x = tempX0;
            float tempX2 = flippedUV[2].x;
            flippedUV[2].x = flippedUV[3].x;
            flippedUV[3].x = tempX2;
        }

        if (flipV) {
            float tempY0 = flippedUV[0].y;
            flippedUV[0].y = flippedUV[3].y;
            flippedUV[3].y = tempY0;
            float tempY1 = flippedUV[1].y;
            flippedUV[1].y = flippedUV[2].y;
            flippedUV[2].y = tempY1;
        }

        Sprite flippedSprite = new Sprite(original.getTexture(), flippedUV);
        spriteRenderer.setSprite(flippedSprite);
    }

    /**
     * Helper para cortar un rango de celdas de un Spritesheet y crear una animación
     */
    public void addAnimationFromSheet(String title, Spritesheet sheet, int startIndex, int frameCount, float fps, boolean loop) {
        Animation anim = new Animation(title, fps, loop);
        for (int i = startIndex; i < startIndex + frameCount && i < sheet.size(); i++) {
            anim.addFrame(sheet.getSprite(i));
        }
        animations.put(title, anim);
        if (currentAnimation == null) {
            play(title);
        }
    }

    // Getters y Setters
    public boolean isFlipH() { return flipH; }
    public void setFlipH(boolean flipH) {
        if (this.flipH != flipH) {
            this.flipH = flipH;
            applyCurrentFrameToRenderer();
        }
    }

    public boolean isFlipV() { return flipV; }
    public void setFlipV(boolean flipV) {
        if (this.flipV != flipV) {
            this.flipV = flipV;
            applyCurrentFrameToRenderer();
        }
    }

    public Animation getCurrentAnimation() { return currentAnimation; }
    public Map< String, Animation > getAnimations() { return animations; }
    public String getDefaultAnimation() { return defaultAnimation; }
    public void setDefaultAnimation(String defaultAnimation) { this.defaultAnimation = defaultAnimation; }
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
    public boolean isPlaying() { return isPlaying; }
}