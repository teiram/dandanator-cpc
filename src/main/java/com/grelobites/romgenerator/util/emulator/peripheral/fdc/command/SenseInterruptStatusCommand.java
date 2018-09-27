package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    -== Sense Interrupt Status ==-
    COMMAND
    - B0.     0   0   0   0   1   0   0   0
    ---------------------------------------------------------------------------
    RESULT
    - B0. SR0.
    - B1. Last cylinder (of the last selected unit? )
 */
public class SenseInterruptStatusCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(SenseInterruptStatusCommand.class);

    private Nec765 controller;
    private int currentResultWord = 0;
    boolean done = false;

    private void setCommandData(int data) {
        //Only one byte and we don't need anything from it
        controller.setCurrentPhase(Nec765Phase.RESULT);
    }

    @Override
    public void setFdcController(Nec765 controller) {
        this.controller = controller;
    }

    @Override
    public void write(int data) {
        switch (controller.getCurrentPhase()) {
            case COMMAND:
                setCommandData(data);
                break;
            default:
                LOGGER.debug("Unexpected write attempt during Sense Interrupt command");
        }
    }

    @Override
    public int read() {
        switch (controller.getCurrentPhase()) {
            case RESULT:
                return getCommandResult();
            default:
                LOGGER.error("Trying to read from Sense Interrupt Command");
                return 0;
        }
    }

    protected int getCommandResult() {
        switch (currentResultWord++) {
            case 0:
                return controller.getStatus0Register().value();
            case 1:
                done = true;
                return controller.getDriveStatus(controller.getLastSelectedUnit())
                        .getCurrentSector().getTrack();
            default:
                throw new IllegalStateException("Result status exhausted");
        }
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
