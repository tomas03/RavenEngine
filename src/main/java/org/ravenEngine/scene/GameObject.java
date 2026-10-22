package org.ravenEngine.scene;

import org.ravenEngine.components.Component;
import org.ravenEngine.components.Transform;

import java.util.ArrayList;
import java.util.List;

public class GameObject {
    private String name;
    private final List<Component> components;
    public Transform transform;
    private int zIndex = 0;

    // Relación de jerarquía estilo Godot
    private String parentName = null; // Se guarda en el JSON
    private transient GameObject parent = null;
    private transient List<GameObject> children = new ArrayList<>();

    public GameObject(String name, Transform transform) {
        this.name = name;
        this.transform = transform;
        if (this.transform != null) {
            this.transform.gameObject = this;
        }
        this.components = new ArrayList<>();
    }

    public GameObject(String name, Transform transform, int zIndex) {
        this(name, transform);
        this.zIndex = zIndex;
    }

    public GameObject(String name) {
        this(name, new Transform(new org.joml.Vector2f(0.0f, 0.0f)));
    }

    // ==========================================
    // MÉTODOS DE JERARQUÍA (PADRE / HIJO)
    // ==========================================
    public List<GameObject> getChildren() {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        return this.children;
    }

    public boolean hasChildren() {
        return this.children != null && !this.children.isEmpty();
    }

    public void addChild(GameObject child) {
        if (child == null || child == this) return;

        if (child.parent != null) {
            child.parent.removeChild(child);
        }

        child.parent = this;
        child.parentName = this.name;
        if (!getChildren().contains(child)) {
            getChildren().add(child);
        }
    }

    public void removeChild(GameObject child) {
        if (child != null && getChildren().remove(child)) {
            child.parent = null;
            child.parentName = null;
        }
    }
    public GameObject getParent() {
        return parent;
    }


    public String getParentName() {
        return parent != null ? parent.getName() : parentName;
    }

    // ==========================================
    // COMPONENTES Y CICLO DE VIDA
    // ==========================================
    public <T extends Component> T getComponent(Class<T> componentClass) {
        for (Component c : components) {
            if (componentClass.isAssignableFrom(c.getClass())) {
                return componentClass.cast(c);
            }
        }
        return null;
    }

    public void addComponent(Component c) {
        c.gameObject = this;
        this.components.add(c);
    }

    public void start() {
        for (Component component : components) {
            component.invokeStart();
        }
    }

    public void update(float dt) {
        for (Component component : components) {
            component.update(dt);
        }
    }

    public int getzIndex() {
        return zIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        // Si renombramos al padre, actualizamos la referencia en los hijos
        for (GameObject child : children) {
            child.parentName = name;
        }
    }

    public List<Component> getComponents() {
        return this.components;
    }

    public List<Component> getAllComponents() {
        return this.components;
    }
}