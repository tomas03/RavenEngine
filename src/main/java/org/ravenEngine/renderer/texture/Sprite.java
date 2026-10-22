package org.ravenEngine.renderer.texture;

import org.joml.Vector2f;

public class Sprite {
    private Texture texture;
    private Vector2f[] texCoords;

    public Sprite(Texture texture) {
        this.texture = texture;
        this.texCoords = new Vector2f[]{
                new Vector2f(1,1),
                new Vector2f(1,0),
                new Vector2f(0,0),
                new Vector2f(0,1)
        };
    }

    public Sprite(Texture texture, Vector2f[] texCoords) {
        this.texture = texture;
        this.texCoords = texCoords;
    }

    public Texture getTexture(){return texture;}

    public Vector2f[] getTexCoords() {return texCoords;}

    public Sprite getFlippedX() {
        Vector2f[] flipped = new Vector2f[]{
                new Vector2f(texCoords[2].x, texCoords[0].y), // Arriba Derecha toma la X izquierda
                new Vector2f(texCoords[2].x, texCoords[1].y), // Abajo Derecha toma la X izquierda
                new Vector2f(texCoords[0].x, texCoords[2].y), // Abajo Izquierda toma la X derecha
                new Vector2f(texCoords[0].x, texCoords[3].y)  // Arriba Izquierda toma la X derecha
        };
        return new Sprite(this.texture, flipped);
    }
    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public void setTexCoords(Vector2f[] texCoords) {this.texCoords = texCoords;}
    // Invierte las coordenadas UV horizontales para espejar el sprite

}
