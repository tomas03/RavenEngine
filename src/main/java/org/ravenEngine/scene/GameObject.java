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

    public GameObject(String name, Transform transform) {
        this.name = name;
        this.transform = transform;
        this.components = new ArrayList<>();
    }

    public GameObject(String name, Transform transform, int zIndex) {
        this(name, transform); // Llama al constructor de arriba que ya hace todo
        this.zIndex = zIndex;
    }

    public GameObject(String name) {
        this(name, new Transform(new org.joml.Vector2f(0.0f, 0.0f)));
    }

    public<T extends Component>T getComponent(Class<T> componentClass){
        for (Component c : components){
            if(componentClass.isAssignableFrom(c.getClass())){
                return componentClass.cast(c);
            }
        }
        return null;
    }

    public void addComponent(Component c){
        c.gameObject = this;
        this.components.add(c);
    }



    public void start(){
        for (Component component : components){
            component.start();
        }
    }
    public void update(float dt){
        for (Component component : components){
            component.update(dt);
        }
    }

    public int getzIndex() {
        return zIndex;
    }

    public String getName(){return name;}

    public void setName(String name) {this.name = name;}

    public List<Component> getComponents() {
        return this.components;
    }

    public List< Component > getAllComponents() {
        return this.components;
    }

}
