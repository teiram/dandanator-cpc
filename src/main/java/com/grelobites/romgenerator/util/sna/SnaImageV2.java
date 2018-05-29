package com.grelobites.romgenerator.util.sna;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class SnaImageV2 extends SnaImageV1 implements SnaImage {
    protected int cpcType;
    protected int interruptNumber;
    protected byte[] multimodeBytes = SnaImageBase.DEFAULT_MULTIMODE_BYTES;

    @Override
    public int getSnapshotVersion() {
        return 2;
    }

    @Override
    public int getCpcType() {
        return cpcType;
    }

    @Override
    public int getInterruptNumber() {
        return interruptNumber;
    }

    @Override
    public byte[] getMultimodeBytes() {
        return multimodeBytes;
    }

    public void setCpcType(int cpcType) {
        this.cpcType = cpcType;
    }

    public void setInterruptNumber(int interruptNumber) {
        this.interruptNumber = interruptNumber;
    }

    public void setMultimodeBytes(byte[] multimodeBytes) {
        this.multimodeBytes = multimodeBytes;
    }

    public void populate(ByteBuffer buffer) throws IOException {
        super.populate(buffer);

        buffer.position(0x6d);
        this.cpcType = Byte.toUnsignedInt(buffer.get());
        this.interruptNumber = Byte.toUnsignedInt(buffer.get());
        this.multimodeBytes = new byte[6];
        buffer.get(multimodeBytes);
    }

    public static SnaImageV2 fromBuffer(ByteBuffer buffer) throws IOException {
        SnaImageV2 image = new SnaImageV2();
        image.populate(buffer);
        return image;
    }

    protected void fillHeader(ByteBuffer header) {
        super.fillHeader(header);
        header.position(0x6d);
        header.put(Integer.valueOf(cpcType).byteValue());
        header.put(Integer.valueOf(interruptNumber).byteValue());
        header.put(multimodeBytes, 0, 6);
    }

    @Override
    public void dump(OutputStream os) throws IOException {
        ByteBuffer header = createHeader();
        fillHeader(header);
        os.write(header.array());
        os.write(memory);
    }

}

