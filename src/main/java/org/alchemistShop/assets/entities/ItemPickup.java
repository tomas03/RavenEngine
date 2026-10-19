package org.alchemistShop.assets.entities;

import org.ravenEngine.components.Interactable;
import org.ravenEngine.components.Inventory;
import org.ravenEngine.core.Engine;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

public class ItemPickup extends Interactable {
    public String itemId = "Pocion";
    public int quantity = 1;

    public ItemPickup() {
        this.interactionRadius = 24.0f;
    }

    public ItemPickup(String itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.interactionRadius = 24.0f;
    }

    @Override
    public void onInteract(GameObject player) {
        System.out.println("[Inventario] ¡Agarraste " + quantity + "x " + itemId + "!");

        //buscar inventario
        Inventory playerInventory = player.getComponent(Inventory.class);
        if (playerInventory != null){
            playerInventory.addItem(this.itemId,this.quantity);
        }else{
            System.out.println("[Inventario] El jugador no tiene el componente Inventario");
        }

        // Destruimos el objeto del mapa al levantarlo
        Scene scene = Engine.getCurrentScene();
        if (scene != null && this.gameObject != null) {
            scene.destroyGameObject(this.gameObject);
        }
    }
}