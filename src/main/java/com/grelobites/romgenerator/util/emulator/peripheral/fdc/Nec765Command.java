package com.grelobites.romgenerator.util.emulator.peripheral.fdc;


public interface Nec765Command {

    void setFdcController(Nec765 controller);

    void addCommandData(int data);

    boolean isPrepared();

    boolean hasExecutionPhase();

    int execute();

    boolean isExecuted();

    int result();

    boolean isDone();
}
