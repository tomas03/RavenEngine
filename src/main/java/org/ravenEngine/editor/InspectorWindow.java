package org.ravenEngine.editor;

import imgui.ImGui;
import imgui.type.ImString;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.ravenEngine.components.*;
import org.ravenEngine.renderer.texture.SpriteRenderer;
import org.ravenEngine.renderer.animation.Animation;
import org.ravenEngine.renderer.texture.Sprite;
import org.ravenEngine.scene.GameObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

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

        // 2. COMPONENTE: TRANSFORM
        Transform transform = selectedGameObject.transform;
        if (transform != null) {
            if (ImGui.collapsingHeader("Transform", imgui.flag.ImGuiTreeNodeFlags.DefaultOpen)) {
                // Posición
                f2Buffer[0] = transform.pos.x;
                f2Buffer[1] = transform.pos.y;
                if (ImGui.dragFloat2("Posición", f2Buffer, 1.0f)) {
                    transform.pos.set(f2Buffer[0], f2Buffer[1]);
                }

                // Escala
                f2Buffer[0] = transform.scale.x;
                f2Buffer[1] = transform.scale.y;
                if (ImGui.dragFloat2("Escala", f2Buffer, 1.0f)) {
                    transform.scale.set(Math.max(1.0f, f2Buffer[0]), Math.max(1.0f, f2Buffer[1]));
                }

                // Rotación (0° a 360°)
                float[] rotBuffer = { transform.rotation };
                if (ImGui.dragFloat("Rotación", rotBuffer, 1.0f, 0.0f, 360.0f, "%.1f°")) {
                    transform.rotation = (rotBuffer[0] % 360.0f + 360.0f) % 360.0f;
                }

                // Z-Index
                int[] zBuffer = { transform.zIndex };
                if (ImGui.dragInt("Z-Index", zBuffer, 1.0f, -100, 100)) {
                    transform.zIndex = zBuffer[0];
                }
            }
        }

        // Variable para diferir la eliminación y no romper los bucles
        Component componentToRemove = null;

        // 3. COMPONENTE: BOX COLLIDER 2D (Controles dedicados)
        BoxCollider2D collider = selectedGameObject.getComponent(BoxCollider2D.class);
        if (collider != null) {
            ImGui.separator();
            if (ImGui.collapsingHeader("BoxCollider2D", imgui.flag.ImGuiTreeNodeFlags.DefaultOpen)) {
                // Offset
                f2Buffer[0] = collider.getOffset().x;
                f2Buffer[1] = collider.getOffset().y;
                if (ImGui.dragFloat2("Offset", f2Buffer, 1.0f)) {
                    collider.getOffset().set(f2Buffer[0], f2Buffer[1]);
                }

                // Tamaño
                f2Buffer[0] = collider.getSize().x;
                f2Buffer[1] = collider.getSize().y;
                if (ImGui.dragFloat2("Tamaño", f2Buffer, 1.0f)) {
                    collider.getSize().set(Math.max(1.0f, f2Buffer[0]), Math.max(1.0f, f2Buffer[1]));
                }

                // Rotación
                float[] colRotBuffer = new float[]{ collider.getRotation() };
                if (ImGui.dragFloat("Rotación Col", colRotBuffer, 1.0f, -360.0f, 360.0f, "%.1f°")) {
                    collider.setRotation(colRotBuffer[0]);
                }

                ImGui.spacing();

                // Checkbox Estático
                if (ImGui.checkbox("Estático", collider.isStatic())) {
                    collider.setStatic(!collider.isStatic());
                }
                ImGui.sameLine();
                ImGui.textDisabled("(Inmóvil)");

                // Checkbox Trigger
                if (ImGui.checkbox("Trigger", collider.isTrigger())) {
                    collider.setTrigger(!collider.isTrigger());
                }
                ImGui.sameLine();
                ImGui.textDisabled("(Zona de evento)");
            }
        }

        // 3.5. COMPONENTE: SPRITE RENDERER (Slot dedicado con Drag & Drop)
        SpriteRenderer spr = selectedGameObject.getComponent(SpriteRenderer.class);
        if (spr != null) {
            ImGui.separator();
            if (ImGui.collapsingHeader("SpriteRenderer", imgui.flag.ImGuiTreeNodeFlags.DefaultOpen)) {
                String currentTexturePath = "Sin textura (Arrastra una aquí)";
                if (spr.getSprite() != null && spr.getSprite().getTexture() != null) {
                    currentTexturePath = spr.getSprite().getTexture().getFilepath();
                }

                ImGui.text("Textura:");
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0.2f, 0.22f, 0.27f, 1.0f);
                ImGui.button(currentTexturePath, -1, 32);
                ImGui.popStyleColor();

                if (ImGui.beginDragDropTarget()) {
                    Object payload = ImGui.acceptDragDropPayload("CONTENT_BROWSER_ITEM");
                    if (payload != null) {
                        String path = (payload instanceof byte[]) ? new String((byte[]) payload) : payload.toString();
                        if (path.endsWith(".png") || path.endsWith(".jpg")) {
                            var texture = org.ravenEngine.util.AssetPool.getTexture(path);
                            if (texture != null) {
                                spr.setSprite(new Sprite(texture));
                                System.out.println("[Inspector] Textura cambiada a: " + path);
                            }
                        }
                    }
                    ImGui.endDragDropTarget();
                }

                // Color Tint
                Vector4f color = spr.getColor();
                if (color != null) {
                    f4Buffer[0] = color.x;
                    f4Buffer[1] = color.y;
                    f4Buffer[2] = color.z;
                    f4Buffer[3] = color.w;
                    if (ImGui.colorEdit4("Color Tint", f4Buffer)) {
                        color.set(f4Buffer[0], f4Buffer[1], f4Buffer[2], f4Buffer[3]);
                    }
                }
            }
        }

        // 3.6. COMPONENTE: SPRITE ANIMATOR (Estilo Godot SpriteFrames)
        SpriteAnimator animator = selectedGameObject.getComponent(SpriteAnimator.class);
        if (animator != null) {
            ImGui.separator();
            if (ImGui.collapsingHeader("SpriteAnimator (SpriteFrames)", imgui.flag.ImGuiTreeNodeFlags.DefaultOpen)) {

                // Flips
                if (ImGui.checkbox("Flip H", animator.isFlipH())) {
                    animator.setFlipH(!animator.isFlipH());
                }
                ImGui.sameLine();
                if (ImGui.checkbox("Flip V", animator.isFlipV())) {
                    animator.setFlipV(!animator.isFlipV());
                }

                // Selector de animación
                Map<String, Animation> anims = animator.getAnimations();
                String currentTitle = animator.getCurrentAnimation() != null ? animator.getCurrentAnimation().getTitle() : "Ninguna";

                if (ImGui.beginCombo("Animación", currentTitle)) {
                    for (String title : anims.keySet()) {
                        boolean isSelected = title.equals(currentTitle);
                        if (ImGui.selectable(title, isSelected)) {
                            animator.play(title);
                        }
                        if (isSelected) ImGui.setItemDefaultFocus();
                    }
                    ImGui.endCombo();
                }

                Animation currentAnim = animator.getCurrentAnimation();
                if (currentAnim != null) {
                    // FPS del clip
                    float[] fpsBuf = { currentAnim.getApproximateFPS() };
                    if (ImGui.dragFloat("FPS", fpsBuf, 0.5f, 1.0f, 60.0f, "%.1f FPS")) {
                        currentAnim.setFPS(fpsBuf[0]);
                    }

                    // Loop toggle
                    if (ImGui.checkbox("Loop", currentAnim.isDoesLoop())) {
                        currentAnim.setDoesLoop(!currentAnim.isDoesLoop());
                    }

                    // Multiplicador general de velocidad
                    float[] speedBuf = { animator.getSpeed() };
                    if (ImGui.dragFloat("Velocidad", speedBuf, 0.05f, 0.0f, 5.0f, "%.2fx")) {
                        animator.setSpeed(speedBuf[0]);
                    }

                    ImGui.spacing();

                    // Barra de Transporte: Play / Pause / Stop
                    if (animator.isPlaying()) {
                        if (ImGui.button("Pause", 75, 24)) animator.pause();
                    } else {
                        if (ImGui.button("Play", 75, 24)) animator.resume();
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Stop", 75, 24)) animator.stop();

                    // Scrubber interactivo de Frames
                    int totalFrames = currentAnim.getFrames().size();
                    if (totalFrames > 0) {
                        int[] frameIndex = { currentAnim.getCurrentFrameIndex() };
                        if (ImGui.sliderInt("Frame", frameIndex, 0, totalFrames - 1, "%d / " + (totalFrames - 1))) {
                            animator.pause();
                            currentAnim.setCurrentFrameIndex(frameIndex[0]);
                            animator.applyCurrentFrameToRenderer();
                        }
                    } else {
                        ImGui.textDisabled("Animación sin frames.");
                    }
                } else {
                    ImGui.textDisabled("No hay animaciones cargadas.");
                }
            }
        }

        // 4. INSPECTOR DINÁMICO POR REFLEXIÓN
        List<Component> components = selectedGameObject.getAllComponents();
        if (components != null) {
            for (int i = 0; i < components.size(); i++) {
                Component component = components.get(i);

                // Evitamos duplicar Transform, Collider, SpriteRenderer y SpriteAnimator
                if (component instanceof BoxCollider2D ||
                        component instanceof SpriteRenderer ||
                        component instanceof SpriteAnimator) {
                    continue;
                }

                String headerName = component.getClass().getSimpleName();

                ImGui.pushID(i);
                boolean headerOpen = ImGui.collapsingHeader(headerName, imgui.flag.ImGuiTreeNodeFlags.DefaultOpen);

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
            java.lang.reflect.Method method = go.getClass().getMethod("removeComponent", Class.class);
            method.invoke(go, comp.getClass());
        } catch (Exception e) {
            try {
                java.lang.reflect.Method method2 = go.getClass().getMethod("removeComponent", Component.class);
                method2.invoke(go, comp);
            } catch (Exception ex) {
                List<Component> list = go.getAllComponents();
                if (list != null) {
                    list.remove(comp);
                }
            }
        }
        System.out.println("[Inspector] Componente eliminado: " + comp.getClass().getSimpleName() + " de " + go.getName());
    }

    private void renderComponentFields(Component component) {
        Class< ? > currentClass = component.getClass();

        // Recorre la clase actual y sus clases padre (por ejemplo UIProgressBar -> UIElement)
        while (currentClass != null && currentClass != Component.class) {
            Field[] fields = currentClass.getDeclaredFields();

            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers)) continue;
                if (field.getName().equals("gameObject")) continue;

                field.setAccessible(true);
                Class< ? > type = field.getType();
                String label = field.getName();

                ImGui.pushID(currentClass.getSimpleName() + "_" + label);

                try {
                    if (type == boolean.class) {
                        boolean val = field.getBoolean(component);
                        if (ImGui.checkbox(label, val)) {
                            field.setBoolean(component, !val);
                        }
                    } else if (type == int.class) {
                        intBuffer[0] = field.getInt(component);
                        if (ImGui.dragInt(label, intBuffer)) {
                            field.setInt(component, intBuffer[0]);
                        }
                    } else if (type == float.class) {
                        f1Buffer[0] = field.getFloat(component);
                        if (ImGui.dragFloat(label, f1Buffer, 0.05f)) {
                            field.setFloat(component, f1Buffer[0]);
                        }
                    } else if (type == String.class) {
                        String strVal = (String) field.get(component);
                        if (strVal == null) strVal = "";
                        strBuffer.set(strVal);
                        if (ImGui.inputText(label, strBuffer)) {
                            field.set(component, strBuffer.get());
                        }
                    } else if (type.isEnum()) {
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
                    } else if (type == Vector2f.class) {
                        Vector2f vec = (Vector2f) field.get(component);
                        if (vec != null) {
                            f2Buffer[0] = vec.x;
                            f2Buffer[1] = vec.y;
                            if (ImGui.dragFloat2(label, f2Buffer, 0.5f)) {
                                vec.set(f2Buffer[0], f2Buffer[1]);
                            }
                        }
                    } else if (type == Vector3f.class) {
                        Vector3f vec = (Vector3f) field.get(component);
                        if (vec != null) {
                            f3Buffer[0] = vec.x;
                            f3Buffer[1] = vec.y;
                            if (ImGui.dragFloat3(label, f3Buffer, 0.5f)) {
                                vec.set(f3Buffer[0], f3Buffer[1], f3Buffer[2]);
                            }
                        }
                    } else if (type == Vector4f.class) {
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

            // Pasa a la clase padre
            currentClass = currentClass.getSuperclass();
        }
    }

    private void drawAddComponentButton(GameObject go) {
        if (ImGui.button("Agregar Componente...", -1, 28)) {
            ImGui.openPopup("AddComponentPopup");
        }

        if (ImGui.beginPopup("AddComponentPopup")) {
            for (String category : org.ravenEngine.util.ComponentRegistry.getCategories()) {
                List< Class< ? extends Component > > comps = org.ravenEngine.util.ComponentRegistry.getComponentsByCategory(category);

                // Comprobar si queda al menos un componente disponible para agregar en esta categoría
                boolean hasAvailable = false;
                for (Class< ? extends Component > c : comps) {
                    if (go.getComponent(c) == null) {
                        hasAvailable = true;
                        break;
                    }
                }

                if (hasAvailable && ImGui.beginMenu(category)) {
                    for (Class< ? extends Component > compClass : comps) {
                        if (go.getComponent(compClass) == null) {
                            if (ImGui.menuItem(compClass.getSimpleName())) {
                                try {
                                    Component newComp = compClass.getDeclaredConstructor().newInstance();
                                    go.addComponent(newComp);
                                } catch (Exception e) {
                                    System.err.println("[Inspector ERROR] No se pudo instanciar: " + compClass.getSimpleName());
                                }
                                ImGui.closeCurrentPopup();
                            }
                        }
                    }
                    ImGui.endMenu();
                }
            }
            ImGui.endPopup();
        }
    }
}