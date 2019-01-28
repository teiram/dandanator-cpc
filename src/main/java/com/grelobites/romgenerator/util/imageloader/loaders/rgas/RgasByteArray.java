package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.google.gson.annotations.SerializedName;

public class RgasByteArray {

    @SerializedName("$value")
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
