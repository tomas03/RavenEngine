package org.ravenEngine.core;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;

public class ScriptCompiler {

    // Carpeta donde dejaremos las clases compiladas en tiempo de ejecución
    private static final String OUTPUT_DIR = "target/classes";

    public static boolean compile(File sourceFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("[Compilador ERR] No se encontró el compilador del JDK. Asegurate de correr con un JDK y no con un JRE.");
            return false;
        }

        // Configurar opciones de compilación
        String classpath = System.getProperty("java.class.path");
        String[] options = new String[] {
                "-d", OUTPUT_DIR,
                "-classpath", classpath,
                sourceFile.getAbsolutePath()
        };

        int result = compiler.run(null, null, null, options);

        if (result == 0) {
            System.out.println("[Compilador] Compilación exitosa: " + sourceFile.getName());
            return true;
        } else {
            System.err.println("[Compilador ERR] Falló la compilación de: " + sourceFile.getName());
            return false;
        }
    }

    /**
     * Carga o recarga la clase recién compilada mediante un ClassLoader dinámico
     */
    public static Class<?> loadClass(String fullyQualifiedName) {
        try {
            File classesDir = new File(OUTPUT_DIR);
            URL[] urls = new URL[] { classesDir.toURI().toURL() };

            // Usamos un ClassLoader hijo para poder cargar la versión fresca del bytecode
            URLClassLoader classLoader = new URLClassLoader(urls, ScriptCompiler.class.getClassLoader());
            return Class.forName(fullyQualifiedName, true, classLoader);
        } catch (Exception e) {
            System.err.println("[Compilador ERROR] No se pudo instanciar la clase: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}