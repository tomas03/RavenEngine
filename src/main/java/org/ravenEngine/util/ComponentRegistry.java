package org.ravenEngine.util;

import org.ravenEngine.audio.AudioSource;
import org.ravenEngine.components.*;
import org.ravenEngine.renderer.texture.SpriteRenderer;

import java.util.*;

public class ComponentRegistry {

    public static class ComponentEntry {
        public final String category;
        public final Class< ? extends Component > componentClass;

        public ComponentEntry(String category, Class< ? extends Component > componentClass) {
            this.category = category;
            this.componentClass = componentClass;
        }
    }

    private static final List< ComponentEntry > entries = new ArrayList<>();

    static {
        // ==========================================
        // COMPONENTES NATIVOS DEL MOTOR (Fijos en el JAR)
        // ==========================================
        register("Rendering", SpriteRenderer.class);
        register("Rendering", TextRenderer.class);
        register("Rendering", SpriteAnimator.class);
        register("Rendering", ParticleSystem.class);
        register("Rendering", Tilemap.class);

        register("Physics 2D", BoxCollider2D.class);
        register("Physics 2D", RigidBody2D.class);

        register("Audio", AudioSource.class);

        register("Gameplay", DialogueInteractable.class);
        register("Gameplay", Inventory.class);

        register("Camera / Utility", CameraFollow.class);

        register("UI (Canvas)", UIProgressBar.class);
        register("UI (Canvas)", UIImage.class);
    }

    public static synchronized void register(String category, Class< ? extends Component > compClass) {
        entries.add(new ComponentEntry(category, compClass));
    }

    /**
     * Registra o actualiza un script dinámico del juego.
     * Si el componente ya existía (por ejemplo, tras recompilar con Ctrl + B),
     * se reemplaza para que apunte a la nueva clase en memoria.
     */
    public static synchronized void registerOrUpdateScript(Class< ? extends Component > scriptClass) {
        if (scriptClass == null) return;

        String simpleName = scriptClass.getSimpleName();

        // Eliminamos la versión anterior si existía
        entries.removeIf(entry -> entry.componentClass.getSimpleName().equals(simpleName));

        // Lo agregamos bajo la categoría "Scripts"
        entries.add(new ComponentEntry("Scripts", scriptClass));
        System.out.println("[ComponentRegistry] Script registrado exitosamente: " + simpleName);
    }

    public static synchronized void unregisterScript(String scriptSimpleName) {
        entries.removeIf(entry -> entry.componentClass.getSimpleName().equals(scriptSimpleName));
    }

    public static synchronized List< ComponentEntry > getAllEntries() {
        return new ArrayList<>(entries);
    }

    public static synchronized Set< String > getCategories() {
        Set< String > categories = new LinkedHashSet<>();
        for (ComponentEntry entry : entries) {
            categories.add(entry.category);
        }
        return categories;
    }

    public static synchronized List< Class< ? extends Component > > getComponentsByCategory(String category) {
        List< Class< ? extends Component > > list = new ArrayList<>();
        for (ComponentEntry entry : entries) {
            if (entry.category.equals(category)) {
                list.add(entry.componentClass);
            }
        }
        return list;
    }
}