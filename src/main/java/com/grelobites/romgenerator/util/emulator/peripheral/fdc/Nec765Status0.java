package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

public class Nec765Status0 extends StatusRegister {
    private static final int INTERRUPT_CODE_MASK     = 0xc0;
    private static final int SEEK_END_MASK           = 1 << 5;
    private static final int EQUIPMENT_CHECK_MASK    = 1 << 4;
    private static final int NOT_READY_MASK          = 1 << 3;
    private static final int HEAD_ADDRESS_MASK       = 1 << 2;
    private static final int DISK_UNIT_MASK          = 0x03;

    public Nec765Status0(int value) {
        super(value);
    }

    public int getInterruptCode() {
        return (value & INTERRUPT_CODE_MASK) >>> 6;
    }

    public void setInterruptCode(int ic) {
        value &= ~INTERRUPT_CODE_MASK;
        value |= (ic & 0x03) << 6;
    }

    public boolean isSeekEnd() {
        return (value & SEEK_END_MASK) != 0;
    }

    public void setSeekEnd(boolean b) {
        setBitValue(b, SEEK_END_MASK);
    }

    public boolean isEquipmentCheck() {
        return (value & EQUIPMENT_CHECK_MASK) != 0;
    }

    public void setEquipmentCheck(boolean b) {
        setBitValue(b, EQUIPMENT_CHECK_MASK);
    }

    public boolean isNotReady() {
        return (value & NOT_READY_MASK) != 0;
    }

    public void setNotReady(boolean b) {
        setBitValue(b, NOT_READY_MASK);
    }

    public int getHeadAddress() {
        return (value & HEAD_ADDRESS_MASK) >>> 2;
    }

    public void setHeadAddress(int ha) {
        value &= ~HEAD_ADDRESS_MASK;
        value |= (ha & 0x01) << 2;
    }

    public int getDiskUnit() {
        return (value & DISK_UNIT_MASK);
    }

    public void setDiskUnit(int du) {
        value &= ~DISK_UNIT_MASK;
        value |= (du & DISK_UNIT_MASK);
    }

}
