package org.alchemistShop.assets.entities;

import org.joml.Vector2f;
import org.ravenEngine.components.Interactable;
import org.ravenEngine.components.RigidBody2D;
import org.ravenEngine.components.SpriteAnimator;
import org.ravenEngine.core.Engine;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.components.Component;
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

        // 1. CAPTURA DE ENTRADAS (EJE Y)
        if (KeyListener.isKeyPressed(GLFW_KEY_W) || KeyListener.isKeyPressed(GLFW_KEY_UP)) {
            dy += 1.0f;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_S) || KeyListener.isKeyPressed(GLFW_KEY_DOWN)) {
            dy -= 1.0f;
        }

        // 2. CAPTURA DE ENTRADAS (EJE X)
        if (KeyListener.isKeyPressed(GLFW_KEY_D) || KeyListener.isKeyPressed(GLFW_KEY_RIGHT)) {
            dx += 1.0f;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_A) || KeyListener.isKeyPressed(GLFW_KEY_LEFT)) {
            dx -= 1.0f;
        }

        // 3. INTERACCIÓN (E)
        if (KeyListener.isKeyPressed(GLFW_KEY_E)) {
            interactWithNearest();
        }

        boolean moving = (dx != 0.0f || dy != 0.0f);

        // 4. NORMALIZACIÓN VECTORIAL (Evita velocidad extra en diagonales)
        if (moving) {
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
        }

        // 5. APLICACIÓN DE FÍSICAS (VÍA RIGIDBODY2D)
        RigidBody2D rb = gameObject.getComponent(RigidBody2D.class);
        if (rb != null) {
            if (moving) {
                rb.setVelocity(dx * speed, dy * speed);
            } else {
                // Al soltar las teclas se frena en seco para mantener control preciso top-down
                rb.setVelocity(0.0f, 0.0f);
            }
        } else {
            // Fallback por si la entidad no tiene Rigidbody2D
            if (moving) {
                gameObject.transform.pos.x += dx * speed * dt;
                gameObject.transform.pos.y += dy * speed * dt;
            }
        }

        // 6. CONTROL DE ANIMACIONES
        if (moving) {
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
                } else {
                    animator.play("walk_right");
                    lastDirection = "right";
                }
            }
        } else {
            // Reposo mirando en la última dirección registrada
            if (animator != null) {
                animator.play("idle_" + lastDirection);
            }
        }
    }

    private void interactWithNearest() {
        if (Engine.getCurrentScene() == null || this.gameObject == null) return;

        Vector2f playerCenter = new Vector2f(
                gameObject.transform.pos.x + (gameObject.transform.scale.x / 2.0f),
                gameObject.transform.pos.y + (gameObject.transform.scale.y / 2.0f)
        );

        Interactable nearestInteractable = null;
        float closestDistance = Float.MAX_VALUE;

        // Buscamos entre todos los GameObjects de la escena
        for (GameObject go : Engine.getCurrentScene().getGameObjects()) {
            if (go == this.gameObject) continue;

            Interactable interactable = go.getComponent(Interactable.class);
            if (interactable != null && interactable.checkPlayerDistance(playerCenter)) {
                float dist = playerCenter.distance(go.transform.pos);
                if (dist < closestDistance) {
                    closestDistance = dist;
                    nearestInteractable = interactable;
                }
            }
        }

        // Si encontramos una poción u objeto al alcance, interactuamos
        if (nearestInteractable != null) {
            nearestInteractable.onInteract(this.gameObject);
        }
    }

    @Override
    public void onCollisionEnter(GameObject other) {return;}

    @Override
    public void onCollisionExit(GameObject other) {
        return;
    }
}