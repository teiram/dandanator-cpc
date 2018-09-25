package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

public class Nec765Status3 extends StatusRegister {
    private static final int FDD_FAULT_MASK           = 1 << 7;
    private static final int FDD_WRITE_PROTECTED_MASK = 1 << 6;
    private static final int FDD_READY_MASK           = 1 << 5;
    private static final int FDD_TRACK0_MASK          = 1 << 4;
    private static final int FDD_2SIDE_MASK           = 1 << 3;
    private static final int FDD_HEAD_MASK            = 1 << 2;
    private static final int FDD_UNIT_MASK            = 0x03;

    public Nec765Status3(int value) {
        super(value);
    }

    public boolean isFddFault() {
        return (value & FDD_FAULT_MASK) != 0;
    }

    public void setFddFault(boolean b) {
        setBitValue(b, FDD_FAULT_MASK);
    }

    public boolean isFddWriteProtected() {
        return (value & FDD_WRITE_PROTECTED_MASK) != 0;
    }

    public void setFddWriteProtected(boolean b) {
        setBitValue(b, FDD_WRITE_PROTECTED_MASK);
    }

    public boolean isFddReady() {
        return (value & FDD_READY_MASK) != 0;
    }

    public void setFddReady(boolean b) {
        setBitValue(b, FDD_READY_MASK);
    }

    public boolean isFddTrack0() {
        return (value & FDD_TRACK0_MASK) != 0;
    }

    public void setFddTrack0(boolean b) {
        setBitValue(b, FDD_TRACK0_MASK);
    }

    public boolean isFdd2Side() {
        return (value & FDD_2SIDE_MASK) != 0;
    }

    public void setFdd2Side(boolean b) {
        setBitValue(b, FDD_2SIDE_MASK);
    }

    public boolean isFddHead() {
        return (value & FDD_HEAD_MASK) != 0;
    }

    public void setFddHead(boolean b) {
        setBitValue(b, FDD_HEAD_MASK);
    }
    
    public int getFddUnit() {
        return value & FDD_UNIT_MASK;
    }

    public void setFddUnit(int unit) {
        value &= ~FDD_UNIT_MASK;
        value |= (unit & FDD_UNIT_MASK);
    }

}
