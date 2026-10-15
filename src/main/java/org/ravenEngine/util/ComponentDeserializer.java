package org.ravenEngine.util;

import com.google.gson.*;
import org.ravenEngine.scene.Component;

import java.lang.reflect.Type;


public class ComponentDeserializer implements JsonSerializer<Component>,JsonDeserializer<Component> {
    @Override
    public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        JsonElement element = jsonObject.get("properties");

        try {
            //Buscamos la clase real en tiempo de ejecución
            Class<?> clazz = Class.forName(type);
            return context.deserialize(element,clazz);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Componente desconocido al deserializar: "+type,e);
        }
    }

    @Override
    public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        //Guardamos el nombre completo de la clase para saber cuál instanciar al cargar
        result.add("type",new JsonPrimitive(src.getClass().getCanonicalName()));
        result.add("properties",context.serialize(src,src.getClass()));
        return result;
    }
}
