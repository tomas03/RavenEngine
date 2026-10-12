package org.ravenEngine.util;

import org.ravenEngine.renderer.Shader;
import org.ravenEngine.renderer.Texture;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AssetPool {
    private static final Map<String, Shader> shaders = new HashMap<>();
    private static final Map<String, Texture> textures = new HashMap<>();

    // SHADERS

    public static Shader getShader(String vertexPath, String fragmentPath){
        File vertFile = new File(vertexPath);
        File fragFile = new File(fragmentPath);

        String combinedKey = vertFile.getAbsolutePath()+"###"+fragFile.getAbsolutePath();

        if (shaders.containsKey(combinedKey)){
            return shaders.get(combinedKey);
        }else{
            Shader shader = new Shader(vertexPath,fragmentPath);
            shader.compile();
            shaders.put(combinedKey, shader);
            return shader;
        }
    }

    //TEXTURAS

    public static Texture getTexture(String resourceName){
        File file = new File(resourceName);
        String absolutePath = file.getAbsolutePath();

        if (textures.containsKey(absolutePath)){
            return  textures.get(absolutePath);
        }else{
            Texture texture = new Texture(resourceName);
            textures.put(absolutePath,texture);
            return texture;
        }
    }

    //LIMPIEZA DE VRAM

    public static void cleanUp(){
        for (Texture texture: textures.values()){
            texture.destroy();
        }
        textures.clear();

        for (Shader shader : shaders.values()){
            shader.destroy();
        }
        shaders.clear();
    }
}
