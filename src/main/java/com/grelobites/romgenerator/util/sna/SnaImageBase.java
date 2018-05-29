package com.grelobites.romgenerator.util.sna;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

public class SnaImageBase {
    public static final byte[] DEFAULT_MULTIMODE_BYTES = new byte[] {0, 0, 0, 0, 0, 0};

    //Version 2
    public int getCpcType() {
        return 0;
    }

    public int getInterruptNumber() {
        return 0;
    }

    public byte[] getMultimodeBytes() {
        return DEFAULT_MULTIMODE_BYTES;
    }

    //Version 3
    public int getFddMotorDriveState() {
        return 0;
    }

    public int getFddCurrentPhysicalTrack() {
        return 0;
    }

    public int getPrinterDataStrobeRegister() {
        return 0;
    }

    public int getCrtcType() {
        return 0;
    }

    public int getCrtcHorizontalCharacterCounterRegister() {
        return 0;
    }

    public int getCrtcCharacterLineCounterRegister() {
        return 0;
    }

    public int getCrtcRasterLineCounterRegister() {
        return 0;
    }

    public int getCrtcVerticalTotalAdjustCounterRegister() {
        return 0;
    }

    public int getCrtcHorizontalSyncWidthCounter() {
        return 0;
    }

    public int getCrtcVerticalSyncWidthCounter() {
        return 0;
    }

    public int getCrtcStateFlags() {
        return 0;
    }

    public int getGAVSyncDelayCounter() {
        return 0;
    }

    public int getGAInterruptScanlineCounter() {
        return 0;
    }

    public int getInterruptRequestedFlag() {
        return 0;
    }

    public Map<String, SnaChunk> getSnaChunks() {
        return Collections.emptyMap();
    }

    public void dump(OutputStream os) throws IOException {
        throw new UnsupportedOperationException("No dump implemented in this image type");
    }
}
