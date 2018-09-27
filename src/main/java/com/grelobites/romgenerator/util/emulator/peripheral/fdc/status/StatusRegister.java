package com.grelobites.romgenerator.util.emulator.peripheral.fdc.status;

public class StatusRegister {
    protected static final int REGISTER_MASK = 0xff;

    protected int value;

    protected StatusRegister(int value) {
        this.value = value & REGISTER_MASK;
    }

    protected void setBitValue(boolean b, int mask) {
        value = b ? value | mask : value & ~mask;
    }

    public int value() {
        return value & REGISTER_MASK;
    }

    public void setValue(int value) {
        this.value = value & REGISTER_MASK;
    }

    @Override
    public String toString() {
        return "StatusRegister{" +
                "value=" + String.format("0x%02x", value & REGISTER_MASK) +
                '}';
    }
}
