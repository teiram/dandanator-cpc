package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

public class Nec765Status2 extends StatusRegister {
    private static final int CONTROL_MARK_MASK      = 1 << 6;
    private static final int DATA_FIELD_ERROR_MASK  = 1 << 5;
    private static final int WRONG_CYLINDER_MASK    = 1 << 4;
    private static final int SCAN_OK_MASK           = 1 << 3;
    private static final int SCAN_NOTOK_MASK        = 1 << 2;
    private static final int BAD_CYLINDER_MASK      = 1 << 1;
    private static final int NO_ADDRESS_MARK_MASK   = 1;

    public Nec765Status2(int value) {
        super(value);
    }

    public boolean isControlMark() {
        return (value & CONTROL_MARK_MASK) != 0;
    }

    public void setControlMark(boolean b) {
        setBitValue(b, CONTROL_MARK_MASK);
    }

    public boolean isDataFieldError() {
        return (value & DATA_FIELD_ERROR_MASK) != 0;
    }

    public void setDataFieldError(boolean b) {
        setBitValue(b, DATA_FIELD_ERROR_MASK);
    }

    public boolean isWrongCylinder() {
        return (value & WRONG_CYLINDER_MASK) != 0;
    }

    public void setWrongCylinder(boolean b) {
        setBitValue(b, WRONG_CYLINDER_MASK);
    }

    public boolean isScanOk() {
        return (value & SCAN_OK_MASK) != 0;
    }

    public void setScanOk(boolean b) {
        setBitValue(b, SCAN_OK_MASK);
    }

    public boolean isScanNotSatisfied() {
        return (value & SCAN_NOTOK_MASK) != 0;
    }

    public void setScanNotSatisfied(boolean b) {
        setBitValue(b, SCAN_NOTOK_MASK);
    }

    public boolean isBadCylinder() {
        return (value & BAD_CYLINDER_MASK) != 0;
    }

    public void setBadCylinder(boolean b) {
        setBitValue(b, BAD_CYLINDER_MASK);
    }

    public boolean isMissingAddressMark() {
        return (value & NO_ADDRESS_MARK_MASK) != 0;
    }

    public void setMissingAddressMark(boolean b) {
        setBitValue(b, NO_ADDRESS_MARK_MASK);
    }

}
