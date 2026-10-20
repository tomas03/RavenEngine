package org.ravenEngine.editor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ProjectExporter {
    public static void exportBuild(String startScenePath){
        new Thread(()->{
            try{
                System.out.println("[Exportador] Iniciando empaquetado de Release");
                Path buildDir = Paths.get("build/release");
                if (Files.exists(buildDir)){
                    deleteDirectory(buildDir.toFile());
                }
                Files.createDirectories(buildDir);

                //Copiar carpeta de assets
                Path sourceAssets = Paths.get("alchemistShop/assets");
                Path targetAssets = buildDir.resolve("alchemistShop/assets");
                if (Files.exists(sourceAssets)) {
                    copyFolder(sourceAssets, targetAssets);
                    System.out.println("[Exportador] Assets copiados correctamente");
                }
                // 2. Crear script de lanzamiento para Linux (CachyOS)
                File runScript = buildDir.resolve("lanzar_juego.sh").toFile();
                try (FileWriter writer = new FileWriter(runScript)) {
                    writer.write("#!/bin/bash\n");
                    writer.write("# Script de lanzamiento Standalone - RavenEngine\n");
                    writer.write("cd \"$(dirname \"$0\")\"\n");
                    writer.write("java -jar RavenEngine.jar --release\n");
                }
                runScript.setExecutable(true);

                System.out.println("[Exportador] ¡Build finalizado con éxito en: " + buildDir.toAbsolutePath() + "!");
                System.out.println("[Exportador] Podés ejecutarlo con: ./build/release/lanzar_juego.sh");
            }catch (Exception e) {
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
