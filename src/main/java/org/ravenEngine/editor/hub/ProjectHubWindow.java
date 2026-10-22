package org.ravenEngine.editor.hub;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;

import java.io.File;
import java.util.List;

public class ProjectHubWindow {

    private final List<ProjectData> projects;
    private ProjectData selectedProject = null;
    private boolean showNewProjectModal = false;

    private final ImString newProjectName = new ImString("MiNuevoJuego", 64);
    private final ImString newProjectPath = new ImString(System.getProperty("user.home") + "/RavenProjects/", 256);

    // Callback para arrancar el IDE una vez seleccionado el proyecto
    public interface ProjectSelectedCallback {
        void onProjectSelected(ProjectData project);
    }
    private final ProjectSelectedCallback callback;

    public ProjectHubWindow(ProjectSelectedCallback callback) {
        this.callback = callback;
        this.projects = ProjectHubManager.loadProjects();
    }

    public void update() {
        ImVec2 center = ImGui.getMainViewport().getCenter();
        ImGui.setNextWindowPos(center.x, center.y, 0, 0.5f, 0.5f);
        ImGui.setNextWindowSize(720.0f, 480.0f);

        int flags = ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse;
        ImGui.begin("RavenEngine - Gestor de Proyectos", flags);

        // Barra superior
        ImGui.textColored(ImColor.rgb(100, 180, 255), "Proyectos Registrados");
        ImGui.sameLine(ImGui.getWindowWidth() - 250);

        if (ImGui.button("+ Nuevo Proyecto", 110, 28)) {
            showNewProjectModal = true;
        }
        ImGui.sameLine();
        if (ImGui.button("Abrir Carpeta", 110, 28)) {
            try {
                // Invoca el selector de carpetas nativo de Linux (KDE/GNOME vía zenity o kdialog)
                Process process = new ProcessBuilder("zenity", "--file-selection", "--directory", "--title=Seleccionar Proyecto RavenEngine").start();
                String path = new String(process.getInputStream().readAllBytes()).trim();
                if (!path.isEmpty()) {
                    java.io.File folder = new java.io.File(path);
                    ProjectHubManager.registerProject(folder.getName(), folder.getAbsolutePath());
                    this.projects.clear();
                    this.projects.addAll(ProjectHubManager.loadProjects());
                }
            } catch (Exception e) {
                System.err.println("[Hub] No se pudo abrir el selector del sistema: " + e.getMessage());
            }
        }

        ImGui.separator();

        // Lista de proyectos estilo Godot
        ImGui.beginChild("##ProjectsList", 0, -50, true);
        for (int i = 0; i < projects.size(); i++) {
            ProjectData p = projects.get(i);
            ImGui.pushID(i);

            boolean isSelected = (selectedProject == p);
            if (isSelected) {
                ImGui.pushStyleColor(ImGuiCol.Header, ImColor.rgb(55, 65, 80));
            }

            ImGui.textColored(ImColor.rgb(255, 255, 255), p.name);
            ImGui.textColored(ImColor.rgb(130, 130, 130), p.path);

            ImGui.sameLine(ImGui.getWindowWidth() - 140);
            if (ImGui.button("Editar", 60, 24)) {
                callback.onProjectSelected(p);
            }
            ImGui.sameLine();
            if (ImGui.button("x", 24, 24)) {
                projects.remove(i);
                ProjectHubManager.saveProjects(projects);
                ImGui.popID();
                break;
            }

            if (isSelected) ImGui.popStyleColor();
            ImGui.separator();
            ImGui.popID();
        }
        ImGui.endChild();

        // Footer
        if (selectedProject != null) {
            if (ImGui.button("Abrir Proyecto Seleccionado", 220, 32)) {
                callback.onProjectSelected(selectedProject);
            }
        }

        renderNewProjectModal();

        ImGui.end();
    }

    private void renderNewProjectModal() {
        if (showNewProjectModal) {
            ImGui.openPopup("Crear Nuevo Proyecto##Modal");
        }

        if (ImGui.beginPopupModal("Crear Nuevo Proyecto##Modal", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Nombre del Proyecto:");
            ImGui.inputText("##pName", newProjectName);

            ImGui.text("Ruta de Creación:");
            ImGui.inputText("##pPath", newProjectPath);

            String fullDir = newProjectPath.get().trim() + "/" + newProjectName.get().trim();
            ImGui.textDisabled("Se creará en: " + fullDir);

            ImGui.separator();

            if (ImGui.button("Crear y Editar", 140, 0)) {
                String name = newProjectName.get().trim();
                ProjectHubManager.createProjectStructure(fullDir, name);
                ProjectHubManager.registerProject(name, fullDir);

                showNewProjectModal = false;
                ImGui.closeCurrentPopup();

                // Abrir directamente en el motor
                callback.onProjectSelected(new ProjectData(name, fullDir));
            }

            ImGui.sameLine();
            if (ImGui.button("Cancelar", 100, 0)) {
                showNewProjectModal = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }
}