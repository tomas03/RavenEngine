package org.ravenEngine.util;

import org.ravenEngine.audio.Sound;
import org.ravenEngine.renderer.Shader;
import org.ravenEngine.renderer.Spritesheet;
import org.ravenEngine.renderer.Texture;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AssetPool {
    private static final Map< String, Shader > shaders = new HashMap<>();
    private static final Map< String, Texture > textures = new HashMap<>();
    private static final Map< String, Spritesheet > spritesheets = new HashMap<>();
    private static final Map< String, Sound> sounds = new HashMap<>();

    // SHADERS

    public static Shader getShader(String vertexPath, String fragmentPath){
        File vertFile = new File(vertexPath);
        File fragFile = new File(fragmentPath);

        String combinedKey = vertFile.getAbsolutePath() + "###" + fragFile.getAbsolutePath();

        if (shaders.containsKey(combinedKey)){
            return shaders.get(combinedKey);
        } else {
            Shader shader = new Shader(vertexPath, fragmentPath);
            shader.compile();
            shaders.put(combinedKey, shader);
            return shader;
        }
    }

    // TEXTURAS

    public static Texture getTexture(String resourceName){
        File file = new File(resourceName);
        String absolutePath = file.getAbsolutePath();

        if (textures.containsKey(absolutePath)){
            return textures.get(absolutePath);
        } else {
            Texture texture = new Texture(resourceName);
            textures.put(absolutePath, texture);
            return texture;
        }
    }

    // SPRITESHEETS

    public static void addSpritesheet(String resourceName, Spritesheet spritesheet) {
        File file = new File(resourceName);
        String absolutePath = file.getAbsolutePath();

        if (!spritesheets.containsKey(absolutePath)) {
            spritesheets.put(absolutePath, spritesheet);
        }
    }

    public static Spritesheet getSpritesheet(String resourceName) {
        File file = new File(resourceName);
        String absolutePath = file.getAbsolutePath();

        if (!spritesheets.containsKey(absolutePath)) {
            System.err.println("[AssetPool ERROR] No se encontró el spritesheet: " + resourceName);
            return null;
        }
        return spritesheets.get(absolutePath);
    }

    // SONIDOS
    public static Sound getSound(String soundPath) {
        File file = new File(soundPath);
        String absolutePath = file.getAbsolutePath();

        if (sounds.containsKey(absolutePath)) {
            return sounds.get(absolutePath);
        } else {
            Sound sound = new Sound(soundPath, false);
            sounds.put(absolutePath, sound);
            return sound;
        }
    }

    public static Sound addSound(String soundPath, boolean loops) {
        File file = new File(soundPath);
        String absolutePath = file.getAbsolutePath();

        if (sounds.containsKey(absolutePath)) {
            return sounds.get(absolutePath);
        } else {
            Sound sound = new Sound(soundPath, loops);
            sounds.put(absolutePath, sound);
            return sound;
        }
    }

    // LIMPIEZA DE VRAM

    public static void cleanUp(){
        for (Texture texture : textures.values()){
            texture.destroy();
        }
        textures.clear();

        for (Shader shader : shaders.values()){
            shader.destroy();
        }
        shaders.clear();

        // Limpiamos referencias de spritesheets
        spritesheets.clear();

        //limpieza de sonidos
        for (Sound sound : sounds.values()) {
            sound.destroy();
        }
        sounds.clear();
    }
}