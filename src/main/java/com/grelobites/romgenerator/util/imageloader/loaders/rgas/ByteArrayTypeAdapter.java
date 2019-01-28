package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Base64;

public class ByteArrayTypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

    @Override
    public byte[] deserialize(JsonElement jsonElement,
                              Type type,
                              JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        return Base64.getDecoder().decode(jsonElement.getAsString());
    }

    @Override
    public JsonElement serialize(byte[] bytes, Type type, JsonSerializationContext jsonSerializationContext) {
        throw new IllegalArgumentException("Not implemented");
    }
}
