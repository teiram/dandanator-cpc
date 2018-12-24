package com.grelobites.romgenerator.util.imageloader.loaders.rgas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RgasFile {
    @JsonProperty("_ImageList")
    private RgasImageList imageList;

    @JsonProperty("Mode")
    private int mode;

    @JsonProperty("Inks")
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
