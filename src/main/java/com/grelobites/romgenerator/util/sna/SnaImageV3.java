package com.grelobites.romgenerator.util.sna;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.sna.SnaCompressedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SnaImageV3 extends SnaImageV2 implements SnaImage {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnaImageV3.class);
    private int fddMotorDriveState;
    private int fddCurrentPhysicalTrack;
    private int printerDataStrobeRegister;

    private int crtcType;
    private int crtcHorizontalCharacterCounterRegister;
    private int crtcCharacterLineCounterRegister;
    private int crtcRasterLineCounterRegister;
    private int crtcVerticalTotalAdjustCounterRegister;
    private int crtcHorizontalSyncWidthCounter;
    private int crtcVerticalSyncWidthCounter;
    private int crtcStateFlags;

    private int gaVSyncDelayCounter;
    private int gaInterruptScanLineCounter;
    private int interruptRequestedFlag;
    private Map<String, SnaChunk> snaChunks = new HashMap<>();

    @Override
    public int getSnapshotVersion() {
        return 3;
    }

    @Override
    public int getFddMotorDriveState() {
        return fddMotorDriveState;
    }

    @Override
    public int getFddCurrentPhysicalTrack() {
        return fddCurrentPhysicalTrack;
    }

    @Override
    public int getPrinterDataStrobeRegister() {
        return printerDataStrobeRegister;
    }

    @Override
    public int getCrtcType() {
        return crtcType;
    }

    @Override
    public int getCrtcHorizontalCharacterCounterRegister() {
        return crtcHorizontalCharacterCounterRegister;
    }

    @Override
    public int getCrtcCharacterLineCounterRegister() {
        return crtcCharacterLineCounterRegister;
    }

    @Override
    public int getCrtcRasterLineCounterRegister() {
        return crtcRasterLineCounterRegister;
    }

    @Override
    public int getCrtcVerticalTotalAdjustCounterRegister() {
        return crtcVerticalTotalAdjustCounterRegister;
    }

    @Override
    public int getCrtcHorizontalSyncWidthCounter() {
        return crtcHorizontalSyncWidthCounter;
    }

    @Override
    public int getCrtcVerticalSyncWidthCounter() {
        return crtcVerticalSyncWidthCounter;
    }

    @Override
    public int getCrtcStateFlags() {
        return crtcStateFlags;
    }

    @Override
    public int getGAVSyncDelayCounter() {
        return gaVSyncDelayCounter;
    }

    @Override
    public int getGAInterruptScanlineCounter() {
        return gaInterruptScanLineCounter;
    }

    @Override
    public int getInterruptRequestedFlag() {
        return interruptRequestedFlag;
    }

    @Override
    public Map<String, SnaChunk> getSnaChunks() {
        return snaChunks;
    }

    public void setFddMotorDriveState(int fddMotorDriveState) {
        this.fddMotorDriveState = fddMotorDriveState;
    }

    public void setFddCurrentPhysicalTrack(int fddCurrentPhysicalTrack) {
        this.fddCurrentPhysicalTrack = fddCurrentPhysicalTrack;
    }

    public void setPrinterDataStrobeRegister(int printerDataStrobeRegister) {
        this.printerDataStrobeRegister = printerDataStrobeRegister;
    }

    public void setCrtcType(int crtcType) {
        this.crtcType = crtcType;
    }

    public void setCrtcHorizontalCharacterCounterRegister(int crtcHorizontalCharacterCounterRegister) {
        this.crtcHorizontalCharacterCounterRegister = crtcHorizontalCharacterCounterRegister;
    }

    public void setCrtcCharacterLineCounterRegister(int crtcCharacterLineCounterRegister) {
        this.crtcCharacterLineCounterRegister = crtcCharacterLineCounterRegister;
    }

    public void setCrtcRasterLineCounterRegister(int crtcRasterLineCounterRegister) {
        this.crtcRasterLineCounterRegister = crtcRasterLineCounterRegister;
    }

    public void setCrtcVerticalTotalAdjustCounterRegister(int crtcVerticalTotalAdjustCounterRegister) {
        this.crtcVerticalTotalAdjustCounterRegister = crtcVerticalTotalAdjustCounterRegister;
    }

    public void setCrtcHorizontalSyncWidthCounter(int crtcHorizontalSyncWidthCounter) {
        this.crtcHorizontalSyncWidthCounter = crtcHorizontalSyncWidthCounter;
    }

    public void setCrtcVerticalSyncWidthCounter(int crtcVerticalSyncWidthCounter) {
        this.crtcVerticalSyncWidthCounter = crtcVerticalSyncWidthCounter;
    }

    public void setCrtcStateFlags(int crtcStateFlags) {
        this.crtcStateFlags = crtcStateFlags;
    }

    public void setGaVSyncDelayCounter(int gaVSyncDelayCounter) {
        this.gaVSyncDelayCounter = gaVSyncDelayCounter;
    }

    public void setGaInterruptScanLineCounter(int gaInterruptScanLineCounter) {
        this.gaInterruptScanLineCounter = gaInterruptScanLineCounter;
    }

    public void setInterruptRequestedFlag(int interruptRequestedFlag) {
        this.interruptRequestedFlag = interruptRequestedFlag;
    }

    public void setSnaChunks(Map<String, SnaChunk> snaChunks) {
        this.snaChunks = snaChunks;
    }

    public static SnaImageV3 fromBuffer(ByteBuffer buffer) throws IOException {
        SnaImageV3 image = new SnaImageV3();
        image.populate(buffer);
        LOGGER.debug("Returning image {}", image);
        return image;
    }


    public void populate(ByteBuffer buffer) throws IOException {
        super.populate(buffer);

        buffer.position(0x9c);

        this.fddMotorDriveState = Byte.toUnsignedInt(buffer.get());
        this.fddCurrentPhysicalTrack = Byte.toUnsignedInt(buffer.get());
        buffer.position(0xa1);
        this.printerDataStrobeRegister = Byte.toUnsignedInt(buffer.get());
        buffer.position(0xa4);
        this.crtcType = Byte.toUnsignedInt(buffer.get());
        buffer.position(0xa9);
        this.crtcHorizontalCharacterCounterRegister = Byte.toUnsignedInt(buffer.get());
        buffer.position(0xab);
        this.crtcCharacterLineCounterRegister = Byte.toUnsignedInt(buffer.get());
        this.crtcRasterLineCounterRegister = Byte.toUnsignedInt(buffer.get());
        this.crtcVerticalTotalAdjustCounterRegister = Byte.toUnsignedInt(buffer.get());
        this.crtcHorizontalSyncWidthCounter = Byte.toUnsignedInt(buffer.get());
        this.crtcVerticalSyncWidthCounter = Byte.toUnsignedInt(buffer.get());
        this.crtcStateFlags = Short.toUnsignedInt(buffer.getShort());
        this.gaVSyncDelayCounter = Byte.toUnsignedInt(buffer.get());
        this.gaInterruptScanLineCounter = Byte.toUnsignedInt(buffer.get());
        this.interruptRequestedFlag = Byte.toUnsignedInt(buffer.get());

        buffer.position(SnaConstants.SNA_HEADER_SIZE + this.memory.length);

        while (buffer.hasRemaining()) {
            SnaChunk chunk = SnaChunk.fromBuffer(buffer);
            LOGGER.debug("Created SNA Chunk {}", chunk);
            if (chunk.getData().length > 0) {
                snaChunks.put(chunk.getName(), chunk);
            } else {
                throw new RuntimeException("No way");
            }
        }
    }

    protected void fillHeader(ByteBuffer header) {
        super.fillHeader(header);

        header.position(0x9c);
        header.put(Integer.valueOf(fddMotorDriveState).byteValue());
        header.putLong(Integer.valueOf(fddCurrentPhysicalTrack).longValue());
        header.put(Integer.valueOf(printerDataStrobeRegister).byteValue());

        header.position(0xa4);
        header.put(Integer.valueOf(crtcType).byteValue());
        header.position(0xa9);
        header.put(Integer.valueOf(crtcHorizontalCharacterCounterRegister).byteValue());
        header.position(0xab);
        header.put(Integer.valueOf(crtcCharacterLineCounterRegister).byteValue());
        header.put(Integer.valueOf(crtcRasterLineCounterRegister).byteValue());
        header.put(Integer.valueOf(crtcVerticalTotalAdjustCounterRegister).byteValue());
        header.put(Integer.valueOf(crtcHorizontalSyncWidthCounter).byteValue());
        header.put(Integer.valueOf(crtcVerticalSyncWidthCounter).byteValue());
        header.putShort(Integer.valueOf(crtcStateFlags).shortValue());
        header.put(Integer.valueOf(gaVSyncDelayCounter).byteValue());
        header.put(Integer.valueOf(gaInterruptScanLineCounter).byteValue());
        header.put(Integer.valueOf(interruptRequestedFlag).byteValue());

    }
    @Override
    public void dump(OutputStream os) throws IOException {
        ByteBuffer header = createHeader();
        fillHeader(header);

        os.write(header.array());
        os.write(memory);

        for (SnaChunk chunk : snaChunks.values()) {
            ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(chunk.getName().getBytes(), 0, 4);
            buffer.putLong(chunk.getData().length);
            os.write(buffer.array());
            SnaCompressedOutputStream zos = new SnaCompressedOutputStream(os);
            zos.write(chunk.getData());
            zos.flush();
        }
    }

    @Override
    public String toString() {
        return "SnaImageV3{" +
                "fddMotorDriveState=" + fddMotorDriveState +
                ", fddCurrentPhysicalTrack=" + fddCurrentPhysicalTrack +
                ", printerDataStrobeRegister=" + printerDataStrobeRegister +
                ", crtcType=" + crtcType +
                ", crtcHorizontalCharacterCounterRegister=" + crtcHorizontalCharacterCounterRegister +
                ", crtcCharacterLineCounterRegister=" + crtcCharacterLineCounterRegister +
                ", crtcRasterLineCounterRegister=" + crtcRasterLineCounterRegister +
                ", crtcVerticalTotalAdjustCounterRegister=" + crtcVerticalTotalAdjustCounterRegister +
                ", crtcHorizontalSyncWidthCounter=" + crtcHorizontalSyncWidthCounter +
                ", crtcVerticalSyncWidthCounter=" + crtcVerticalSyncWidthCounter +
                ", crtcStateFlags=" + crtcStateFlags +
                ", gaVSyncDelayCounter=" + gaVSyncDelayCounter +
                ", gaInterruptScanLineCounter=" + gaInterruptScanLineCounter +
                ", interruptRequestedFlag=" + interruptRequestedFlag +
                ", snaChunks=" + snaChunks +
                ", cpcType=" + cpcType +
                ", interruptNumber=" + interruptNumber +
                ", multimodeBytes=" + Util.dumpAsHexString(multimodeBytes) +
                ", afRegister=" + afRegister +
                ", bcRegister=" + bcRegister +
                ", deRegister=" + deRegister +
                ", hlRegister=" + hlRegister +
                ", ixRegister=" + ixRegister +
                ", iyRegister=" + iyRegister +
                ", iRegister=" + iRegister +
                ", rRegister=" + rRegister +
                ", pc=" + pc +
                ", sp=" + sp +
                ", iff0=" + iff0 +
                ", iff1=" + iff1 +
                ", interruptMode=" + interruptMode +
                ", altAfRegister=" + altAfRegister +
                ", altBcRegister=" + altBcRegister +
                ", altDeRegister=" + altDeRegister +
                ", altHlRegister=" + altHlRegister +
                ", gateArraySelectedPen=" + gateArraySelectedPen +
                ", gateArrayCurrentPalette=" + Util.dumpAsHexString(gateArrayCurrentPalette) +
                ", gateArrayMultiConfiguration=" + gateArrayMultiConfiguration +
                ", currentRamConfiguration=" + currentRamConfiguration +
                ", crtcSelectedRegisterIndex=" + crtcSelectedRegisterIndex +
                ", crtcRegisterData=" + Util.dumpAsHexString(crtcRegisterData) +
                ", currentRomSelection=" + currentRomSelection +
                ", ppiPortA=" + ppiPortA +
                ", ppiPortB=" + ppiPortB +
                ", ppiPortC=" + ppiPortC +
                ", ppiControlPort=" + ppiControlPort +
                ", psgSelectedRegisterIndex=" + psgSelectedRegisterIndex +
                ", psgRegisterData=" + Util.dumpAsHexString(psgRegisterData) +
                ", memoryDumpSize=" + memoryDumpSize +
                ", memory.length=" + memory.length +
                ", snaChunks.size=" + snaChunks.size() +
                '}';
    }
}