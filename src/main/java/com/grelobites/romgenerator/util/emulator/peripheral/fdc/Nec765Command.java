package com.grelobites.romgenerator.util.emulator.peripheral.fdc;


public interface Nec765Command {

    void setFdcController(Nec765 controller);

    void write(int data);

    int read();

    boolean isDone();
}
