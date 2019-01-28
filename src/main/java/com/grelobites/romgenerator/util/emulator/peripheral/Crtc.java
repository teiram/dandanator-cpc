package com.grelobites.romgenerator.util.emulator.peripheral;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class Crtc {
    private static final Logger LOGGER = LoggerFactory.getLogger(Crtc.class);

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

    private Set<CrtcChangeListener> crtcChangeListeners = new HashSet<>();

    public Crtc(CrtcType crtcType) {
        this.crtcType = crtcType;
    }

    private boolean notifyListeners(CrtcOperation operation) {
        for (CrtcChangeListener listener: crtcChangeListeners) {
            if (!listener.onChange(operation)) {
                return false;
            }
        }
        return true;
    }

    public void addChangeListener(CrtcChangeListener listener) {
        crtcChangeListeners.add(listener);
    }

    public void removeChangeListener(CrtcChangeListener listener) {
        crtcChangeListeners.remove(listener);
    }

    public void onSelectRegisterOperation(int register) {
        if (notifyListeners(CrtcOperation.SELECT_REGISTER)) {
            if (register < NUM_REGISTERS) {
                selectedRegister = register;
            } else {
                throw new IllegalArgumentException("Invalid CRTC register index");
            }
        } else {
            LOGGER.debug("CRTC Register Select rejected by listener");
        }
    }

    public void onWriteRegisterOperation(int value) {
        if (notifyListeners(CrtcOperation.WRITE_REGISTER)) {
            crtcRegisterData[selectedRegister] = (byte) value;
        } else {
            LOGGER.debug("CRTC Write Register rejected by listener");
        }
    }

    public int onReadStatusRegisterOperation() {
        if (notifyListeners(CrtcOperation.READ_STATUS_REGISTER)) {
            if (crtcType.hasFunction2()) {
                if (!crtcType.hasReadStatusFunction()) {
                    return onReadRegisterOperation();
                } else {
                    return statusRegister;
                }
            } else {
                return 0;
            }
        } else {
            LOGGER.debug("CRTC Read Status rejected by listener");
        }
        return 0;
    }

    public int onReadRegisterOperation() {
        if (notifyListeners(CrtcOperation.READ_REGISTER)) {
            return crtcType.canReadRegister(selectedRegister) ?
                    crtcRegisterData[selectedRegister] : 0;
        } else {
            LOGGER.debug("CRTC Read Register rejected by listener");
            return 0;
        }
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

    public int getStatusRegister() {
        return statusRegister;
    }

    public void setStatusRegister(int statusRegister) {
        this.statusRegister = statusRegister;
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

    private int getScreenPage() {
        return crtcRegisterData[REGISTER_DISPLAY_START_ADDR_HI] & 0x30 << 10;
    }

    private int getScreenOffset() {
        return (((crtcRegisterData[REGISTER_DISPLAY_START_ADDR_HI] & 0x3) << 8) |
                (crtcRegisterData[REGISTER_DISPLAY_START_ADDR_LO] & 0xff)) << 4;
    }

    private int getScreenSize() {
        return (crtcRegisterData[REGISTER_DISPLAY_START_ADDR_HI] & 0xC0) == 0xC0 ?
                32768 : 16384;
    }

    public boolean isVideoAddress(int address) {
        final int baseScreenAddress = getScreenPage() + getScreenOffset();
        final int screenSize = getScreenSize();
        return address >= baseScreenAddress && address < (baseScreenAddress + screenSize);
    }
}
