package org.ravenEngine.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joml.Vector2f;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ProjectSettings {

    private static String currentSettingsPath = "project_settings.json";
    private static ProjectSettings instance;

    // Propiedades del proyecto (defaults genéricos)
    public String gameTitle = "Nuevo Juego";
    public String defaultScenePath = "assets/scenes/main.json";
    public String iconPath = "assets/icon.png";
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
        loadFromCustomPath(currentSettingsPath);
    }

    public static ProjectSettings loadFromCustomPath(String filePath) {
        currentSettingsPath = filePath;
        File file = new File(filePath);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                instance = new Gson().fromJson(reader, ProjectSettings.class);
                if (instance != null) {
                    return instance;
                }
            } catch (Exception e) {
                System.err.println("[Settings] Error al leer " + filePath + ": " + e.getMessage());
            }
        }
        instance = new ProjectSettings();
        instance.save();
        return instance;
    }

    public void save() {
        try {
            File file = new File(currentSettingsPath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                writer.write(gson.toJson(this));
                System.out.println("[Settings] Configuración guardada en: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[Settings] Error al guardar configuración: " + e.getMessage());
        }
    }
}