package com.grelobites.romgenerator.util.daad;

public enum MldType {
    MLD_48K(0x83),
    MLD_128K(0x88),
    MLD_PLUS2A(0xC8);

    private int id;

    MldType(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }
}
