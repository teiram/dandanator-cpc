package com.grelobites.romgenerator.util.emulator.peripheral;

@FunctionalInterface
public interface ChangeListener<T> {
    boolean onChange(T value);
}
