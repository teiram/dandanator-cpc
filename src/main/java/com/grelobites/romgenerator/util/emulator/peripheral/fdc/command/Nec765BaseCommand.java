package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;

public class Nec765BaseCommand {
    protected Nec765 controller;
    protected boolean mfm;
    protected boolean multitrack;
    protected boolean skipBit;
    protected int unit;
    protected int physicalHeadNumber;
    protected boolean done;

    public void setFdcController(Nec765 controller) {
        if (controller.getCurrentCommand() == null) {
            this.controller = controller;
            onStartup();
            controller.setCurrentPhase(Nec765Phase.COMMAND);
        } else {
            throw new IllegalStateException("Controller executing command");
        }
    }

    protected void onStartup() {
        controller.getMainStatusRegister().setFdcBusy(true);
        switch (unit) {
            case 0:
                controller.getMainStatusRegister().setFdd0Busy(true);
                break;
            case 1:
                controller.getMainStatusRegister().setFdd1Busy(true);
                break;
            case 2:
                controller.getMainStatusRegister().setFdd2Busy(true);
                break;
            case 4:
                controller.getMainStatusRegister().setFdd3Busy(true);
        }
    }

    protected void onExecution() {
        controller.getMainStatusRegister().setExecMode(true);
    }

    protected void onCommandDone() {
        controller.getMainStatusRegister().setValue(0x80);
    }

    public boolean isDone() {
        return done;
    }

    protected void setPrimaryFlags(int data) {
        multitrack = (data & 0x80) != 0;
        mfm = (data & 0x40) != 0;
        skipBit = (data & 0x20) != 0;
    }

    protected void setSecondaryFlags(int data) {
        physicalHeadNumber = (data & 0x40) >>> 3;
        unit = data & 0x03;
    }
}
