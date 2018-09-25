package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

public class Nec765MainStatus extends StatusRegister {
    private static final int FDD0_BUSY_MASK     = 1;
    private static final int FDD1_BUSY_MASK     = 1 << 1;
    private static final int FDD2_BUSY_MASK     = 1 << 2;
    private static final int FDD3_BUSY_MASK     = 1 << 3;
    private static final int FDC_BUSY_MASK      = 1 << 4;
    private static final int EXEC_MODE_MASK     = 1 << 5;
    private static final int DATA_INPUT_MASK    = 1 << 6;
    private static final int RQM_MASK           = 1 << 7;

    public Nec765MainStatus(int value) {
        super(value);
    }

    public boolean isFdd0Busy() {
        return (value & FDD0_BUSY_MASK) != 0;
    }

    private void setFdd0Busy(boolean b) {
        setBitValue(b, FDD0_BUSY_MASK);
    }

    public boolean isFdd1Busy() {
        return (value & FDD1_BUSY_MASK) != 0;
    }

    public void setFdd1Busy(boolean b) {
        setBitValue(b, FDD1_BUSY_MASK);
    }

    public boolean isFdd2Busy() {
        return (value & FDD2_BUSY_MASK) != 0;
    }

    public void setFdd2Busy(boolean b) {
        setBitValue(b, FDD2_BUSY_MASK);
    }

    public boolean isFdd3Busy() {
        return (value & FDD3_BUSY_MASK) != 0;
    }

    public void setFdd3Busy(boolean b) {
        setBitValue(b, FDD3_BUSY_MASK);
    }

    public boolean isFdcBusy() {
        return (value & FDC_BUSY_MASK) != 0;
    }

    public void setFdcBusy(boolean b) {
        setBitValue(b, FDC_BUSY_MASK);
    }

    public boolean isExecMode() {
        return (value & EXEC_MODE_MASK) != 0;
    }

    public void setExecMode(boolean b) {
        setBitValue(b, EXEC_MODE_MASK);
    }

    public boolean isDataInput() {
        return (value & DATA_INPUT_MASK) != 0;
    }

    public void setDataInput(boolean b) {
        setBitValue(b, DATA_INPUT_MASK);
    }

    public boolean isRQM() {
        return (value & RQM_MASK) != 0;
    }

    public void setRQM(boolean b) {
        setBitValue(b, RQM_MASK);
    }


}
