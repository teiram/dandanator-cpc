package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;


import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;

public interface Nec765Command {

    void initialize(Nec765 controller);

    void write(int data);

    int read();

}
