package org.ravenEngine.components;

import org.joml.Vector2f;

public class RigidBody2D extends Component {
    private Vector2f velocity = new Vector2f(0.0f, 0.0f);
    private float linearDamping = 0.92f; // Rozamiento natural para frenado suave
    private float mass = 1.0f;
    private boolean isKinematic = false; // Kinematic: solo se mueve por código, no por colisiones

    public RigidBody2D() {}

    public RigidBody2D(float mass) {
        this.mass = Math.max(0.001f, mass);
    }

    public void updatePhysics(float dt) {
        if (gameObject == null || gameObject.transform == null || isKinematic) return;

        // Desplazamiento por velocidad
        gameObject.transform.pos.x += velocity.x * dt;
        gameObject.transform.pos.y += velocity.y * dt;

        // Fricción / Amortiguación
        velocity.mul(linearDamping);

        // Umbral mínimo para evitar números microscópicos en float
        if (Math.abs(velocity.x) < 0.01f) velocity.x = 0.0f;
        if (Math.abs(velocity.y) < 0.01f) velocity.y = 0.0f;
    }

    public void addForce(Vector2f force) {
        if (isKinematic) return;
        // F = m * a  =>  a = F / m
        this.velocity.x += (force.x / mass);
        this.velocity.y += (force.y / mass);
    }

    public void addImpulse(Vector2f impulse) {
        if (isKinematic) return;
        this.velocity.add(impulse);
    }

    public void setVelocity(float x, float y) {
        this.velocity.set(x, y);
    }

    public Vector2f getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2f velocity) {
        this.velocity.set(velocity);
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = Math.max(0.001f, mass);
    }

    public float getLinearDamping() {
        return linearDamping;
    }

    public void setLinearDamping(float linearDamping) {
        this.linearDamping = linearDamping;
    }

    public boolean isKinematic() {
        return isKinematic;
    }

    public void setKinematic(boolean kinematic) {
        isKinematic = kinematic;
    }
}