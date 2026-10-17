package org.ravenEngine.components;

import org.joml.Vector2f;
import org.ravenEngine.scene.Component;
import org.ravenEngine.scene.GameObject;

public abstract class Interactable extends Component {
    //Radio en píxeles donde el personaje puede interactuar con el objeto
    public float interactionRadius = 24.0f;
    protected boolean isPlayerInRange = false;

    public abstract void onInteract(GameObject player);

    public boolean checkPlayerDistance(Vector2f playerPos){
        if (gameObject == null||gameObject.transform == null)return false;
        //calcular distancia centro a centro
        float centerX = gameObject.transform.pos.x + (gameObject.transform.scale.x/2.0f);
        float centerY = gameObject.transform.pos.y + (gameObject.transform.scale.y/2.0f);

        float dx = playerPos.x - centerX;
        float dy = playerPos.y - centerY;
        float distanceSquared = (dx*dx) + (dy*dy);

        this.isPlayerInRange = distanceSquared<=(interactionRadius*interactionRadius);
        return this.isPlayerInRange;
    }

    public boolean isInRange(){
        return isPlayerInRange;
    }
}
