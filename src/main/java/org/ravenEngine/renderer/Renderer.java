package org.ravenEngine.renderer;

import org.ravenEngine.scene.GameObject;

import java.util.ArrayList;
import java.util.List;

public class Renderer {
    private static final int MAX_BATCH_SIZE = 1000;
    private final List<RenderBatch> batches;
    private final Shader defaultShader;

    public Renderer() {
        this.batches = new ArrayList<>();
        this.defaultShader = new Shader("/Shaders/default.vert","/Shaders/default.frag");
        this.defaultShader.compile();
    }

    public void add(GameObject go){
        SpriteRenderer spr = go.getComponent(SpriteRenderer.class);
        if (spr != null){
            add(spr);
        }
    }
    private void add(SpriteRenderer spr) {
        boolean added = false;
        for (RenderBatch batch : batches) {
            if (batch.hasRoom()) {
                Texture tex = spr.getTexture();
                if (tex == null || (batch.hasTexture(tex) || batch.hasTextureRoom())) {
                    batch.addSprite(spr);
                    added = true;
                    break;
                }
            }
        }

        if (!added) {
            RenderBatch newBatch = new RenderBatch(MAX_BATCH_SIZE, defaultShader);
            newBatch.start();
            batches.add(newBatch);
            newBatch.addSprite(spr);
        }
    }

    public void clear() {
        this.batches.clear();
    }

    public void render(Camera camera){
        for (RenderBatch batch : batches){
            batch.render(camera);
        }
    }
}
