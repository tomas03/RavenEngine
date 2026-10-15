package org.alchemistShop.entities;

import org.ravenEngine.components.SpriteAnimator;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.scene.Component;
import org.ravenEngine.scene.GameObject;

import static org.lwjgl.glfw.GLFW.*;

public class PlayerController extends Component {
    private final float speed = 150.0f;
    private SpriteAnimator animator;
    private String lastDirection = "down";

    public PlayerController() {
        // Ya no necesita recibir el Spritesheet por constructor
    }

    @Override
    public void start() {
        // Obtenemos el animador que configuramos en ShopScene
        this.animator = gameObject.getComponent(SpriteAnimator.class);
    }

    @Override
    public void update(float dt) {
        float dx = 0.0f;
        float dy = 0.0f;

        // EJE Y
        if (KeyListener.isKeyPressed(GLFW_KEY_W) || KeyListener.isKeyPressed(GLFW_KEY_UP)) {
            dy += 1.0f;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_S) || KeyListener.isKeyPressed(GLFW_KEY_DOWN)) {
            dy -= 1.0f;
        }

        // EJE X
        if (KeyListener.isKeyPressed(GLFW_KEY_D) || KeyListener.isKeyPressed(GLFW_KEY_RIGHT)) {
            dx += 1.0f;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_A) || KeyListener.isKeyPressed(GLFW_KEY_LEFT)) {
            dx -= 1.0f;
        }

        boolean moving = (dx != 0.0f || dy != 0.0f);

        if (moving) {
            // Aplicamos movimiento (Scene se encarga de frenarlo si choca contra un objeto sólido)
            gameObject.transform.pos.x += dx * speed * dt;
            gameObject.transform.pos.y += dy * speed * dt;

            // Selección de animación según dirección
            if (animator != null) {
                if (dy > 0) {
                    animator.play("walk_up");
                    lastDirection = "up";
                } else if (dy < 0) {
                    animator.play("walk_down");
                    lastDirection = "down";
                } else if (dx < 0) {
                    animator.play("walk_left");
                    lastDirection = "left";
                } else if (dx > 0) {
                    animator.play("walk_right");
                    lastDirection = "right";
                }
            }
        } else {
            // Reposo mirando en la última dirección que caminó
            if (animator != null) {
                animator.play("idle_" + lastDirection);
            }
        }
    }

    @Override
    public void onCollisionEnter(GameObject other) {
        System.out.println("¡CRASH! El mago chocó contra: " + other.getName());
    }

    @Override
    public void onCollisionExit(GameObject other) {
        System.out.println("El mago dejó de tocar: " + other.getName());
    }
}