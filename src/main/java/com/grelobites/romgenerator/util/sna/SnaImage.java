package com.grelobites.romgenerator.util.sna;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface SnaImage {
    static final int CRTC_REGISTER_DATA_LENGTH = 18;
    static final int GATE_ARRAY_PALETTE_LENGTH = 17;
    static final int PSG_REGISTER_DATA_LENGTH = 16;
    int getSnapshotVersion();
    int getAFRegister();
    int getBCRegister();
    int getDERegister();
    int getHLRegister();
    int getIXRegister();
    int getIYRegister();
    int getIRegister();
    int getRRegister();
    int getPC();
    int getSP();
    int getIFF0();
    int getIFF1();
    int getInterruptMode();
    int getAltAFRegister();
    int getAltBCRegister();
    int getAltDERegister();
    int getAltHLRegister();

    int getGateArraySelectedPen();
    byte[] getGateArrayCurrentPalette();
    int getGateArrayMultiConfiguration();

    int getCurrentRamConfiguration();

    int getCrtcSelectedRegisterIndex();
    byte[] getCrtcRegisterData();

    int getCurrentRomSelection();

    int getPpiPortA();
    int getPpiPortB();
    int getPpiPortC();
    int getPpiControlPort();

    int getPsgSelectedRegisterIndex();
    byte[] getPsgRegisterData();

    int getMemoryDumpSize();

    byte[] getMemoryDump();

    //Version 2
    int getCpcType();
    int getInterruptNumber();
    byte[] getMultimodeBytes();

    //Version 3
    int getFddMotorDriveState();
    int getFddCurrentPhysicalTrack();
    int getPrinterDataStrobeRegister();
    int getCrtcType();
    int getCrtcHorizontalCharacterCounterRegister();
    int getCrtcCharacterLineCounterRegister();
    int getCrtcRasterLineCounterRegister();
    int getCrtcVerticalTotalAdjustCounterRegister();
    int getCrtcHorizontalSyncWidthCounter();
    int getCrtcVerticalSyncWidthCounter();
    int getCrtcStateFlags();

    int getGAVSyncDelayCounter();
    int getGAInterruptScanlineCounter();
    int getInterruptRequestedFlag();

    Map<String, SnaChunk> getSnaChunks();

    void dump(OutputStream os) throws IOException;
}
