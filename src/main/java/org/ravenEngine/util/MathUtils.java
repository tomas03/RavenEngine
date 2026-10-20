package org.ravenEngine.util;

public class MathUtils {
    // Interpolación Lineal (Linear Interpolation)
    // a = valor actual, b = valor destino, t = factor de suavizado [0.0 - 1.0]
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}