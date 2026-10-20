package org.ravenEngine.physics;

import org.joml.Vector2f;
import org.ravenEngine.components.*;
import org.ravenEngine.components.Component;
import org.ravenEngine.scene.GameObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Physics2D {
    private final List<BoxCollider2D> colliders = new ArrayList< >();
    private final List<RigidBody2D> rigidbodies = new ArrayList< >();

    private final Set<CollisionPair> activeCollisions = new HashSet< >();
    private final Set<CollisionPair> currentCollisions = new HashSet< >();

    // ==========================================
    // CICLO DE SIMULACIÓN
    // ==========================================
    public void update(float dt, List< GameObject > gameObjects) {
        // 1. Integración de movimiento con Rigidbody2D
        for (RigidBody2D rb : rigidbodies) {
            rb.updatePhysics(dt);
        }

        // 2. Colisión contra Tilemap
        Tilemap activeTilemap = null;
        for (GameObject go : gameObjects) {
            Tilemap tm = go.getComponent(Tilemap.class);
            if (tm != null) {
                activeTilemap = tm;
                break;
            }
        }

        if (activeTilemap != null) {
            for (BoxCollider2D col : colliders) {
                if (!col.isStatic()) {
                    resolveTilemapCollisions(col, activeTilemap);
                }
            }
        }

        // 3. Colisión entre entidades (AABB)
        currentCollisions.clear();
        for (int i = 0; i < colliders.size(); i++) {
            BoxCollider2D colA = colliders.get(i);
            for (int j = i + 1; j < colliders.size(); j++) {
                BoxCollider2D colB = colliders.get(j);

                if (checkAABB(colA, colB)) {
                    CollisionPair pair = new CollisionPair(colA, colB);
                    currentCollisions.add(pair);

                    boolean isTriggerInteraction = colA.isTrigger() || colB.isTrigger();
                    if (!isTriggerInteraction) {
                        resolvePhysicsCollision(colA, colB);
                    }

                    if (!activeCollisions.contains(pair)) {
                        notifyCollisionEnter(colA, colB, isTriggerInteraction);
                    } else {
                        notifyCollisionStay(colA, colB, isTriggerInteraction);
                    }
                }
            }
        }

        // 4. Salida de colisiones (Exit)
        for (CollisionPair pair : activeCollisions) {
            if (!currentCollisions.contains(pair)) {
                boolean isTriggerInteraction = pair.a.isTrigger() || pair.b.isTrigger();
                notifyCollisionExit(pair.a, pair.b, isTriggerInteraction);
            }
        }

        activeCollisions.clear();
        activeCollisions.addAll(currentCollisions);
    }

    // ==========================================
    // MATEMÁTICA Y RESOLUCIÓN DE CHOQUES
    // ==========================================
    private boolean checkAABB(BoxCollider2D a, BoxCollider2D b) {
        return a.getMinX() < b.getMaxX() &&
                a.getMaxX() > b.getMinX() &&
                a.getMinY() < b.getMaxY() &&
                a.getMaxY() > b.getMinY();
    }

    private void resolvePhysicsCollision(BoxCollider2D a, BoxCollider2D b) {
        if (a.isStatic() && b.isStatic()) return;

        float overlapLeft   = a.getMaxX() - b.getMinX();
        float overlapRight  = b.getMaxX() - a.getMinX();
        float overlapBottom = a.getMaxY() - b.getMinY();
        float overlapTop    = b.getMaxY() - a.getMinY();

        float overlapX = Math.min(overlapLeft, overlapRight);
        float overlapY = Math.min(overlapBottom, overlapTop);

        float resolveX = (overlapLeft < overlapRight) ? -overlapX : overlapX;
        float resolveY = (overlapBottom < overlapTop) ? -overlapY : overlapY;

        if (overlapX < overlapY) {
            resolveY = 0;
        } else {
            resolveX = 0;
        }

        if (a.isStatic()) {
            b.gameObject.transform.pos.x -= resolveX;
            b.gameObject.transform.pos.y -= resolveY;
        } else if (b.isStatic()) {
            a.gameObject.transform.pos.x += resolveX;
            a.gameObject.transform.pos.y += resolveY;
        } else {
            a.gameObject.transform.pos.x += resolveX * 0.5f;
            a.gameObject.transform.pos.y += resolveY * 0.5f;

            b.gameObject.transform.pos.x -= resolveX * 0.5f;
            b.gameObject.transform.pos.y -= resolveY * 0.5f;
        }
    }

    private void resolveTilemapCollisions(BoxCollider2D collider, Tilemap tilemap) {
        if (collider.isStatic() || tilemap == null || tilemap.gameObject == null) return;

        Vector2f tilemapOrigin = tilemap.gameObject.transform.pos;
        int tileW = tilemap.getTileWidth();
        int tileH = tilemap.getTileHeight();

        int minTileX = (int) Math.floor((collider.getMinX() - tilemapOrigin.x) / tileW);
        int maxTileX = (int) Math.floor((collider.getMaxX() - tilemapOrigin.x) / tileW);
        int minTileY = (int) Math.floor((collider.getMinY() - tilemapOrigin.y) / tileH);
        int maxTileY = (int) Math.floor((collider.getMaxY() - tilemapOrigin.y) / tileH);

        for (int tx = minTileX; tx <= maxTileX; tx++) {
            for (int ty = minTileY; ty <= maxTileY; ty++) {
                if (!tilemap.isSolid(tx, ty)) continue;

                float tileMinX = tilemapOrigin.x + (tx * tileW);
                float tileMaxX = tileMinX + tileW;
                float tileMinY = tilemapOrigin.y + (ty * tileH);
                float tileMaxY = tileMinY + tileH;

                float curMinX = collider.getMinX();
                float curMaxX = collider.getMaxX();
                float curMinY = collider.getMinY();
                float curMaxY = collider.getMaxY();

                float overlapLeft   = curMaxX - tileMinX;
                float overlapRight  = tileMaxX - curMinX;
                float overlapBottom = curMaxY - tileMinY;
                float overlapTop    = tileMaxY - curMinY;

                if (overlapLeft > 0 && overlapRight > 0 && overlapBottom > 0 && overlapTop > 0) {
                    float overlapX = Math.min(overlapLeft, overlapRight);
                    float overlapY = Math.min(overlapBottom, overlapTop);

                    if (overlapX < overlapY) {
                        float resolveX = (overlapLeft < overlapRight) ? -overlapX : overlapX;
                        collider.gameObject.transform.pos.x += resolveX;
                    } else {
                        float resolveY = (overlapBottom < overlapTop) ? -overlapY : overlapY;
                        collider.gameObject.transform.pos.y += resolveY;
                    }
                }
            }
        }
    }

    // ==========================================
    // DISPACHO DE EVENTOS
    // ==========================================
    private void notifyCollisionEnter(BoxCollider2D a, BoxCollider2D b, boolean isTrigger) {
        for (Component c : a.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerEnter(b.gameObject);
            else c.onCollisionEnter(b.gameObject);
        }
        for (Component c : b.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerEnter(a.gameObject);
            else c.onCollisionEnter(a.gameObject);
        }
    }

    private void notifyCollisionStay(BoxCollider2D a, BoxCollider2D b, boolean isTrigger) {
        for (Component c : a.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerStay(b.gameObject);
            else c.onCollisionStay(b.gameObject);
        }
        for (Component c : b.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerStay(a.gameObject);
            else c.onCollisionStay(a.gameObject);
        }
    }

    private void notifyCollisionExit(BoxCollider2D a, BoxCollider2D b, boolean isTrigger) {
        for (Component c : a.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerExit(b.gameObject);
            else c.onCollisionExit(b.gameObject);
        }
        for (Component c : b.gameObject.getAllComponents()) {
            if (isTrigger) c.onTriggerExit(a.gameObject);
            else c.onCollisionExit(a.gameObject);
        }
    }

    // ==========================================
    // GESTIÓN DE ENTIDADES
    // ==========================================
    public void add(GameObject go) {
        BoxCollider2D col = go.getComponent(BoxCollider2D.class);
        if (col != null && !colliders.contains(col)) {
            colliders.add(col);
        }

        RigidBody2D rb = go.getComponent(RigidBody2D.class);
        if (rb != null && !rigidbodies.contains(rb)) {
            rigidbodies.add(rb);
        }
    }

    public void remove(GameObject go) {
        BoxCollider2D col = go.getComponent(BoxCollider2D.class);
        if (col != null) {
            colliders.remove(col);
        }

        RigidBody2D rb = go.getComponent(RigidBody2D.class);
        if (rb != null) {
            rigidbodies.remove(rb);
        }
    }

    public void clear() {
        this.colliders.clear();
        this.rigidbodies.clear();
        this.activeCollisions.clear();
        this.currentCollisions.clear();
    }

    public List< BoxCollider2D > getColliders() {
        return colliders;
    }
}