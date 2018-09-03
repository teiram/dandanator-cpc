package com.grelobites.romgenerator.util.emulator.peripheral;

@FunctionalInterface
public interface MotorStateChangeListener {
    void onMotorStateChange(boolean newState);
}
