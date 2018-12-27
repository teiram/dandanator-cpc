package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SectorInformationBlock {

    private int track;
    private int side;
    private int sectorId;
    private int sectorSize;
    private int fdcStatusRegister1;
    private int fdcStatusRegister2;
    private int physicalPosition;

    public static SectorInformationBlock fromInputStream(InputStream data) throws IOException {
        return fromByteArray(Util.fromInputStream(data, 8));
    }

    public static SectorInformationBlock fromByteArray(byte[] data) {
        SectorInformationBlock block = new SectorInformationBlock();
        ByteBuffer header = ByteBuffer.wrap(data);
        block.track = header.get();
        block.side = header.get();
        block.sectorId = header.get() & 0xff;
        block.sectorSize = header.get();
        block.fdcStatusRegister1 = header.get();
        block.fdcStatusRegister2 = header.get();

        return block;
    }

    public int getTrack() {
        return track;
    }

    public int getSide() {
        return side;
    }

    public int getSectorId() {
        return sectorId;
    }

    public int getSectorSize() {
        return sectorSize;
    }

    public int getFdcStatusRegister1() {
        return fdcStatusRegister1;
    }

    public int getFdcStatusRegister2() {
        return fdcStatusRegister2;
    }

    public int getPhysicalPosition() {
        return physicalPosition;
    }

    public void setPhysicalPosition(int physicalPosition) {
        this.physicalPosition = physicalPosition;
    }

    @Override
    public String toString() {
        return "SectorInformationBlock{" +
                "track=" + track +
                ", side=" + side +
                ", sectorId=0x" + String.format("%02x", sectorId) +
                ", sectorSize=" + sectorSize +
                ", fdcStatusRegister1=" + fdcStatusRegister1 +
                ", fdcStatusRegister2=" + fdcStatusRegister2 +
                ", physicalPosition=" + physicalPosition +
                '}';
    }
}
