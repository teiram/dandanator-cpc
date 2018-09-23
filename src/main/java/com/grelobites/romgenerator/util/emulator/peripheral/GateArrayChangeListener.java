package com.grelobites.romgenerator.util.emulator.peripheral;

@FunctionalInterface
public interface GateArrayChangeListener {
    boolean onChange(GateArrayFunction function, int value);
}
