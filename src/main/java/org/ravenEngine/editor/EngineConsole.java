package org.ravenEngine.editor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EngineConsole {
    private static final int MAX_LOGS = 500;
    private static final List< String > logs = Collections.synchronizedList(new ArrayList< String >());
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        // Interceptamos System.out
        System.setOut(new PrintStream(new DualOutputStream(originalOut, false)));
        // Interceptamos System.err
        System.setErr(new PrintStream(new DualOutputStream(originalErr, true)));
    }

    public static void log(String message) {
        if (logs.size() >= MAX_LOGS) {
            logs.remove(0);
        }
        logs.add(message);
    }

    public static List< String > getLogs() {
        return logs;
    }

    public static void clear() {
        logs.clear();
    }

    // Stream auxiliar que escribe en la terminal nativa y en nuestra lista
    private static class DualOutputStream extends OutputStream {
        private final PrintStream targetStream;
        private final boolean isError;
        private final ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        public DualOutputStream(PrintStream targetStream, boolean isError) {
            this.targetStream = targetStream;
            this.isError = isError;
        }

        @Override
        public synchronized void write(int b) {
            targetStream.write(b);
            if (b == '\n') {
                flushLine();
            } else if (b != '\r') {
                byteBuffer.write(b);
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            targetStream.write(b, off, len);
            for (int i = 0; i < len; i++) {
                int val = b[off + i];
                if (val == '\n') {
                    flushLine();
                } else if (val != '\r') {
                    byteBuffer.write(val);
                }
            }
        }

        private void flushLine() {
            String line = byteBuffer.toString(StandardCharsets.UTF_8);
            if (isError) {
                EngineConsole.log("[ERR] " + line);
            } else {
                EngineConsole.log(line);
            }
            byteBuffer.reset();
        }
    }
}