package com.grelobites.romgenerator.util.emulator.peripheral.fdc;


public interface Nec765Command {

    void setFdcController(Nec765 controller);

    void addCommandData(int data);

    int execute();

    int result();

    boolean isDone();
}
