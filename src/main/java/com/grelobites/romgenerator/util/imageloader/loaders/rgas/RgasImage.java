package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.google.gson.annotations.SerializedName;

public class RgasImage {

    @SerializedName("pixels")
    private RgasByteArray pixels;

    @SerializedName("Width")
    private int width;

    @SerializedName("Height")
    private int height;

    @SerializedName("_name")
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
