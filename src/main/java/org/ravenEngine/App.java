package org.ravenEngine;

import org.ravenEngine.core.Engine;
import org.ravenEngine.core.EngineMode;
import org.ravenEngine.editor.EngineConsole;
import org.ravenEngine.scene.Scene;

public class App {
    public static String initialProjectPath = null;

    public static void main(String[] args) {
        boolean isRelease = false;

        // Leer argumentos pasados por consola
        for (String arg : args) {
            if ("--release".equalsIgnoreCase(arg)) {
                isRelease = true;
            } else if (!arg.startsWith("-")) {
                initialProjectPath = arg;
            }
        }

        // Activamos el modo runtime si vino el flag
        if (isRelease) {
            Engine.setMode(EngineMode.RUNTIME);
        }

        // El constructor sin parámetros de Engine ya lee el título y resolución de ProjectSettings
        Engine engine = new Engine();
        EngineConsole.init();

        engine.start(new Scene());
    }
}