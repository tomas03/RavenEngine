package org.ravenEngine.editor.history;

import org.ravenEngine.scene.GameObject;
import org.ravenEngine.scene.Scene;

public class DeleteGameObjectCommand implements EditorCommand {

    private final Scene scene;
    private final GameObject target;

    public DeleteGameObjectCommand(Scene scene, GameObject target) {
        this.scene = scene;
        this.target = target;
    }

    @Override
    public void execute() {
        if (scene != null && target != null) {
            scene.destroyGameObject(target);
        }
    }

    @Override
    public void undo() {
        if (scene != null && target != null) {
            scene.addGameObjectToScene(target);
        }
    }

    @Override
    public void redo() {
        execute();
    }
}