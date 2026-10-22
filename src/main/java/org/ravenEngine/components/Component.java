package org.ravenEngine.components;

import org.ravenEngine.scene.GameObject;

public class Component {
    public transient GameObject gameObject = null;
    private transient boolean started = false;

    public void start(){}
    public void update(float dt){}
    public void invokeStart() {if (!this.started) {this.start();this.started = true;}}

    public void resetState() {
        this.started = false;
    }

    // Eventos de colisión física (cuerpos sólidos)
    public void onCollisionEnter(GameObject other) {}
    public void onCollisionStay(GameObject other) {}
    public void onCollisionExit(GameObject other) {}

    // Eventos de triggers (sensores / zonas de paso)
    public void onTriggerEnter(GameObject other) {}
    public void onTriggerStay(GameObject other) {}
    public void onTriggerExit(GameObject other) {}
}
