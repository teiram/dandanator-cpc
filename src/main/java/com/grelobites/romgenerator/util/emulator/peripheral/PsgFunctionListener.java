package com.grelobites.romgenerator.util.emulator.peripheral;


@FunctionalInterface
public interface PsgFunctionListener {
    void onPsgFunctionExecution(PsgFunction function);
}
