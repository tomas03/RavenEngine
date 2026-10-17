package org.ravenEngine.util;

import com.google.gson.*;
import org.ravenEngine.scene.Component;

import java.lang.reflect.Type;

public class ComponentDeserializer implements JsonDeserializer<Component> {

    @Override
    public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        JsonElement typeElement = jsonObject.get("type");
        String type = null;

        if (typeElement != null && !typeElement.isJsonNull()) {
            type = typeElement.getAsString();
        } else {
            // Fallback limpio por si abrís un JSON viejo que no tenía "type"
            if (jsonObject.has("tiles") || jsonObject.has("tileWidth")) {
                type = "Tilemap";
            } else if (jsonObject.has("sprite") || jsonObject.has("color")) {
                type = "SpriteRenderer";
            } else if (jsonObject.has("speed") || jsonObject.has("lastDirection")) {
                type = "PlayerController";
            } else if (jsonObject.has("animations")) {
                type = "SpriteAnimator";
            } else if (jsonObject.has("camera") || jsonObject.has("smoothSpeed") || jsonObject.has("viewportSize")) {
                type = "CameraFollow";
            } else if (jsonObject.has("size") || jsonObject.has("isTrigger") || jsonObject.has("isStatic")) {
                type = "BoxCollider2D";
            } else {
                throw new JsonParseException("Objeto JSON de Component no posee la propiedad 'type': " + jsonObject);
            }
        }

        JsonElement properties = jsonObject.get("properties");

        Class< ? > clazz = findComponentClass(type);

        if (clazz == null) {
            throw new JsonParseException("Componente desconocido al deserializar: " + type);
        }

        return context.deserialize(properties != null ? properties : jsonObject, clazz);
    }

    private Class<?> findComponentClass(String type) {
        // 1. Si ya viene con el nombre de clase completo (FQCN)
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException ignored) {}

        // 2. Si viene sólo el nombre simple (o si el paquete quedó desfasado), probamos los paquetes conocidos:
        String simpleName = type.contains(".") ? type.substring(type.lastIndexOf('.') + 1) : type;

        String[] packages = new String[] {
                "org.ravenEngine.renderer.",
                "org.ravenEngine.components.",
                "org.alchemistShop.assets.scenes.entities.",
                "org.ravenEngine.physics."
        };

        for (String pkg : packages) {
            try {
                return Class.forName(pkg + simpleName);
            } catch (ClassNotFoundException ignored) {}
        }

        return null;
    }
}