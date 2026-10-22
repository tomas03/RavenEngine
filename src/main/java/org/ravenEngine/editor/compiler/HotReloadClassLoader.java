package org.ravenEngine.editor.compiler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class HotReloadClassLoader extends URLClassLoader {

    public HotReloadClassLoader(ClassLoader parent) throws MalformedURLException {
        super(new URL[]{ new File(ScriptCompiler.OUTPUT_DIR).toURI().toURL() }, parent);
    }

    /**
     * Fuerza la lectura de la clase recompilada desde el disco antes de delegar en el padre.
     */
    @Override
    protected Class< ? > loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            Class< ? > c = findLoadedClass(name);
            if (c == null) {
                c = findClass(name); // Carga la versión local recompilada
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        } catch (ClassNotFoundException e) {
            // Clases del motor y dependencias estándar se delegan al ClassLoader padre
            return super.loadClass(name, resolve);
        }
    }
}