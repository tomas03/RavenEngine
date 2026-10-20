package org.ravenEngine.physics;

import org.ravenEngine.components.BoxCollider2D;

import java.util.Objects;

public class CollisionPair {
    public final BoxCollider2D a;
    public final BoxCollider2D b;

    public CollisionPair(BoxCollider2D a, BoxCollider2D b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CollisionPair that = (CollisionPair) o;
        return (Objects.equals(a, that.a) && Objects.equals(b, that.b)) ||
                (Objects.equals(a, that.b) && Objects.equals(b, that.a));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(a) ^ Objects.hashCode(b);
    }
}