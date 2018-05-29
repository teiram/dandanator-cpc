package com.grelobites.romgenerator.util.sna;

import com.grelobites.romgenerator.util.compress.sna.SnaCompressedOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SnaImageV1 extends SnaImageBase implements SnaImage {

    protected int afRegister;
    protected int bcRegister;
    protected int deRegister;
    protected int hlRegister;
    protected int ixRegister;
    protected int iyRegister;
    protected int iRegister;
    protected int rRegister;
    protected int pc;
    protected int sp;
    protected int iff0;
    protected int iff1;
    protected int interruptMode;
    protected int altAfRegister;
    protected int altBcRegister;
    protected int altDeRegister;
    protected int altHlRegister;

    protected int gateArraySelectedPen;
    protected byte[] gateArrayCurrentPalette;
    protected int gateArrayMultiConfiguration;

    protected int currentRamConfiguration;

    protected int crtcSelectedRegisterIndex;
    protected byte[] crtcRegisterData;

    protected int currentRomSelection;

    protected int ppiPortA;
    protected int ppiPortB;
    protected int ppiPortC;
    protected int ppiControlPort;

    protected int psgSelectedRegisterIndex;
    protected byte[] psgRegisterData;

    protected int memoryDumpSize;
    protected byte[] memory;

    @Override
    public int getSnapshotVersion() {
        return 1;
    }

    @Override
    public int getAFRegister() {
        return afRegister;
    }

    @Override
    public int getBCRegister() {
        return bcRegister;
    }

    @Override
    public int getDERegister() {
        return deRegister;
    }

    @Override
    public int getHLRegister() {
        return hlRegister;
    }

    @Override
    public int getIXRegister() {
        return ixRegister;
    }

    @Override
    public int getIYRegister() {
        return iyRegister;
    }

    @Override
    public int getIRegister() {
        return iRegister;
    }

    @Override
    public int getRRegister() {
        return rRegister;
    }

    @Override
    public int getPC() {
        return pc;
    }

    @Override
    public int getSP() {
        return sp;
    }

    @Override
    public int getIFF0() {
        return iff0;
    }

    @Override
    public int getIFF1() {
        return iff1;
    }

    @Override
    public int getInterruptMode() {
        return interruptMode;
    }

    @Override
    public int getAltAFRegister() {
        return altAfRegister;
    }

    @Override
    public int getAltBCRegister() {
        return altBcRegister;
    }

    @Override
    public int getAltDERegister() {
        return altDeRegister;
    }

    @Override
    public int getAltHLRegister() {
        return altHlRegister;
    }

    @Override
    public int getGateArraySelectedPen() {
        return gateArraySelectedPen;
    }

    @Override
    public byte[] getGateArrayCurrentPalette() {
        return gateArrayCurrentPalette;
    }

    @Override
    public int getGateArrayMultiConfiguration() {
        return gateArrayMultiConfiguration;
    }

    @Override
    public int getCurrentRamConfiguration() {
        return currentRamConfiguration;
    }

    @Override
    public int getCrtcSelectedRegisterIndex() {
        return crtcSelectedRegisterIndex;
    }

    @Override
    public byte[] getCrtcRegisterData() {
        return crtcRegisterData;
    }

    @Override
    public int getCurrentRomSelection() {
        return currentRomSelection;
    }

    @Override
    public int getPpiPortA() {
        return ppiPortA;
    }

    @Override
    public int getPpiPortB() {
        return ppiPortB;
    }

    @Override
    public int getPpiPortC() {
        return ppiPortC;
    }

    @Override
    public int getPpiControlPort() {
        return ppiControlPort;
    }

    @Override
    public int getPsgSelectedRegisterIndex() {
        return psgSelectedRegisterIndex;
    }

    @Override
    public byte[] getPsgRegisterData() {
        return psgRegisterData;
    }

    @Override
    public int getMemoryDumpSize() {
        return memoryDumpSize;
    }

    @Override
    public byte[] getMemoryDump() {
        return memory;
    }

    public void setAfRegister(int afRegister) {
        this.afRegister = afRegister;
    }

    public void setBcRegister(int bcRegister) {
        this.bcRegister = bcRegister;
    }

    public void setDeRegister(int deRegister) {
        this.deRegister = deRegister;
    }

    public void setHlRegister(int hlRegister) {
        this.hlRegister = hlRegister;
    }

    public void setIxRegister(int ixRegister) {
        this.ixRegister = ixRegister;
    }

    public void setIyRegister(int iyRegister) {
        this.iyRegister = iyRegister;
    }

    public void setiRegister(int iRegister) {
        this.iRegister = iRegister;
    }

    public void setrRegister(int rRegister) {
        this.rRegister = rRegister;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public void setSp(int sp) {
        this.sp = sp;
    }

    public void setIff0(int iff0) {
        this.iff0 = iff0;
    }

    public void setIff1(int iff1) {
        this.iff1 = iff1;
    }

    public void setInterruptMode(int interruptMode) {
        this.interruptMode = interruptMode;
    }

    public void setAltAfRegister(int altAfRegister) {
        this.altAfRegister = altAfRegister;
    }

    public void setAltBcRegister(int altBcRegister) {
        this.altBcRegister = altBcRegister;
    }

    public void setAltDeRegister(int altDeRegister) {
        this.altDeRegister = altDeRegister;
    }

    public void setAltHlRegister(int altHlRegister) {
        this.altHlRegister = altHlRegister;
    }

    public void setGateArraySelectedPen(int gateArraySelectedPen) {
        this.gateArraySelectedPen = gateArraySelectedPen;
    }

    public void setGateArrayCurrentPalette(byte[] gateArrayCurrentPalette) {
        this.gateArrayCurrentPalette = gateArrayCurrentPalette;
    }

    public void setGateArrayMultiConfiguration(int gateArrayMultiConfiguration) {
        this.gateArrayMultiConfiguration = gateArrayMultiConfiguration;
    }

    public void setCurrentRamConfiguration(int currentRamConfiguration) {
        this.currentRamConfiguration = currentRamConfiguration;
    }

    public void setCrtcSelectedRegisterIndex(int crtcSelectedRegisterIndex) {
        this.crtcSelectedRegisterIndex = crtcSelectedRegisterIndex;
    }

    public void setCrtcRegisterData(byte[] crtcRegisterData) {
        this.crtcRegisterData = crtcRegisterData;
    }

    public void setCurrentRomSelection(int currentRomSelection) {
        this.currentRomSelection = currentRomSelection;
    }

    public void setPpiPortA(int ppiPortA) {
        this.ppiPortA = ppiPortA;
    }

    public void setPpiPortB(int ppiPortB) {
        this.ppiPortB = ppiPortB;
    }

    public void setPpiPortC(int ppiPortC) {
        this.ppiPortC = ppiPortC;
    }

    public void setPpiControlPort(int ppiControlPort) {
        this.ppiControlPort = ppiControlPort;
    }

    public void setPsgSelectedRegisterIndex(int psgSelectedRegisterIndex) {
        this.psgSelectedRegisterIndex = psgSelectedRegisterIndex;
    }

    public void setPsgRegisterData(byte[] psgRegisterData) {
        this.psgRegisterData = psgRegisterData;
    }

    public void setMemory(byte[] memory) {
        this.memory = memory;
    }

    public void populate(ByteBuffer buffer) throws IOException {
        buffer.position(17);

        this.afRegister = Short.toUnsignedInt(buffer.getShort());
        this.bcRegister = Short.toUnsignedInt(buffer.getShort());
        this.deRegister = Short.toUnsignedInt(buffer.getShort());
        this.hlRegister = Short.toUnsignedInt(buffer.getShort());
        this.rRegister = Byte.toUnsignedInt(buffer.get());
        this.iRegister = Byte.toUnsignedInt(buffer.get());
        this.iff0 = Byte.toUnsignedInt(buffer.get());
        this.iff1 = Byte.toUnsignedInt(buffer.get());
        this.ixRegister = Short.toUnsignedInt(buffer.getShort());
        this.iyRegister = Short.toUnsignedInt(buffer.getShort());

        this.sp = Short.toUnsignedInt(buffer.getShort());
        this.pc = Short.toUnsignedInt(buffer.getShort());
        this.interruptMode = Byte.toUnsignedInt(buffer.get());

        this.altAfRegister = Short.toUnsignedInt(buffer.getShort());
        this.altBcRegister = Short.toUnsignedInt(buffer.getShort());
        this.altDeRegister = Short.toUnsignedInt(buffer.getShort());
        this.altHlRegister = Short.toUnsignedInt(buffer.getShort());

        this.gateArraySelectedPen = Byte.toUnsignedInt(buffer.get());
        this.gateArrayCurrentPalette = new byte[17];
        buffer.get(this.gateArrayCurrentPalette);
        this.gateArrayMultiConfiguration = Byte.toUnsignedInt(buffer.get());
        this.currentRamConfiguration = Byte.toUnsignedInt(buffer.get());

        this.crtcSelectedRegisterIndex = Byte.toUnsignedInt(buffer.get());
        this.crtcRegisterData = new byte[18];
        buffer.get(this.crtcRegisterData);
        this.currentRomSelection = Byte.toUnsignedInt(buffer.get());

        this.ppiPortA = Byte.toUnsignedInt(buffer.get());
        this.ppiPortB = Byte.toUnsignedInt(buffer.get());
        this.ppiPortC = Byte.toUnsignedInt(buffer.get());
        this.ppiControlPort = Byte.toUnsignedInt(buffer.get());

        this.psgSelectedRegisterIndex = Byte.toUnsignedInt(buffer.get());
        buffer.get(this.psgRegisterData);

        this.memoryDumpSize = Short.toUnsignedInt(buffer.getShort());

        this.memory = new byte[memoryDumpSize * 1024];
        buffer.position(256);
        buffer.get(this.memory);
    }
    /*
     *
     */
    public static SnaImageV1 fromBuffer(ByteBuffer buffer) throws IOException {
        SnaImageV1 image = new SnaImageV1();
        image.populate(buffer);
        return image;
    }

    protected ByteBuffer createHeader() {
        return ByteBuffer.allocate(SnaConstants.SNA_HEADER_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    protected void fillHeader(ByteBuffer header) {
        header.put(SnaConstants.SNA_SIGNATURE.getBytes());
        header.position(0x10);
        header.put(Integer.valueOf(getSnapshotVersion()).byteValue());
        header.putShort(Integer.valueOf(afRegister).shortValue());
        header.putShort(Integer.valueOf(bcRegister).shortValue());
        header.putShort(Integer.valueOf(deRegister).shortValue());
        header.putShort(Integer.valueOf(hlRegister).shortValue());
        header.put(Integer.valueOf(rRegister).byteValue());
        header.put(Integer.valueOf(iRegister).byteValue());
        header.put(Integer.valueOf(iff0).byteValue());
        header.put(Integer.valueOf(iff1).byteValue());
        header.putShort(Integer.valueOf(ixRegister).shortValue());
        header.putShort(Integer.valueOf(iyRegister).shortValue());
        header.putShort(Integer.valueOf(sp).shortValue());
        header.putShort(Integer.valueOf(pc).shortValue());
        header.put(Integer.valueOf(interruptMode).byteValue());
        header.putShort(Integer.valueOf(altAfRegister).shortValue());
        header.putShort(Integer.valueOf(altBcRegister).shortValue());
        header.putShort(Integer.valueOf(altDeRegister).shortValue());
        header.putShort(Integer.valueOf(altHlRegister).shortValue());

        header.put(Integer.valueOf(gateArraySelectedPen).byteValue());
        header.put(gateArrayCurrentPalette, 0, 17);
        header.put(Integer.valueOf(gateArrayMultiConfiguration).byteValue());
        header.put(Integer.valueOf(currentRamConfiguration).byteValue());
        header.put(Integer.valueOf(crtcSelectedRegisterIndex).byteValue());
        header.put(crtcRegisterData, 0, 18);
        header.put(Integer.valueOf(currentRomSelection).byteValue());

        header.put(Integer.valueOf(ppiPortA).byteValue());
        header.put(Integer.valueOf(ppiPortB).byteValue());
        header.put(Integer.valueOf(ppiPortC).byteValue());
        header.put(Integer.valueOf(ppiControlPort).byteValue());

        header.put(Integer.valueOf(psgSelectedRegisterIndex).byteValue());
        header.put(psgRegisterData, 0, 16);
        header.putShort(Integer.valueOf(memoryDumpSize).shortValue()); //Dump size
    }

    @Override
    public void dump(OutputStream os) throws IOException {
        ByteBuffer header = createHeader();
        fillHeader(header);
        os.write(header.array());
        os.write(memory);
    }


}
