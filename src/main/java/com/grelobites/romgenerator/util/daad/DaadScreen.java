package com.grelobites.romgenerator.util.daad;

public class DaadScreen {
    private int slotOffset;
    private byte[] data;

    public DaadScreen(byte[] data) {
        this.data = data;
        this.slotOffset = 0;
    }

    public int getSlotOffset() {
        return slotOffset;
    }

    public void setSlotOffset(int slotOffset) {
        this.slotOffset = slotOffset;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
