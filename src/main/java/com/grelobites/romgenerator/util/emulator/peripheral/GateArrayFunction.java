package com.grelobites.romgenerator.util.emulator.peripheral;

public enum GateArrayFunction {
    PEN_SELECTION_FN(0),
    PALETTE_DATA_FN(1),
    SCREEN_MODE_AND_ROM_CFG_FN(2),
    RAM_BANKING_FN(3);

    private int id;

    GateArrayFunction(int id) {
        this.id = id;
    }

    public static GateArrayFunction fromId(int id) {
        for (GateArrayFunction fn : GateArrayFunction.values()) {
            if (fn.id == id) {
                return fn;
            }
        }
        throw new IllegalArgumentException("No GateArray Function with id " + id);
    }
}
