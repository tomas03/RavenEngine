package org.ravenEngine.editor.compiler;

import com.google.gson.Gson;
import org.ravenEngine.components.Component;
import org.ravenEngine.core.Engine;
import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;
import org.ravenEngine.util.ComponentRegistry;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class ScriptHotReloader {
    private static final Gson gson = new Gson();

    /**
     * Sobrecarga para compilar un archivo deduciendo su package y nombre de clase.
     * Ideal para el escaneo inicial al abrir proyectos desde el Hub.
     */
    public static boolean compileAndLoad(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) return false;
        try {
            String content = Files.readString(sourceFile.toPath());
            String packageName = "";
            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.startsWith("package ") && line.endsWith(";")) {
                    packageName = line.substring(8, line.length() - 1).trim();
                    break;
                }
            }
            String className = sourceFile.getName().replace(".java", "");
            String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;
            return reloadScript(sourceFile, fullClassName);
        } catch (Exception e) {
            System.err.println("[HotReload ERROR] No se pudo leer el archivo: " + sourceFile.getName());
            return false;
        }
    }

    /**
     * @param sourceFile Archivo .java modificado
     * @param fullClassName Nombre completo de la clase (ej: "org.ravenEngine.components.HealthTest")
     */
    public static boolean reloadScript(File sourceFile, String fullClassName) {
        System.out.println("[HotReload] Compilando " + fullClassName + "...");

        ScriptCompiler.CompileResult result = ScriptCompiler.compile(sourceFile);
        if (!result.success) {
            System.err.println("[HotReload ERROR] Fallo la compilacion:");
            for (String err : result.errors) {
                System.err.println("   -> " + err);
            }
            return false;
        }

        System.out.println("[HotReload] Compilacion exitosa. Actualizando componentes en escena...");

        try {
            // Instanciar un loader nuevo para romper la cache de la JVM
            HotReloadClassLoader loader = new HotReloadClassLoader(ScriptHotReloader.class.getClassLoader());
            Class< ? > rawClass = loader.loadClass(fullClassName);

            if (!Component.class.isAssignableFrom(rawClass)) {
                System.err.println("[HotReload ERROR] " + fullClassName + " no hereda de Component.");
                return false;
            }

            @SuppressWarnings("unchecked")
            Class< ? extends Component > newComponentClass = (Class< ? extends Component >) rawClass;

            // =========================================================================
            // REGISTRO AUTOMÁTICO EN EL MOTOR (DISPONIBLE EN EL INSPECTOR)
            // =========================================================================
            ComponentRegistry.registerOrUpdateScript(newComponentClass);

            Scene activeScene = Engine.getCurrentScene();
            if (activeScene == null) return true;

            int swappedCount = 0;

            for (GameObject go : activeScene.getGameObjects()) {
                List< Component > comps = go.getAllComponents();
                for (int i = 0; i < comps.size(); i++) {
                    Component oldComp = comps.get(i);

                    // Si coincide por nombre de clase, hacemos el swap
                    if (oldComp.getClass().getName().equals(fullClassName)) {
                        // 1. Snapshot del estado interno del componente anterior
                        String jsonState = gson.toJson(oldComp);

                        // 2. Instanciar la versión nueva y restaurar campos
                        Component newComp = gson.fromJson(jsonState, newComponentClass);
                        newComp.gameObject = go;

                        // 3. Reemplazar la instancia vieja en el GameObject
                        comps.set(i, newComp);

                        // Si el juego esta en Play, inicializarlo
                        if (activeScene.isRunning()) {
                            newComp.invokeStart();
                        }

                        swappedCount++;
                    }
                }
            }

            System.out.println("[HotReload] Script actualizado con exito. Instancias reemplazadas: " + swappedCount);
            return true;

        } catch (Exception e) {
            System.err.println("[HotReload ERROR] Error durante el hot-swap: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}