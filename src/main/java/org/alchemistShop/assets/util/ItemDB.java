package org.alchemistShop.assets.util;

import org.joml.Vector2f;
import org.ravenEngine.renderer.Sprite;
import org.ravenEngine.renderer.SpriteRenderer;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.components.Transform;
import org.ravenEngine.util.AssetPool;
import org.alchemistShop.assets.entities.ItemPickup;

public class ItemDB {

    // Ruta de tu atlas/tileset de ítems
    private static final String ITEMS_SHEET_PATH = "/textures/items_alquimia.png";

    // Mapeo de cada ítem a su índice dentro del Spritesheet
    public static int getSpriteIndex(String itemId) {
        return switch (itemId) {
            case "PocionRoja", "Poción de Vida Menor" -> 0;
            case "VialVerde"                         -> 1;
            case "MatrazAzul"                        -> 2;
            case "ElixirPurpura"                     -> 3;
            case "Hierbas"                           -> 4;
            case "Raiz"                              -> 5;
            case "Hongo"                             -> 6;
            case "FrascoVacio"                       -> 7;
            default                                  -> 0;
        };
    }

    // Devuelve el GameObject listo para el mundo usando el recorte del spritesheet
    public static GameObject createWorldItem(String itemId, Vector2f position) {
        GameObject go = new GameObject(itemId, new Transform(new Vector2f(position), new Vector2f(16.0f, 16.0f)));

        // Obtenemos el spritesheet ya cacheado en AssetPool
        Spritesheet sheet = AssetPool.getSpritesheet(ITEMS_SHEET_PATH);
        if (sheet != null) {
            int spriteIndex = getSpriteIndex(itemId);
            Sprite sprite = sheet.getSprite(spriteIndex);
            go.addComponent(new SpriteRenderer(sprite));
        } else {
            System.err.println("[ItemDatabase ERROR] No se encontró el spritesheet en: " + ITEMS_SHEET_PATH);
        }

        // Le reponemos el componente para que RavenTom pueda volver a levantarlo
        go.addComponent(new ItemPickup(itemId, 1));
        return go;
    }

    // Lógica al consumir un ítem
    public static void useItem(String itemId, GameObject player) {
        switch (itemId) {
            case "PocionRoja", "Poción de Vida Menor" -> {
                System.out.println("[Efecto] ¡RavenTom bebió una poción y recuperó vida!");
            }
            case "Hierbas" -> {
                System.out.println("[Efecto] Consumiste hierbas medicinales.");
            }
            default -> {
                System.out.println("[Efecto] Usaste " + itemId + " sin efecto aparente.");
            }
        }
    }
}