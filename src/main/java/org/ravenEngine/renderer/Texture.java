package org.ravenEngine.renderer;

import org.lwjgl.BufferUtils;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    private final String filepath;
    private int textureId;
    private int width;
    private int height;

    public Texture(String resourcePath) {
        this.filepath = resourcePath;

        // 1. Generamos textura en GPU
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // 2. Parámetros para pixel art nítido
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // 3. Voltear verticalmente para coordenadas de OpenGL
        stbi_set_flip_vertically_on_load(true);

        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        // 4. Resolver la ruta absoluta del recurso
        String resolvedPath = resolveResourcePath(resourcePath);
        if (resolvedPath == null) {
            throw new RuntimeException("Error: No se pudo encontrar el archivo de textura: " + resourcePath);
        }

        // 5. Cargar con STBImage directamente desde la ruta en disco
        ByteBuffer image = stbi_load(resolvedPath, widthBuffer, heightBuffer, channels, 4);

        if (image != null) {
            this.width = widthBuffer.get(0);
            this.height = heightBuffer.get(0);

            // Subir a la GPU en formato RGBA
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.width, this.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            stbi_image_free(image);
        } else {
            throw new RuntimeException("Error al procesar la imagen con STB: " + resolvedPath + " -> " + stbi_failure_reason());
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private String resolveResourcePath(String resource) {
        String pathWithSlash = resource.startsWith("/") ? resource : "/" + resource;
        URL url = Texture.class.getResource(pathWithSlash);
        if (url != null) {
            return new File(url.getFile()).getAbsolutePath();
        }

        String pathWithoutSlash = resource.startsWith("/") ? resource.substring(1) : resource;
        File file = new File("src/main/resources/" + pathWithoutSlash);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        return null;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void destroy(){
        if (this.textureId != 0){
            glDeleteTextures(this.textureId);
            this.textureId = 0;
        }
    }
}