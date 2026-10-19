package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.ravenEngine.components.BoxCollider2D;
import org.ravenEngine.components.Component;
import org.ravenEngine.components.Transform;
import org.ravenEngine.scene.GameObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class InspectorWindow {

    public void update(GameObject selectedGameObject) {
        ImGui.begin("Inspector");

        if (selectedGameObject == null) {
            ImGui.textDisabled("Selecciona un objeto en la Jerarquía");
            ImGui.end();
            return;
        }

        // Nombre del objeto editable
        ImString nameBuffer = new ImString(selectedGameObject.getName(), 64);
        if (ImGui.inputText("Nombre", nameBuffer)) {
            selectedGameObject.setName(nameBuffer.get());
        }

        ImGui.separator();

        // 1. COMPONENTE: TRANSFORM
        Transform transform = selectedGameObject.transform;
        if (transform != null) {
            if (ImGui.collapsingHeader("Transform")) {
                float[] pos = new float[]{ transform.pos.x, transform.pos.y };
                if (ImGui.dragFloat2("Posición", pos, 1.0f)) {
                    transform.pos.set(pos[0], pos[1]);
                }

                float[] scale = new float[]{ transform.scale.x, transform.scale.y };
                if (ImGui.dragFloat2("Escala", scale, 1.0f)) {
                    transform.scale.set(scale[0], scale[1]);
                }
            }
        }

        // 2. COMPONENTE: BOX COLLIDER 2D (Tus controles dedicados)
        BoxCollider2D collider = selectedGameObject.getComponent(BoxCollider2D.class);
        if (collider != null) {
            if (ImGui.collapsingHeader("BoxCollider2D")) {
                float[] colSize = new float[]{ collider.getSize().x, collider.getSize().y };
                if (ImGui.dragFloat2("Tamaño Collider", colSize, 1.0f)) {
                    collider.getSize().set(colSize[0], colSize[1]);
                }

                float[] colOffset = new float[]{ collider.getOffset().x, collider.getOffset().y };
                if (ImGui.dragFloat2("Offset", colOffset, 1.0f)) {
                    collider.getOffset().set(colOffset[0], colOffset[1]);
                }
            }
        }

        // 3. INSPECTOR DINÁMICO PARA EL RESTO DE COMPONENTES
        List< Component > components = selectedGameObject.getAllComponents();
        if (components != null) {
            for (Component component : components) {
                // Ya procesamos Transform y BoxCollider2D arriba
                if (component instanceof BoxCollider2D) {
                    continue;
                }

                String headerName = component.getClass().getSimpleName();
                if (ImGui.collapsingHeader(headerName)) {
                    renderComponentFields(component);
                }
            }
        }

        ImGui.end();
    }

    // ==========================================
    // REFLEXIÓN AUTOMÁTICA DE CAMPOS
    // ==========================================
    private void renderComponentFields(Component component) {
        Field[] fields = component.getClass().getDeclaredFields();

        for (Field field : fields) {
            int modifiers = field.getModifiers();
            // Ignoramos constantes estáticas, transient y referencias al GameObject padre
            if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) continue;
            if (field.getName().equals("gameObject")) continue;

            field.setAccessible(true);
            Class< ? > type = field.getType();
            String label = field.getName();

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
                    int[] val = { field.getInt(component) };
                    if (ImGui.dragInt(label, val)) {
                        field.setInt(component, val[0]);
                    }
                }
                // TIPO: float
                else if (type == float.class) {
                    float[] val = { field.getFloat(component) };
                    if (ImGui.dragFloat(label, val, 0.1f)) {
                        field.setFloat(component, val[0]);
                    }
                }
                // TIPO: String
                else if (type == String.class) {
                    String strVal = (String) field.get(component);
                    if (strVal == null) strVal = "";
                    ImString buf = new ImString(strVal, 128);
                    if (ImGui.inputText(label, buf)) {
                        field.set(component, buf.get());
                    }
                }
                // TIPO: Vector2f
                else if (type == Vector2f.class) {
                    Vector2f vec = (Vector2f) field.get(component);
                    if (vec != null) {
                        float[] val = { vec.x, vec.y };
                        if (ImGui.dragFloat2(label, val, 0.1f)) {
                            vec.set(val[0], val[1]);
                        }
                    }
                }
                // TIPO: Vector3f
                else if (type == Vector3f.class) {
                    Vector3f vec = (Vector3f) field.get(component);
                    if (vec != null) {
                        float[] val = { vec.x, vec.y, vec.z };
                        if (ImGui.dragFloat3(label, val, 0.1f)) {
                            vec.set(val[0], val[1], val[2]);
                        }
                    }
                }
                // TIPO: Vector4f (Colores RGBA)
                else if (type == Vector4f.class) {
                    Vector4f vec = (Vector4f) field.get(component);
                    if (vec != null) {
                        float[] val = { vec.x, vec.y, vec.z, vec.w };
                        if (ImGui.colorEdit4(label, val)) {
                            vec.set(val[0], val[1], val[2], val[3]);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                System.err.println("[Inspector ERROR] No se pudo acceder al campo: " + field.getName());
            }
        }
    }
}