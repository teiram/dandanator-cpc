package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RgasImageList {

    @JsonProperty("$values")
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
