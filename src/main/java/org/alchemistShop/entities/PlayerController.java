package org.alchemistShop.entities;

import org.ravenEngine.components.Animation;
import org.ravenEngine.components.BoxCollider2D;
import org.ravenEngine.components.Component;
import org.ravenEngine.components.SpriteRenderer;
import org.ravenEngine.core.Engine;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.scene.GameObject;


import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;


public class PlayerController  extends Component {
    private final float speed = 150.0f;
    private SpriteRenderer spr;
    //traer animaciones
    private Animation walkUp;
    private Animation walkDown;
    private Animation walkLeft;
    private Animation walkRight;

    private final Spritesheet sheet;
    private Animation currentAnim;

    private BoxCollider2D boxCollider;

    public PlayerController(Spritesheet sheet) {
        this.sheet = sheet;
    }

    @Override
    public void start(){
        this.spr = gameObject.getComponent(SpriteRenderer.class);

        this.boxCollider = gameObject.getComponent(BoxCollider2D.class);

        // Fila 0 (arriba): Frente
        this.walkDown = new Animation(Arrays.asList(
                sheet.getSprite(0),
                sheet.getSprite(1),
                sheet.getSprite(2)), 0.15f);

        // Fila 1: Derecha
        this.walkRight = new Animation(Arrays.asList(
                sheet.getSprite(6),
                sheet.getSprite(7),
                sheet.getSprite(8)), 0.15f);

        // Izquierda: Espejamos la fila de la derecha con getFlippedX()
        this.walkLeft = new Animation(Arrays.asList(
                sheet.getSprite(3),
                sheet.getSprite(4),
                sheet.getSprite(5)), 0.15f);

        // Fila 3 (abajo en el png): Espalda / Arriba (sprites 12, 13, 14, 15)
        this.walkUp = new Animation(Arrays.asList(
                sheet.getSprite(9),
                sheet.getSprite(10),
                sheet.getSprite(11)), 0.15f);

        this.currentAnim = walkDown;
    }

    private boolean checkCollision(){
        if (this.boxCollider == null) return false;
        List<BoxCollider2D> colliders = Engine.getCurrentScene().getColliders();
        for (BoxCollider2D other:colliders){
            //no colisionar contra uno mismo
            if (other == this.boxCollider) continue;
            if (this.boxCollider.isTrigger()){
                return true;
            }
        }
        return false;
    }

    @Override
    public void update(float dt){
        boolean moving = false;

        //EJE Y
        float prevY = gameObject.transform.pos.y;
        if (KeyListener.isKeyPressed(GLFW_KEY_W)|| KeyListener.isKeyPressed(GLFW_KEY_UP)){
            gameObject.transform.pos.y += speed*dt;
            currentAnim = walkUp;
            moving = true;
        } else if (KeyListener.isKeyPressed(GLFW_KEY_S)|| KeyListener.isKeyPressed(GLFW_KEY_DOWN)) {
            gameObject.transform.pos.y -= speed*dt;
            currentAnim = walkDown;
            moving = true;
        }
        //si chocas en Y volves al paso anterior
        if (checkCollision()){
            gameObject.transform.pos.y = prevY;
        }

        //EJE X
        float prevX = gameObject.transform.pos.x;
        if (KeyListener.isKeyPressed(GLFW_KEY_D)||KeyListener.isKeyPressed(GLFW_KEY_RIGHT)){
            gameObject.transform.pos.x += speed*dt;
            currentAnim = walkRight;
            moving = true;
        }else if(KeyListener.isKeyPressed(GLFW_KEY_A)||KeyListener.isKeyPressed(GLFW_KEY_LEFT)){
            gameObject.transform.pos.x -= speed*dt;
            currentAnim = walkLeft;
            moving = true;
        }
        //si chocas en X volves al paso anterior
        if (checkCollision()){
            gameObject.transform.pos.x = prevX;
        }

        //ANIMACIONES
        if (moving){
            currentAnim.update(dt);
            spr.setSprite(currentAnim.getCurrentSprite());
        }else {
            currentAnim.reset();
            spr.setSprite(currentAnim.getCurrentSprite());
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
