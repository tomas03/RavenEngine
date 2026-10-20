package org.ravenEngine.util;

import com.google.gson.*;
import org.ravenEngine.components.Component;

import java.lang.reflect.Type;

public class ComponentSerializer implements JsonSerializer<Component> {
    @Override
    public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        //Guardar nombre completo de la clase
        result.add("type",new JsonPrimitive(src.getClass().getName()));
        //Serializar las propiedades específicas de la subclase
        result.add("properties",context.serialize(src,src.getClass()));
        return result;
    }

    //Getters y Setters
    private static Gson gsonInstance;

    public static Gson getGson() {
        if (gsonInstance == null) {
            gsonInstance = new GsonBuilder()
                    .registerTypeAdapter(Component.class, new ComponentDeserializer())
                    .registerTypeAdapter(Component.class, new ComponentSerializer())
                    .setPrettyPrinting()
                    .create();
        }
        return gsonInstance;
    }
}
