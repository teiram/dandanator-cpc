package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

public class Nec765Status1 extends StatusRegister {
    private static final int END_OF_CYLINDER_MASK    = 1 << 7;
    private static final int CRC_ERROR_MASK          = 1 << 5;
    private static final int OVERRUN_MASK            = 1 << 4;
    private static final int NO_DATA_MASK            = 1 << 2;
    private static final int NO_WRITEABLE_MASK       = 1 << 1;
    private static final int NO_ADDRESS_MARK_MASK    = 1;

    public Nec765Status1(int value) {
        super(value);
    }

    public boolean isEndOfCylinder() {
        return (value & END_OF_CYLINDER_MASK) != 0;
    }

    public void setEndOfCylinder(boolean b) {
        setBitValue(b, END_OF_CYLINDER_MASK);
    }

    public boolean isCrcError() {
        return (value & CRC_ERROR_MASK) != 0;
    }

    public void setCrcError(boolean b) {
        setBitValue(b, CRC_ERROR_MASK);
    }

    public boolean isOverrun() {
        return (value & OVERRUN_MASK) != 0;
    }

    public void setOverrun(boolean b) {
        setBitValue(b, OVERRUN_MASK);
    }

    public boolean isNoData() {
        return (value & NO_DATA_MASK) != 0;
    }

    public void setNoData(boolean b) {
        setBitValue(b, NO_DATA_MASK);
    }

    public boolean isNoWritable() {
        return (value & NO_WRITEABLE_MASK) != 0;
    }

    public void setNoWritable(boolean b) {
        setBitValue(b, NO_WRITEABLE_MASK);
    }

    public boolean isMissingAddressMark() {
        return (value & NO_ADDRESS_MARK_MASK) != 0;
    }

    public void setMissingAddressMark(boolean b) {
        setBitValue(b, NO_ADDRESS_MARK_MASK);
    }

}
