package com.grelobites.romgenerator.util.emulator.peripheral;

@FunctionalInterface
public interface CrtcChangeListener {
    boolean onChange(CrtcOperation operation);
}
