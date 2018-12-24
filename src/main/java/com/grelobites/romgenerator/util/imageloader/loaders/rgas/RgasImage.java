package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RgasImage {

    @JsonProperty("pixels")
    private RgasByteArray pixels;

    @JsonProperty("Width")
    private int width;

    @JsonProperty("Height")
    private int height;

    @JsonProperty("_name")
    private String name;

    public RgasByteArray getPixels() {
        return pixels;
    }

    public void setPixels(RgasByteArray pixels) {
        this.pixels = pixels;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RgasImage{" +
                "pixels=" + pixels +
                ", width=" + width +
                ", height=" + height +
                ", name='" + name + '\'' +
                '}';
    }
}
