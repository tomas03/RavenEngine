package org.ravenEngine.editor.history;

import org.ravenEngine.scene.GameObject;

public class RotateCommand implements EditorCommand {
    private final GameObject gameObject;
    private final float oldRotation;
    private final float newRotation;

    public RotateCommand(GameObject gameObject, float oldRotation, float newRotation) {
        this.gameObject = gameObject;
        this.oldRotation = oldRotation;
        this.newRotation = newRotation;
    }

    @Override
    public void execute() {
        this.gameObject.transform.rotation = this.newRotation;
    }

    @Override
    public void undo() {
        this.gameObject.transform.rotation = this.oldRotation;
    }

    @Override
    public void redo() {
        this.execute();
    }
}