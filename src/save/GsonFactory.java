package save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import components.Component;
import staff.Staff;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

final class GsonFactory {
    private GsonFactory() {
    }

    static Gson create() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new RuntimeTypeAdapter<>(Component.class, "componentType")
                        .registerSubtype("Engine", components.Engine.class)
                        .registerSubtype("Transmission", components.Transmission.class)
                        .registerSubtype("Chassis", components.Chassis.class)
                        .registerSubtype("Suspension", components.Suspension.class)
                        .registerSubtype("Aerodynamics", components.Aerodynamics.class)
                        .registerSubtype("Tyres", components.Tyres.class))
                .registerTypeAdapter(Staff.class, new RuntimeTypeAdapter<>(Staff.class, "staffType")
                        .registerSubtype("Engineer", staff.Engineer.class)
                        .registerSubtype("Mechanic", staff.Mechanic.class)
                        .registerSubtype("ElectronicsEngineer", staff.ElectronicsEngineer.class)
                        .registerSubtype("Principal", staff.Principal.class)
                        .registerSubtype("TechnicalDirector", staff.TechnicalDirector.class))
                .create();
    }

    private static final class RuntimeTypeAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {
        private final Class<T> baseType;
        private final String typeFieldName;
        private final Map<String, Class<? extends T>> subtypesByName = new HashMap<>();
        private final Map<Class<? extends T>, String> subtypeNames = new HashMap<>();

        private RuntimeTypeAdapter(Class<T> baseType, String typeFieldName) {
            this.baseType = baseType;
            this.typeFieldName = typeFieldName;
        }

        private RuntimeTypeAdapter<T> registerSubtype(String name, Class<? extends T> subtype) {
            subtypesByName.put(name, subtype);
            subtypeNames.put(subtype, name);
            return this;
        }

        @Override
        public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
            JsonElement tree = context.serialize(src, src.getClass());
            JsonObject object = tree.getAsJsonObject();
            String label = subtypeNames.get(src.getClass());
            if (label == null) {
                throw new JsonParseException("Unknown subtype for " + baseType.getSimpleName());
            }
            object.addProperty(typeFieldName, label);
            return object;
        }

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject object = json.getAsJsonObject();
            JsonElement typeElement = object.get(typeFieldName);
            if (typeElement == null) {
                throw new JsonParseException("Missing type field " + typeFieldName);
            }
            String label = typeElement.getAsString();
            Class<? extends T> subtype = subtypesByName.get(label);
            if (subtype == null) {
                throw new JsonParseException("Unknown subtype " + label + " for " + baseType.getSimpleName());
            }
            return context.deserialize(json, subtype);
        }
    }
}
