package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

public class Nec765CommandBase {
    protected boolean mfm;
    protected boolean multitrack;
    protected boolean skipBit;
    protected int unit;
    protected int physicalHeadNumber;

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
