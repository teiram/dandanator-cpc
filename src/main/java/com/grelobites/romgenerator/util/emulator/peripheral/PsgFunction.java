package com.grelobites.romgenerator.util.emulator.peripheral;

public enum PsgFunction {
    NONE(0),
    READ(1),
    WRITE(2),
    SELECT(3);
    private int id;

    PsgFunction(int id) {
        this.id = id;
    }
    public int id() {
        return id;
    }
    public static PsgFunction fromId(int id) {
        for (PsgFunction f : PsgFunction.values()) {
            if (f.id == id) {
                return f;
            }
        }
        throw new IllegalArgumentException("Non-existing PSG function: " + id);
    }
}

