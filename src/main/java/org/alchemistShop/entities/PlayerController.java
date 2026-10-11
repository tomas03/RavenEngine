package org.alchemistShop.entities;

import org.ravenEngine.components.Animation;
import org.ravenEngine.components.Component;
import org.ravenEngine.components.SpriteRenderer;
import org.ravenEngine.io.KeyListener;
import org.ravenEngine.renderer.Spritesheet;


import java.util.Arrays;

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

    public PlayerController(Spritesheet sheet) {
        this.sheet = sheet;
    }

    @Override
    public void start(){
        this.spr = gameObject.getComponent(SpriteRenderer.class);

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

    @Override
    public void update(float dt){
        boolean moving = false;

        if (KeyListener.isKeyPressed(GLFW_KEY_W)|| KeyListener.isKeyPressed(GLFW_KEY_UP)){
            gameObject.transform.pos.y += speed*dt;
            currentAnim = walkUp;
            moving = true;
        } else if (KeyListener.isKeyPressed(GLFW_KEY_S)|| KeyListener.isKeyPressed(GLFW_KEY_DOWN)) {
            gameObject.transform.pos.y -= speed*dt;
            currentAnim = walkDown;
            moving = true;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_D)||KeyListener.isKeyPressed(GLFW_KEY_RIGHT)){
            gameObject.transform.pos.x += speed*dt;
            currentAnim = walkRight;
            moving = true;
        }else if(KeyListener.isKeyPressed(GLFW_KEY_A)||KeyListener.isKeyPressed(GLFW_KEY_LEFT)){
            gameObject.transform.pos.x -= speed*dt;
            currentAnim = walkLeft;
            moving = true;
        }
        if (moving){
            currentAnim.update(dt);
            spr.setSprite(currentAnim.getCurrentSprite());
        }else {
            currentAnim.reset();
            spr.setSprite(currentAnim.getCurrentSprite());
        }
    }
}
