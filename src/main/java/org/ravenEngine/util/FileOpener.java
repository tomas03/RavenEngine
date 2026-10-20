package org.ravenEngine.util;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class FileOpener {
    //cambiar "xdg-open" por "code", "kate", "zed", etc., si se quiere forzar un editor puntual
    private static final String DEFAULT_EDITOR = "code";

    public static void openInEditor(File file){
        if (file == null || !file.exists()){
            System.out.println("[FileOpener ERR] El archivo no existe: "+(file != null ? file.getPath():"null"));
            return;
        }

        new Thread(()->{
            try{
                //Ejecutar en segundo plano para no congelar el loop del engine
                ProcessBuilder pb = new ProcessBuilder(DEFAULT_EDITOR, file.getAbsolutePath());
                pb.start();
                System.out.println("[FileOpener] Abriendo: "+file.getName()+" con "+DEFAULT_EDITOR);
            } catch (IOException e) {
                System.err.println("[FileOpener ERR] Falló al intentar abrir el archivo: "+e.getMessage());
            }
        }).start();
    }

    public static boolean isEditable(String fileName){
        String lower = fileName.toLowerCase();
        return lower.endsWith(".java")||
                lower.endsWith(".json")||
                lower.endsWith(".glsl")||
                lower.endsWith(".vert")||
                lower.endsWith(".frag")||
                lower.endsWith(".txt")||
                lower.endsWith(".md")||
                lower.endsWith(".xml");
    }
}
