package org.ravenEngine.editor.hub;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProjectHubManager {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.ravenengine";
    private static final String PROJECTS_FILE = CONFIG_DIR + "/projects.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static List<ProjectData> loadProjects() {
        File file = new File(PROJECTS_FILE);
        if (!file.exists()) return new ArrayList<>();

        try {
            String json = Files.readString(file.toPath());
            List<ProjectData> list = gson.fromJson(json, new TypeToken<List<ProjectData>>(){}.getType());
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static void saveProjects(List<ProjectData> projects) {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();
            Files.writeString(Path.of(PROJECTS_FILE), gson.toJson(projects));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void registerProject(String name, String path) {
        List<ProjectData> list = loadProjects();
        list.removeIf(p -> p.path.equalsIgnoreCase(path));
        list.add(0, new ProjectData(name, path));
        saveProjects(list);
    }

    public static void createProjectStructure(String projectPath, String projectName) {
        File root = new File(projectPath);
        new File(root, "assets/scenes").mkdirs();
        new File(root, "assets/textures").mkdirs();
        new File(root, "assets/audio").mkdirs();
        new File(root, "scripts").mkdirs();

        // Generar un project_settings.json limpio para el nuevo juego
        String settingsJson = "{\n" +
                "  \"gameTitle\": \"" + projectName + "\",\n" +
                "  \"defaultScenePath\": \"assets/scenes/main.json\"\n" +
                "}";

        try {
            Files.writeString(new File(root, "project_settings.json").toPath(), settingsJson);
            // Generar una escena inicial vacía
            Files.writeString(new File(root, "assets/scenes/main.json").toPath(), "[]");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}