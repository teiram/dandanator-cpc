package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RgasImageList {

    @SerializedName("$values")
    private List<RgasImage> values;

    public List<RgasImage> getValues() {
        return values;
    }

    public void setValues(List<RgasImage> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "RgasImageList{" +
                "values=" + values +
                '}';
    }
}
