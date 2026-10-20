package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;

public class ConsolePanel {

    private boolean autoScroll = true;

    public void update() {
        ImGui.begin("Terminal RENGINE");

        if (ImGui.button("Limpiar")) {
            EngineConsole.clear();
        }
        ImGui.sameLine();
        if (ImGui.checkbox("Auto-Scroll", autoScroll)) {
            autoScroll = !autoScroll;
        }
        ImGui.separator();

        ImGui.beginChild("ConsoleScrollRegion", 0, 0, false, ImGuiWindowFlags.HorizontalScrollbar);
        synchronized (EngineConsole.getLogs()) {
            for (String log : EngineConsole.getLogs()) {
                if (log.startsWith("[ERROR]")) {
                    ImGui.textColored(1.0f, 0.4f, 0.4f, 1.0f, log);
                } else if (log.startsWith("[Inventario]")) {
                    ImGui.textColored(0.4f, 1.0f, 0.4f, 1.0f, log);
                } else if (log.startsWith("[Scene]")) {
                    ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, log);
                } else {
                    ImGui.textUnformatted(log);
                }
            }
        }

        if (autoScroll && ImGui.getScrollY() >= ImGui.getScrollMaxY()) {
            ImGui.setScrollHereY(1.0f);
        }

        ImGui.endChild();
        ImGui.end();
    }
}