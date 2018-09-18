package com.grelobites.romgenerator.util.emulator.peripheral;

public class Crtc {
    private static final int NUM_REGISTERS = 18;

    public static final int REGISTER_HORIZ_TOTAL            = 0;
    public static final int REGISTER_HORIZ_DISPLAYED        = 1;
    public static final int REGISTER_HORIZ_SYNC_POS         = 2;
    public static final int REGISTER_SYNC_WIDTHS            = 3;
    public static final int REGISTER_VERT_TOTAL             = 4;
    public static final int REGISTER_VERT_TOTAL_ADJUST      = 5;
    public static final int REGISTER_VERT_DISPLAYED         = 6;
    public static final int REGISTER_VERT_SYNC_POS          = 7;
    public static final int REGISTER_INTERLACE_AND_SKEW     = 8;
    public static final int REGISTER_MAX_RASTER_ADDR        = 9;
    public static final int REGISTER_CURSOR_START_RASTER    = 10;
    public static final int REGISTER_CURSOR_END_RASTER      = 11;
    public static final int REGISTER_DISPLAY_START_ADDR_HI  = 12;
    public static final int REGISTER_DISPLAY_START_ADDR_LO  = 13;
    public static final int REGISTER_CURSOR_ADDR_HI         = 14;
    public static final int REGISTER_CURSOR_ADDR_LO         = 15;
    public static final int REGISTER_LIGHTPEN_ADDR_HI       = 16;
    public static final int REGISTER_LIGHTPEN_ADDR_LO       = 17;

    private byte[] crtcRegisterData = new byte[NUM_REGISTERS];
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
        crtcRegisterData[selectedRegister] = (byte) value;
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

    public byte[] getCrtcRegisterData() {
        return crtcRegisterData;
    }

    public void setCrtcRegisterData(byte[] data) {
        System.arraycopy(data, 0, crtcRegisterData, 0,
                Math.min(data.length, NUM_REGISTERS));
    }

    public int getSelectedRegister() {
        return selectedRegister;
    }

    public void setSelectedRegister(int value) {
        this.selectedRegister = value;
    }

    public long getHorizontalTotal() {
        return crtcRegisterData[REGISTER_HORIZ_TOTAL] + 1;
    }

    public int getMaximumRasterAddress() {
        return crtcRegisterData[REGISTER_MAX_RASTER_ADDR] & 0x7;
    }

    public int getVSyncPos() {
        return crtcRegisterData[REGISTER_VERT_SYNC_POS] & 0x7f;
    }

    public int getHSyncPos() {
        return crtcRegisterData[REGISTER_HORIZ_SYNC_POS];
    }

    public int getHSyncLength() {
        int value = crtcRegisterData[REGISTER_SYNC_WIDTHS] & 0xf;
        return value != 0 ? value : 16;
    }

    public int getVSyncLength() {
        return (crtcRegisterData[REGISTER_SYNC_WIDTHS] >>> 4) & 0x0f;
    }
}
