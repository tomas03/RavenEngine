package org.ravenEngine.editor.hub;

public class ProjectData {
    public String name;
    public String path;
    public String lastScene;
    public long lastOpened;

    public ProjectData(String name, String path) {
        this.name = name;
        this.path = path;
        this.lastScene = "assets/scenes/main.json";
        this.lastOpened = System.currentTimeMillis();
    }
}