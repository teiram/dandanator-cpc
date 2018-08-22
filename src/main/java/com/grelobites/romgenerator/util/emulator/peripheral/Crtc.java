package com.grelobites.romgenerator.util.emulator.peripheral;

public class Crtc {
    private static final int NUM_REGISTERS = 18;
    private int[] crtcRegisterData = new int[NUM_REGISTERS];
    private int statusRegister = 0;
    private int selectedRegister;
    private CrtcType crtcType;

    public Crtc(CrtcType crtcType) {
        this.crtcType = crtcType;
    }

    public void onSelectRegisterOperation(int register) {
        if (register < NUM_REGISTERS) {
            selectedRegister = register;
        } else {
            throw new IllegalArgumentException("Invalid CRTC register index");
        }
    }

    public void onWriteRegisterOperation(int value) {
        crtcRegisterData[selectedRegister] = value;
    }

    public int onReadStatusRegisterOperation() {
        if (crtcType.hasFunction2()) {
            if (!crtcType.hasReadStatusFunction()) {
                return onReadRegisterOperation();
            } else {
                return statusRegister;
            }
        } else {
            return 0;
        }
    }

    public int onReadRegisterOperation() {
        return crtcType.canReadRegister(selectedRegister) ?
                crtcRegisterData[selectedRegister] : 0;
    }

    public int[] getCrtcRegisterData() {
        return crtcRegisterData;
    }

}
