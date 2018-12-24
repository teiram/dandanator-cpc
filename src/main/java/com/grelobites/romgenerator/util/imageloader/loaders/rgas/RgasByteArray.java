package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RgasByteArray {

    @JsonProperty("$value")
    @JsonDeserialize(using = Base64Deserializer.class)
    private byte[] value;

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "RgasByteArray{" +
                "length=" + (value != null ? value.length : "undefined") +
                '}';
    }
}
