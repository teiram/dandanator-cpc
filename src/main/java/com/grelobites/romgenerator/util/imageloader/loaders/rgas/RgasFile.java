package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.google.gson.annotations.SerializedName;

public class RgasFile {
    @SerializedName("_ImageList")
    private RgasImageList imageList;

    @SerializedName("Mode")
    private int mode;

    @SerializedName("Inks")
    private RgasByteArray inks;

    public RgasImageList getImageList() {
        return imageList;
    }

    public void setImageList(RgasImageList imageList) {
        this.imageList = imageList;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public RgasByteArray getInks() {
        return inks;
    }

    public void setInks(RgasByteArray inks) {
        this.inks = inks;
    }

    @Override
    public String toString() {
        return "RgasFile{" +
                "imageList=" + imageList +
                ", mode=" + mode +
                ", inks=" + inks +
                '}';
    }
}
