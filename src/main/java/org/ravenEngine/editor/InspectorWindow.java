package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.ravenEngine.audio.AudioSource;
import org.ravenEngine.components.*;
import org.ravenEngine.renderer.SpriteRenderer;
import org.ravenEngine.scene.GameObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class InspectorWindow {

    // Buffers reutilizables para que el GC no sufra microtirones
    private final float[] f1Buffer = new float[1];
    private final float[] f2Buffer = new float[2];
    private final float[] f3Buffer = new float[3];
    private final float[] f4Buffer = new float[4];
    private final int[] intBuffer = new int[1];
    private final ImString nameBuffer = new ImString(64);
    private final ImString strBuffer = new ImString(256);

    public void update(GameObject selectedGameObject) {
        ImGui.begin("Inspector");

        if (selectedGameObject == null) {
            ImGui.textDisabled("Selecciona un objeto en la Jerarquia");
            ImGui.end();
            return;
        }

        // 1. Nombre del objeto editable
        nameBuffer.set(selectedGameObject.getName());
        if (ImGui.inputText("Nombre", nameBuffer)) {
            selectedGameObject.setName(nameBuffer.get());
        }

        ImGui.separator();

        // 2. COMPONENTE: TRANSFORM (Transform no se puede eliminar)
        Transform transform = selectedGameObject.transform;
        if (transform != null) {
            if (ImGui.collapsingHeader("Transform", imgui.flag.ImGuiTreeNodeFlags.DefaultOpen)) {
                f2Buffer[0] = transform.pos.x;
                f2Buffer[1] = transform.pos.y;
                if (ImGui.dragFloat2("Posicion", f2Buffer, 1.0f)) {
                    transform.pos.set(f2Buffer[0], f2Buffer[1]);
                }

                f2Buffer[0] = transform.scale.x;
                f2Buffer[1] = transform.scale.y;
                if (ImGui.dragFloat2("Escala", f2Buffer, 1.0f)) {
                    transform.scale.set(f2Buffer[0], f2Buffer[1]);
                }
            }
        }

        // Variable para diferir la eliminación y no romper los bucles
        Component componentToRemove = null;

        // 3. COMPONENTE: BOX COLLIDER 2D (Controles dedicados)
        BoxCollider2D collider = selectedGameObject.getComponent(BoxCollider2D.class);
        if (collider != null) {
            ImGui.pushID("BoxCollider2D_Header");
            boolean open = ImGui.collapsingHeader("BoxCollider2D", imgui.flag.ImGuiTreeNodeFlags.DefaultOpen);

            // Clic derecho en el encabezado para eliminar
            if (ImGui.beginPopupContextItem("ComponentContext_BoxCollider")) {
                if (ImGui.menuItem("Eliminar BoxCollider2D")) {
                    componentToRemove = collider;
                }
                ImGui.endPopup();
            }

            if (open) {
                f2Buffer[0] = collider.getSize().x;
                f2Buffer[1] = collider.getSize().y;
                if (ImGui.dragFloat2("Tamano Collider", f2Buffer, 1.0f)) {
                    collider.getSize().set(f2Buffer[0], f2Buffer[1]);
                }

                f2Buffer[0] = collider.getOffset().x;
                f2Buffer[1] = collider.getOffset().y;
                if (ImGui.dragFloat2("Offset Collider", f2Buffer, 1.0f)) {
                    collider.getOffset().set(f2Buffer[0], f2Buffer[1]);
                }
            }
            ImGui.popID();
        }

        // 4. INSPECTOR DINÁMICO POR REFLEXIÓN
        List<Component> components = selectedGameObject.getAllComponents();
        if (components != null) {
            for (int i = 0; i < components.size(); i++) {
                Component component = components.get(i);

                // Evitamos duplicar Transform y Collider ya procesados arriba
                if (component instanceof BoxCollider2D) {
                    continue;
                }

                String headerName = component.getClass().getSimpleName();

                ImGui.pushID(i);
                boolean headerOpen = ImGui.collapsingHeader(headerName, imgui.flag.ImGuiTreeNodeFlags.DefaultOpen);

                // Clic derecho en el encabezado del componente para eliminarlo
                if (ImGui.beginPopupContextItem("ComponentContext_" + i)) {
                    if (ImGui.menuItem("Eliminar " + headerName)) {
                        componentToRemove = component;
                    }
                    ImGui.endPopup();
                }

                if (headerOpen) {
                    renderComponentFields(component);
                }
                ImGui.popID();
            }
        }

        // 5. PROCESAR ELIMINACIÓN SEGURA
        if (componentToRemove != null) {
            removeComponent(selectedGameObject, componentToRemove);
        }

        ImGui.spacing();
        ImGui.separator();

        // 6. BOTÓN PARA AÑADIR COMPONENTES AL VUELO
        drawAddComponentButton(selectedGameObject);

        ImGui.end();
    }

    private void removeComponent(GameObject go, Component comp) {
        if (go == null || comp == null) return;

        try {
            // Intentar invocar go.removeComponent(Class) si está implementado
            java.lang.reflect.Method method = go.getClass().getMethod("removeComponent", Class.class);
            method.invoke(go, comp.getClass());
        } catch (Exception e) {
            try {
                // Fallback a removeComponent(Component)
                java.lang.reflect.Method method2 = go.getClass().getMethod("removeComponent", Component.class);
                method2.invoke(go, comp);
            } catch (Exception ex) {
                // Fallback directo a la lista de componentes
                List<Component> list = go.getAllComponents();
                if (list != null) {
                    list.remove(comp);
                }
            }
        }
        System.out.println("[Inspector] Componente eliminado: " + comp.getClass().getSimpleName() + " de " + go.getName());
    }

    private void renderComponentFields(Component component) {
        Field[] fields = component.getClass().getDeclaredFields();

        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) continue;
            if (field.getName().equals("gameObject")) continue;

            field.setAccessible(true);
            Class<?> type = field.getType();
            String label = field.getName();

            ImGui.pushID(label);

            try {
                // TIPO: boolean
                if (type == boolean.class) {
                    boolean val = field.getBoolean(component);
                    if (ImGui.checkbox(label, val)) {
                        field.setBoolean(component, !val);
                    }
                }
                // TIPO: int
                else if (type == int.class) {
                    intBuffer[0] = field.getInt(component);
                    if (ImGui.dragInt(label, intBuffer)) {
                        field.setInt(component, intBuffer[0]);
                    }
                }
                // TIPO: float
                else if (type == float.class) {
                    f1Buffer[0] = field.getFloat(component);
                    if (ImGui.dragFloat(label, f1Buffer, 0.05f)) {
                        field.setFloat(component, f1Buffer[0]);
                    }
                }
                // TIPO: String
                else if (type == String.class) {
                    String strVal = (String) field.get(component);
                    if (strVal == null) strVal = "";
                    strBuffer.set(strVal);
                    if (ImGui.inputText(label, strBuffer)) {
                        field.set(component, strBuffer.get());
                    }
                }
                // TIPO: Enum (Combobox desplegable para RigidBody2D BodyType, etc.)
                else if (type.isEnum()) {
                    Object enumVal = field.get(component);
                    Object[] enumConstants = type.getEnumConstants();
                    String currentEnumName = (enumVal != null) ? enumVal.toString() : "";

                    if (ImGui.beginCombo(label, currentEnumName)) {
                        for (Object constant : enumConstants) {
                            boolean isSelected = constant.toString().equals(currentEnumName);
                            if (ImGui.selectable(constant.toString(), isSelected)) {
                                field.set(component, constant);
                            }
                            if (isSelected) {
                                ImGui.setItemDefaultFocus();
                            }
                        }
                        ImGui.endCombo();
                    }
                }
                // TIPO: Vector2f
                else if (type == Vector2f.class) {
                    Vector2f vec = (Vector2f) field.get(component);
                    if (vec != null) {
                        f2Buffer[0] = vec.x;
                        f2Buffer[1] = vec.y;
                        if (ImGui.dragFloat2(label, f2Buffer, 0.5f)) {
                            vec.set(f2Buffer[0], f2Buffer[1]);
                        }
                    }
                }
                // TIPO: Vector3f
                else if (type == Vector3f.class) {
                    Vector3f vec = (Vector3f) field.get(component);
                    if (vec != null) {
                        f3Buffer[0] = vec.x;
                        f3Buffer[1] = vec.y;
                        f3Buffer[2] = vec.z;
                        if (ImGui.dragFloat3(label, f3Buffer, 0.5f)) {
                            vec.set(f3Buffer[0], f3Buffer[1], f3Buffer[2]);
                        }
                    }
                }
                // TIPO: Vector4f (Colores RGBA)
                else if (type == Vector4f.class) {
                    Vector4f vec = (Vector4f) field.get(component);
                    if (vec != null) {
                        f4Buffer[0] = vec.x;
                        f4Buffer[1] = vec.y;
                        f4Buffer[2] = vec.z;
                        f4Buffer[3] = vec.w;
                        if (ImGui.colorEdit4(label, f4Buffer)) {
                            vec.set(f4Buffer[0], f4Buffer[1], f4Buffer[2], f4Buffer[3]);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                System.err.println("[Inspector ERROR] Falló acceso a: " + field.getName());
            }

            ImGui.popID();
        }
    }

    private void drawAddComponentButton(GameObject go) {
        if (ImGui.button("Agregar Componente...", -1, 28)) {
            ImGui.openPopup("AddComponentPopup");
        }

        if (ImGui.beginPopup("AddComponentPopup")) {

            // ==========================================
            // 1. RENDER & VISUALES
            // ==========================================
            if (ImGui.beginMenu("Rendering")) {
                if (go.getComponent(org.ravenEngine.renderer.SpriteRenderer.class) == null) {
                    if (ImGui.menuItem("SpriteRenderer")) {
                        go.addComponent(new org.ravenEngine.renderer.SpriteRenderer());
                        ImGui.closeCurrentPopup();
                    }
                }

                if (go.getComponent(TextRenderer.class) == null) {
                    if (ImGui.menuItem("TextRenderer")) {
                        go.addComponent(new TextRenderer());
                        ImGui.closeCurrentPopup();
                    }
                }

                if (go.getComponent(SpriteAnimator.class) == null) {
                    if (ImGui.menuItem("SpriteAnimator")) {
                        go.addComponent(new SpriteAnimator());
                        ImGui.closeCurrentPopup();
                    }
                }

                if (go.getComponent(ParticleSystem.class) == null) {
                    if (ImGui.menuItem("ParticleSystem")) {
                        go.addComponent(new ParticleSystem());
                        ImGui.closeCurrentPopup();
                    }
                }

                if (go.getComponent(Tilemap.class) == null) {
                    if (ImGui.menuItem("Tilemap")) {
                        go.addComponent(new Tilemap());
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endMenu();
            }

            // ==========================================
            // 2. FÍSICA & COLISIONES (2D)
            // ==========================================
            if (ImGui.beginMenu("Physics 2D")) {
                if (go.getComponent(BoxCollider2D.class) == null) {
                    if (ImGui.menuItem("BoxCollider2D")) {
                        go.addComponent(new BoxCollider2D());
                        ImGui.closeCurrentPopup();
                    }
                }

                if (go.getComponent(RigidBody2D.class) == null) {
                    if (ImGui.menuItem("RigidBody2D")) {
                        go.addComponent(new RigidBody2D());
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endMenu();
            }

            // ==========================================
            // 3. AUDIO
            // ==========================================
            if (ImGui.beginMenu("Audio")) {
                if (go.getComponent(org.ravenEngine.audio.AudioSource.class) == null) {
                    if (ImGui.menuItem("AudioSource")) {
                        go.addComponent(new org.ravenEngine.audio.AudioSource());
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endMenu();
            }

            // ==========================================
            // 4. GAMEPLAY & RPG
            // ==========================================
            if (ImGui.beginMenu("Gameplay")) {
                if (go.getComponent(DialogueInteractable.class) == null) {
                    if (ImGui.menuItem("DialogueInteractable (NPC / Cartel)")) {
                        go.addComponent(new DialogueInteractable());
                        ImGui.closeCurrentPopup();
                    }
                }

                if (go.getComponent(Inventory.class) == null) {
                    if (ImGui.menuItem("Inventory (Mochila / Cofre)")) {
                        go.addComponent(new Inventory());
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endMenu();
            }

            // ==========================================
            // 5. CÁMARA & UTILIDAD
            // ==========================================
            if (ImGui.beginMenu("Camera / Utility")) {
                if (go.getComponent(CameraFollow.class) == null) {
                    if (ImGui.menuItem("CameraFollow")) {
                        go.addComponent(new CameraFollow());
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endMenu();
            }

            ImGui.endPopup();
        }
    }
}