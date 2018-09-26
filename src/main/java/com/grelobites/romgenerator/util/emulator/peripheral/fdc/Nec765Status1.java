package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

/*
    Provides information about the execution phase of the different commands, in the results phase.
    Bit 7 (EN): End of Cylinder. Set on attempts to access a sector after reaching the programmed end of track.
    Bit 6: Not used (zero)
    Bit 5 (DE): Data Error. Set to one on data read and CRC mismatch (or on sector ID fields CRC mismatch)
    Bit 4 (OR): Overrun (transfer time exceeded). Set when the processor is not able to read FDC data at the
        required speed (32 usec per byte)
    Bit 3: Not used (zero).
    Bit 2 (ND): No Data. Set to one during scan or read if the FDC cannot find the required sector. Also on a
        read ID operation if the FDC cannot read succesfully the ID field (i.e.: bad CRC). Also set when the
        command read track doesn't find the initial sector.
    Bit 1 (NW): Not writable. Set whenever a write command is issued on a write protected disk.
    Bit 0 (MA): Missing Address Mark. Set when the FDC cannot find tjhe sector ID after a round trip. On
        Data Address Mark absence (or a Deleted Data Address Mark) this bit is set and also the MD bit in the
        Status register 2.
 */
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
