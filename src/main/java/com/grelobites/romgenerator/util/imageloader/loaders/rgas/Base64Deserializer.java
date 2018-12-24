package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;

public class Base64Deserializer extends StdDeserializer<byte[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Base64Deserializer.class);

    public Base64Deserializer() {
        this(null);
    }

    public Base64Deserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public byte[] deserialize(JsonParser jsonparser,
                              DeserializationContext context)
            throws IOException {
        return Base64.getDecoder().decode(jsonparser.getText());
    }

}
