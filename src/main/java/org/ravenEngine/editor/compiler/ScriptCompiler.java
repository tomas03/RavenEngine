package org.ravenEngine.editor.compiler;

import javax.tools.*;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ScriptCompiler {
    private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    public static final String OUTPUT_DIR = "bin/runtime_scripts";

    public static class CompileResult {
        public final boolean success;
        public final List< String > errors;

        public CompileResult(boolean success, List< String > errors) {
            this.success = success;
            this.errors = errors;
        }
    }

    public static CompileResult compile(File sourceFile) {
        if (compiler == null) {
            List< String > err = new ArrayList<>();
            err.add("No se encontro el JDK. Asegurate de ejecutar el motor con un JDK y no con un JRE.");
            return new CompileResult(false, err);
        }

        // Asegurar que exista el directorio de salida
        File outDir = new File(OUTPUT_DIR);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        DiagnosticCollector< JavaFileObject > diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), StandardCharsets.UTF_8);

        // Classpath actual del motor + dependencias (LWJGL, JOML, ImGui, etc.)
        String classpath = System.getProperty("java.class.path");

        List< String > options = Arrays.asList(
                "-d", OUTPUT_DIR,
                "-classpath", classpath
        );

        Iterable< ? extends JavaFileObject > compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
        StringWriter outputWriter = new StringWriter();

        JavaCompiler.CompilationTask task = compiler.getTask(
                outputWriter,
                fileManager,
                diagnostics,
                options,
                null,
                compilationUnits
        );

        boolean success = task.call();
        List< String > errors = new ArrayList<>();

        if (!success) {
            for (Diagnostic< ? extends JavaFileObject > d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(String.format("Linea %d: %s", d.getLineNumber(), d.getMessage(Locale.getDefault())));
                }
            }
        }

        try {
            fileManager.close();
        } catch (Exception ignored) {}

        return new CompileResult(success, errors);
    }
}