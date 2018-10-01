package com.grelobites.romgenerator.util.emulator.peripheral.fdc.status;

/*
    Main FDC status register. Handles communication between microprocessor and FDC. Bit meaning:

    * 7th Bit (RQM): Request For Master. If set, the FDC is ready to receive or send bytes through the
    * data register.
    * 6th Bit (DIO): Data Input/Output. If set, the FDC has a byte ready to be consumed. Otherwise, the FDC
    * is waiting a byte from the processor. Only valid if RQM = 1.
    * 5th Bit (NDM): Non DMA Mode. Set to one in non-DMA mode during execution phase. It is reset to zero
    * when the execution phase ends.
    * 4th Bit (CB): FDC Busy. If set, the FDC is through a read/write command. No more commands can be processed
    * Set on receiving the first byte of a command and reset when the last result byte of the command is read.
    * 0...3rd Bits (DB): FDD 0...3 Busy. Set for a drive unit when a seek or recalibrate command is issued.
    * No read/write commands can be sent to the FDC with one of these bits set, seek or recalibrate commands for
    * other units are allowed though. The bits are reset sending a Read Interrupt State command.
 */
public class Nec765MainStatus extends StatusRegister {
    private static final int FDD0_BUSY_MASK     = 1;
    private static final int FDD1_BUSY_MASK     = 1 << 1;
    private static final int FDD2_BUSY_MASK     = 1 << 2;
    private static final int FDD3_BUSY_MASK     = 1 << 3;
    private static final int FDC_BUSY_MASK      = 1 << 4;
    private static final int EXEC_MODE_MASK     = 1 << 5;
    private static final int DATA_READY_MASK = 1 << 6;
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

    public boolean isDataReady() {
        return (value & DATA_READY_MASK) != 0;
    }

    public void setDataReady(boolean b) {
        setBitValue(b, DATA_READY_MASK);
    }

    public boolean isRQM() {
        return (value & RQM_MASK) != 0;
    }

    public void setRQM(boolean b) {
        setBitValue(b, RQM_MASK);
    }


}
