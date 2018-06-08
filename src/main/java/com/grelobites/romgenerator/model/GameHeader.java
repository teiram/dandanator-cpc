package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.util.sna.SnaChunk;
import com.grelobites.romgenerator.util.sna.SnaImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

public class GameHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameHeader.class);

    private int snapshotVersion;
    private int afRegister;
    private int bcRegister;
    private int deRegister;
    private int hlRegister;
    private int ixRegister;
    private int iyRegister;
    private int iRegister;
    private int rRegister;
    private int pc;
    private int sp;
    private int iff0;
    private int interruptMode;
    private int altAfRegister;
    private int altBcRegister;
    private int altDeRegister;
    private int altHlRegister;

    private int gateArraySelectedPen;
    private byte[] gateArrayCurrentPalette = new byte[17];
    private int gateArrayMultiConfiguration;

    private int currentRamConfiguration;

    private int crtcSelectedRegisterIndex;
    private byte[] crtcRegisterData = new byte[16];

    private int currentRomSelection;

    private int ppiPortA;
    private int ppiPortB;
    private int ppiPortC;
    private int ppiControlPort;

    private int psgSelectedRegisterIndex;
    private byte[] psgRegisterData;

    private int memoryDumpSize;
    private int cpcType;
    private int fddMotorDriveState;

    public int getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(int snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public int getAfRegister() {
        return afRegister;
    }

    public void setAfRegister(int afRegister) {
        this.afRegister = afRegister;
    }

    public int getBcRegister() {
        return bcRegister;
    }

    public void setBcRegister(int bcRegister) {
        this.bcRegister = bcRegister;
    }

    public int getDeRegister() {
        return deRegister;
    }

    public void setDeRegister(int deRegister) {
        this.deRegister = deRegister;
    }

    public int getHlRegister() {
        return hlRegister;
    }

    public void setHlRegister(int hlRegister) {
        this.hlRegister = hlRegister;
    }

    public int getIxRegister() {
        return ixRegister;
    }

    public void setIxRegister(int ixRegister) {
        this.ixRegister = ixRegister;
    }

    public int getIyRegister() {
        return iyRegister;
    }

    public void setIyRegister(int iyRegister) {
        this.iyRegister = iyRegister;
    }

    public int getiRegister() {
        return iRegister;
    }

    public void setiRegister(int iRegister) {
        this.iRegister = iRegister;
    }

    public int getrRegister() {
        return rRegister;
    }

    public void setrRegister(int rRegister) {
        this.rRegister = rRegister;
    }

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public int getSp() {
        return sp;
    }

    public void setSp(int sp) {
        this.sp = sp;
    }

    public int getIff0() {
        return iff0;
    }

    public void setIff0(int iff0) {
        this.iff0 = iff0;
    }

    public int getInterruptMode() {
        return interruptMode;
    }

    public void setInterruptMode(int interruptMode) {
        this.interruptMode = interruptMode;
    }

    public int getAltAfRegister() {
        return altAfRegister;
    }

    public void setAltAfRegister(int altAfRegister) {
        this.altAfRegister = altAfRegister;
    }

    public int getAltBcRegister() {
        return altBcRegister;
    }

    public void setAltBcRegister(int altBcRegister) {
        this.altBcRegister = altBcRegister;
    }

    public int getAltDeRegister() {
        return altDeRegister;
    }

    public void setAltDeRegister(int altDeRegister) {
        this.altDeRegister = altDeRegister;
    }

    public int getAltHlRegister() {
        return altHlRegister;
    }

    public void setAltHlRegister(int altHlRegister) {
        this.altHlRegister = altHlRegister;
    }

    public int getGateArraySelectedPen() {
        return gateArraySelectedPen;
    }

    public void setGateArraySelectedPen(int gateArraySelectedPen) {
        this.gateArraySelectedPen = gateArraySelectedPen;
    }

    public byte[] getGateArrayCurrentPalette() {
        return gateArrayCurrentPalette;
    }

    public void setGateArrayCurrentPalette(byte[] gateArrayCurrentPalette) {
        this.gateArrayCurrentPalette = gateArrayCurrentPalette;
    }

    public int getGateArrayMultiConfiguration() {
        return gateArrayMultiConfiguration;
    }

    public void setGateArrayMultiConfiguration(int gateArrayMultiConfiguration) {
        this.gateArrayMultiConfiguration = gateArrayMultiConfiguration;
    }

    public int getCurrentRamConfiguration() {
        return currentRamConfiguration;
    }

    public void setCurrentRamConfiguration(int currentRamConfiguration) {
        this.currentRamConfiguration = currentRamConfiguration;
    }

    public int getCrtcSelectedRegisterIndex() {
        return crtcSelectedRegisterIndex;
    }

    public void setCrtcSelectedRegisterIndex(int crtcSelectedRegisterIndex) {
        this.crtcSelectedRegisterIndex = crtcSelectedRegisterIndex;
    }

    public byte[] getCrtcRegisterData() {
        return crtcRegisterData;
    }

    public void setCrtcRegisterData(byte[] crtcRegisterData) {
        this.crtcRegisterData = crtcRegisterData;
    }

    public int getCurrentRomSelection() {
        return currentRomSelection;
    }

    public void setCurrentRomSelection(int currentRomSelection) {
        this.currentRomSelection = currentRomSelection;
    }

    public int getPpiPortA() {
        return ppiPortA;
    }

    public void setPpiPortA(int ppiPortA) {
        this.ppiPortA = ppiPortA;
    }

    public int getPpiPortB() {
        return ppiPortB;
    }

    public void setPpiPortB(int ppiPortB) {
        this.ppiPortB = ppiPortB;
    }

    public int getPpiPortC() {
        return ppiPortC;
    }

    public void setPpiPortC(int ppiPortC) {
        this.ppiPortC = ppiPortC;
    }

    public int getPpiControlPort() {
        return ppiControlPort;
    }

    public void setPpiControlPort(int ppiControlPort) {
        this.ppiControlPort = ppiControlPort;
    }

    public int getPsgSelectedRegisterIndex() {
        return psgSelectedRegisterIndex;
    }

    public void setPsgSelectedRegisterIndex(int psgSelectedRegisterIndex) {
        this.psgSelectedRegisterIndex = psgSelectedRegisterIndex;
    }

    public byte[] getPsgRegisterData() {
        return psgRegisterData;
    }

    public void setPsgRegisterData(byte[] psgRegisterData) {
        this.psgRegisterData = psgRegisterData;
    }

    public int getMemoryDumpSize() {
        return memoryDumpSize;
    }

    public void setMemoryDumpSize(int memoryDumpSize) {
        this.memoryDumpSize = memoryDumpSize;
    }

    public int getCpcType() {
        return cpcType;
    }

    public void setCpcType(int cpcType) {
        this.cpcType = cpcType;
    }

    public int getFddMotorDriveState() {
        return fddMotorDriveState;
    }

    public void setFddMotorDriveState(int fddMotorDriveState) {
        this.fddMotorDriveState = fddMotorDriveState;
    }

    public static GameHeader fromSnaImage(SnaImage snaImage) {
        GameHeader header = new GameHeader();
        header.setSnapshotVersion(snaImage.getSnapshotVersion());

        header.setAfRegister(snaImage.getAFRegister());
        header.setBcRegister(snaImage.getBCRegister());
        header.setDeRegister(snaImage.getDERegister());
        header.setHlRegister(snaImage.getHLRegister());
        header.setIxRegister(snaImage.getIXRegister());
        header.setIyRegister(snaImage.getIYRegister());
        header.setiRegister(snaImage.getIRegister());
        header.setrRegister(snaImage.getRRegister());
        header.setPc(snaImage.getPC());
        header.setSp(snaImage.getSP());

        header.setIff0(snaImage.getIFF0());
        header.setInterruptMode(snaImage.getInterruptMode());

        header.setAltAfRegister(snaImage.getAltAFRegister());
        header.setAltBcRegister(snaImage.getAltBCRegister());
        header.setAltDeRegister(snaImage.getAltDERegister());
        header.setAltHlRegister(snaImage.getAltHLRegister());
        header.setGateArraySelectedPen(snaImage.getGateArraySelectedPen());
        header.setGateArrayCurrentPalette(snaImage.getGateArrayCurrentPalette());
        header.setGateArrayMultiConfiguration(snaImage.getGateArrayMultiConfiguration());
        header.setCurrentRamConfiguration(snaImage.getCurrentRamConfiguration());
        header.setCrtcSelectedRegisterIndex(snaImage.getCrtcSelectedRegisterIndex());

        //Last two registers are read-only
        header.setCrtcRegisterData(Arrays.copyOfRange(snaImage.getCrtcRegisterData(),
            0, 16));

        header.setCurrentRomSelection(snaImage.getCurrentRomSelection());
        header.setPpiPortA(snaImage.getPpiPortA());
        header.setPpiPortB(snaImage.getPpiPortB());
        header.setPpiPortC(snaImage.getPpiPortC());
        header.setPpiControlPort(snaImage.getPpiControlPort());
        header.setPsgSelectedRegisterIndex(snaImage.getPsgSelectedRegisterIndex());
        header.setPsgRegisterData(snaImage.getPsgRegisterData());

        header.setCpcType(snaImage.getCpcType());
        header.setFddMotorDriveState(snaImage.getFddMotorDriveState());

        int imageSize = snaImage.getMemoryDumpSize();
        boolean mem0Declared = imageSize >= 64;
        boolean mem1Declared = imageSize >= 128;
        for (Map.Entry<String, SnaChunk> chunkEntry : snaImage.getSnaChunks().entrySet()) {

            if (SnaChunk.CHUNK_MEM0.equals(chunkEntry.getKey())) {
                imageSize += mem0Declared ? 0 : 64;
            } else if (SnaChunk.CHUNK_MEM1.equals(chunkEntry.getKey())) {
                imageSize += mem1Declared ? 0 : 64;
            } else if (chunkEntry.getKey().startsWith("MEM")) {
                LOGGER.warn("Unsupported SNA with extra memory definitions");
                throw new IllegalArgumentException("SNA with RAM expansions");
            }
        }
        header.setMemoryDumpSize(imageSize);
        return header;
    }

    @Override
    public String toString() {
        return "GameHeader{" +
                "snapshotVersion=" + snapshotVersion +
                ", afRegister=0x" + String.format("%04x", afRegister) +
                ", bcRegister=0x" + String.format("%04x", bcRegister) +
                ", deRegister=0x" + String.format("%04x", deRegister) +
                ", hlRegister=0x" + String.format("%04x", hlRegister) +
                ", ixRegister=0x" + String.format("%04x", ixRegister) +
                ", iyRegister=0x" + String.format("%04x", iyRegister) +
                ", iRegister=0x" + String.format("%02x", iRegister) +
                ", rRegister=0x" + String.format("%02x", rRegister) +
                ", pc=0x" + String.format("%04x", pc) +
                ", sp=0x" + String.format("%04x", sp) +
                ", iff0=" + iff0 +
                ", interruptMode=" + interruptMode +
                ", altAfRegister=0x" + String.format("%04x", altAfRegister) +
                ", altBcRegister=0x" + String.format("%04x", altBcRegister) +
                ", altDeRegister=0x" + String.format("%04x", altDeRegister) +
                ", altHlRegister=0x" + String.format("%04x", altHlRegister) +
                ", gateArraySelectedPen=" + gateArraySelectedPen +
                ", gateArrayCurrentPalette=" + Arrays.toString(gateArrayCurrentPalette) +
                ", gateArrayMultiConfiguration=" + gateArrayMultiConfiguration +
                ", currentRamConfiguration=0x" + currentRamConfiguration +
                ", crtcSelectedRegisterIndex=" + crtcSelectedRegisterIndex +
                ", crtcRegisterData=" + Arrays.toString(crtcRegisterData) +
                ", currentRomSelection=" + currentRomSelection +
                ", ppiPortA=" + ppiPortA +
                ", ppiPortB=" + ppiPortB +
                ", ppiPortC=" + ppiPortC +
                ", ppiControlPort=" + ppiControlPort +
                ", psgSelectedRegisterIndex=" + psgSelectedRegisterIndex +
                ", psgRegisterData=" + Arrays.toString(psgRegisterData) +
                ", memoryDumpSize=" + memoryDumpSize +
                ", cpcType=" + cpcType +
                ", fddMotorDriveState=" + fddMotorDriveState +
                '}';
    }
}
