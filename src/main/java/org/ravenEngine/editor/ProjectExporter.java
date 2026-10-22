package org.ravenEngine.editor;

import org.ravenEngine.core.ProjectSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ProjectExporter {

    // Sobrecarga por defecto
    public static void exportBuild(String projectRootPath) {
        exportBuild(projectRootPath, "build/release");
    }

    /**
     * Exporta el proyecto activo a la carpeta seleccionada por el usuario.
     */
    public static void exportBuild(String projectRootPath, String outputDirPath) {
        new Thread(() -> {
            try {
                System.out.println("[Exportador] Iniciando empaquetado de Release...");
                Path buildDir = Paths.get(outputDirPath);
                if (!Files.exists(buildDir)) {
                    Files.createDirectories(buildDir);
                }

                Path projectRoot = Paths.get(projectRootPath);
                String gameTitle = ProjectSettings.get().gameTitle.replaceAll("\\s+", "_");

                // 1. Copiar carpeta de assets
                Path sourceAssets = projectRoot.resolve("assets");
                Path targetAssets = buildDir.resolve("assets");
                if (Files.exists(sourceAssets)) {
                    copyFolder(sourceAssets, targetAssets);
                    System.out.println("[Exportador] Assets copiados correctamente.");
                }

                // 2. Copiar project_settings.json
                Path sourceSettings = projectRoot.resolve("project_settings.json");
                Path targetSettings = buildDir.resolve("project_settings.json");
                if (Files.exists(sourceSettings)) {
                    Files.copy(sourceSettings, targetSettings, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[Exportador] Configuración del proyecto copiada.");
                }

                // 3. Copiar el Fat JAR del motor
                Path sourceJar = Paths.get("target/RavenEngine-IDE.jar");
                if (!Files.exists(sourceJar)) {
                    File targetFolder = new File("target");
                    File[] jars = targetFolder.listFiles((dir, name) -> name.endsWith(".jar") && !name.startsWith("original-"));
                    if (jars != null && jars.length > 0) {
                        sourceJar = jars[0].toPath();
                    }
                }

                Path targetJar = buildDir.resolve(gameTitle + ".jar");
                if (Files.exists(sourceJar)) {
                    Files.copy(sourceJar, targetJar, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[Exportador] JAR empaquetado como: " + targetJar.getFileName());
                } else {
                    System.err.println("[Exportador ALERTA] No se encontró el .jar en target/. Corré 'mvn package' primero.");
                }

                // 4. Script de lanzamiento para Linux (CachyOS)
                File runScriptLinux = buildDir.resolve("lanzar_juego.sh").toFile();
                try (FileWriter writer = new FileWriter(runScriptLinux)) {
                    writer.write("#!/bin/bash\n");
                    writer.write("# Script de lanzamiento Standalone - RavenEngine\n");
                    writer.write("cd \"$(dirname \"$0\")\"\n");
                    writer.write("java -jar \"" + targetJar.getFileName() + "\" --release\n");
                }
                runScriptLinux.setExecutable(true);

                // 5. Script de lanzamiento para Windows (.bat)
                File runScriptWin = buildDir.resolve("lanzar_juego.bat").toFile();
                try (FileWriter writer = new FileWriter(runScriptWin)) {
                    writer.write("@echo off\n");
                    writer.write("cd /d \"%~dp0\"\n");
                    writer.write("start \"\" javaw -jar \"" + targetJar.getFileName() + "\" --release\n");
                }

                System.out.println("[Exportador] ¡Build finalizado con éxito en: " + buildDir.toAbsolutePath() + "!");

            } catch (Exception e) {
                System.err.println("[Exportador ERROR] Falló la exportación: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private static void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            Path dest = target.resolve(source.relativize(src));
            try {
                if (Files.isDirectory(src)) {
                    if (!Files.exists(dest)) Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                System.err.println("Error copiando archivo: " + src);
            }
        });
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }
}