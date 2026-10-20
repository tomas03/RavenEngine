package org.ravenEngine.editor.history;

public interface EditorCommand {
    void execute();
    void undo();
    void redo();
}