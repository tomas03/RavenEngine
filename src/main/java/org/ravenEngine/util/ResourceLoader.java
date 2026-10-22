package org.ravenEngine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourceLoader {

    /**
     * Obtiene un InputStream buscando primero dentro del JAR (Classpath)
     * y como alternativa en el disco local si se ejecuta desde el IDE.
     */
    public static InputStream getResourceAsStream(String resourcePath) {
        // 1. Buscar en el Classpath (dentro del JAR compilado)
        String normalizedPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        InputStream in = ResourceLoader.class.getResourceAsStream(normalizedPath);
        if (in != null) {
            return in;
        }

        // 2. Si no está en la raíz del classpath, buscar con el ClassLoader
        String relativePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        in = ClassLoader.getSystemResourceAsStream(relativePath);
        if (in != null) {
            return in;
        }

        // 3. Fallback: buscar directamente en el sistema de archivos (desarrollo local)
        try {
            File diskFile = new File(resourcePath);
            if (diskFile.exists()) {
                return new FileInputStream(diskFile);
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Carga todos los bytes de un recurso a memoria.
     */
    public static byte[] loadResourceBytes(String resourcePath) {
        try (InputStream in = getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            return in.readAllBytes();
        } catch (Exception e) {
            System.err.println("[ResourceLoader ERROR] No se pudieron leer bytes de: " + resourcePath + " -> " + e.getMessage());
            return null;
        }
    }

    /**
     * Carga un archivo de texto completo (como un Shader GLSL).
     */
    public static String loadResourceString(String resourcePath) {
        byte[] bytes = loadResourceBytes(resourcePath);
        if (bytes == null) return null;
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}