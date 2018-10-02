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

    public void initialize(Nec765 controller) {
        this.controller = controller;
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
