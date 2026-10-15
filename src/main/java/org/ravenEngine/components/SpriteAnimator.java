package org.ravenEngine.components;

import java.util.HashMap;
import java.util.Map;

import org.ravenEngine.renderer.Sprite;
import org.ravenEngine.renderer.animation.Animation;
import org.ravenEngine.scene.Component;

public class SpriteAnimator extends Component{

    private final Map<String,Animation>animations = new HashMap<>();
    private Animation currentAnimation = null;
    private SpriteRenderer spriteRenderer = null;

    @Override
    public void start() {
        this.spriteRenderer = gameObject.getComponent(SpriteRenderer.class);
    }


    public void play(String animationTitle){
        if (currentAnimation != null && currentAnimation.title.equals(animationTitle)){
            return; //la animación ya se está reproduciendo
        }

        Animation anim = animations.get(animationTitle);
        if (anim != null){
            currentAnimation = anim;
            currentAnimation.reset();
        }else{
            System.err.println("[SpriteAnimator] Animación no encontrada: "+animationTitle);
        }
    }

    @Override
    public void update(float dt) {
        if (spriteRenderer == null){
            spriteRenderer = gameObject.getComponent(SpriteRenderer.class);
            if (spriteRenderer == null) return;
        }

        if (currentAnimation != null){
            currentAnimation.update(dt);
            Sprite currentSprite = currentAnimation.getCurrentSprite();
            if (currentSprite != null){
                spriteRenderer.setSprite(currentSprite);
            }
        }
    }

    public void addAnimation(Animation animation){
        animations.put(animation.getTitle(),animation);
        if (currentAnimation == null){
            play(animation.title);
        }
    }

    public String getCurrentAnimationTitle(){
        return currentAnimation != null ? currentAnimation.title : "";
    }
}
