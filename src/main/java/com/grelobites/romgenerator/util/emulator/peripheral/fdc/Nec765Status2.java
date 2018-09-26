package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

/*
    Bit 7: Not used (zero).
    Bit 6 (CM): Control Mark. Set when the FDC finds a deleted Data Address Mark during a read or scan command.
    Bit 5 (DD): Data Error in Data Field. Set on CRC errors in the data field.
    Bit 4 (WC): Wrong Cylinder. Set on mismatches between the cylinder number stored in the sector during
        formatting and the required cylinder on a read operation.
    Bit 3 (SH): Scan Equal Hit. Set after an scan command with an equal condition, if the comparison was
        succesful in all its bytes.
    Bit 2 (SN): Scan Not Satisfied. Set if after any scan command, no sector is found on the track that matches
        the scan requirements.
    Bit 1 (BC): Bad Cylinder). Similar to WC but set when the read cylinder number is 0xFF and also a mismatch.
    Bit 0 (MD): Missing Address Mark in Data Field. Set when on a read operation no Data Address Mark is found
        (not even a deleted one).
 */
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
