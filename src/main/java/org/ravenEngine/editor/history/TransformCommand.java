package org.ravenEngine.editor.history;

import org.joml.Vector2f;
import org.ravenEngine.scene.GameObject;

public class TransformCommand implements EditorCommand {

    private final GameObject target;
    private final Vector2f oldPos;
    private final Vector2f newPos;
    private final Vector2f oldScale;
    private final Vector2f newScale;

    public TransformCommand(GameObject target, Vector2f oldPos, Vector2f newPos, Vector2f oldScale, Vector2f newScale) {
        this.target = target;
        this.oldPos = new Vector2f(oldPos);
        this.newPos = new Vector2f(newPos);
        this.oldScale = new Vector2f(oldScale);
        this.newScale = new Vector2f(newScale);
    }

    @Override
    public void execute() {
        if (target != null && target.transform != null) {
            target.transform.pos.set(newPos);
            target.transform.scale.set(newScale);
        }
    }

    @Override
    public void undo() {
        if (target != null && target.transform != null) {
            target.transform.pos.set(oldPos);
            target.transform.scale.set(oldScale);
        }
    }

    @Override
    public void redo() {
        execute();
    }
}