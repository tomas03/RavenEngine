package org.ravenEngine.editor.history;

import java.util.ArrayDeque;
import java.util.Deque;

public class CommandHistory {
    private static final int MAX_HISTORY_SIZE = 64;
    private static final Deque< EditorCommand > undoStack = new ArrayDeque< >();
    private static final Deque< EditorCommand > redoStack = new ArrayDeque< >();

    public static void executeCommand(EditorCommand command) {
        if (command == null) return;

        command.execute();

        if (undoStack.size() >= MAX_HISTORY_SIZE) {
            undoStack.removeLast(); // Descartamos la acción más vieja
        }
        undoStack.push(command);
        redoStack.clear(); // Cualquier acción nueva invalida el rehacer
    }

    public static void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("[Historial] Nada para deshacer.");
            return;
        }

        EditorCommand command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        System.out.println("[Historial] Deshacer ejecutado: " + command.getClass().getSimpleName());
    }
    public static void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("[Historial] Nada para rehacer.");
            return;
        }

        EditorCommand command = redoStack.pop();
        command.redo();
        undoStack.push(command);
        System.out.println("[Historial] Rehacer ejecutado: " + command.getClass().getSimpleName());
    }
    public static void clear() {
        undoStack.clear();
        redoStack.clear();
    }
    public static boolean canUndo() {
        return !undoStack.isEmpty();
    }
    public static boolean canRedo() {
        return !redoStack.isEmpty();
    }
}
