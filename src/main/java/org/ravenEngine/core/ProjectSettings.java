package org.ravenEngine.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector2f;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ProjectSettings {

    private static final String CONFIG_FILE = "project_settings.json";
    private static ProjectSettings instance;

    // Propiedades del proyecto
    public String gameTitle = "Alchemist Shop";
    public String defaultScenePath = "src/main/java/org/alchemistShop/assets/level1.json";
    public int windowWidth = 1280;
    public int windowHeight = 720;
    public Vector2f gravity = new Vector2f(0.0f, -9.8f);
    public boolean vSync = true;

    public static ProjectSettings get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                instance = new Gson().fromJson(reader, ProjectSettings.class);
                if (instance != null) return;
            } catch (Exception e) {
                System.err.println("[Settings] Error al leer project_settings.json: " + e.getMessage());
            }
        }
        instance = new ProjectSettings();
        instance.save();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            writer.write(gson.toJson(this));
            System.out.println("[Settings] Configuración global guardada en: " + CONFIG_FILE);
        } catch (Exception e) {
            System.err.println("[Settings] Error al guardar configuración: " + e.getMessage());
        }
    }
}