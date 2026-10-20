package org.ravenEngine.renderer;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class Spritesheet {
    private final Texture texture;
    private final List< Sprite > sprites;

    // Constructor automático: calcula las columnas según el ancho de la imagen
    public Spritesheet(Texture texture, int spriteWidth, int spriteHeight, int numSprites, int spacing) {
        this(texture, spriteWidth, spriteHeight, numSprites, spacing, 0, 0, (texture.getWidth() + spacing) / (spriteWidth + spacing));
    }

    public Spritesheet(Texture texture, int spriteWidth, int spriteHeight, int numSprites, int spacing, int startX, int startY) {
        this(texture, spriteWidth, spriteHeight, numSprites, spacing, startX, startY, (texture.getWidth() - startX + spacing) / (spriteWidth + spacing));
    }

    public Spritesheet(Texture texture, int spriteWidth, int spriteHeight, int numSprites, int spacing, int startX, int startY, int cols) {
        this.sprites = new ArrayList< >();
        this.texture = texture;

        for (int i = 0; i < numSprites; i++) {
            int col = i % cols;
            int row = i / cols;

            int currentX = startX + col * (spriteWidth + spacing);
            int currentY = texture.getHeight() - startY - ((row + 1) * spriteHeight);

            float leftX   = currentX / (float) texture.getWidth();
            float rightX  = (currentX + spriteWidth) / (float) texture.getWidth();
            float bottomY = currentY / (float) texture.getHeight();
            float topY    = (currentY + spriteHeight) / (float) texture.getHeight();

            Vector2f[] texCoords = new Vector2f[]{
                    new Vector2f(rightX, topY),
                    new Vector2f(rightX, bottomY),
                    new Vector2f(leftX, bottomY),
                    new Vector2f(leftX, topY)
            };

            this.sprites.add(new Sprite(this.texture, texCoords));
        }
    }

    public Sprite getSprite(int index) {
        return this.sprites.get(index);
    }

    public int size() {
        return this.sprites.size();
    }
}