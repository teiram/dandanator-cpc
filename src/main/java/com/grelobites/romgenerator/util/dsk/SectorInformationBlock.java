package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SectorInformationBlock {
    private static final int BLOCK_SIZE = 8;
    private int track;
    private int side;
    private int sectorId;
    private int sectorSize;
    private int fdcStatusRegister1;
    private int fdcStatusRegister2;
    private int physicalPosition;

    public static class Builder {
        private SectorInformationBlock block = new SectorInformationBlock();

        public Builder withTrack(int track) {
            block.track = track;
            return this;
        }

        public Builder withSide(int side) {
            block.side = side;
            return this;
        }

        public Builder withSectorId(int sectorId) {
            block.sectorId = sectorId;
            return this;
        }

        public Builder withSectorSize(int sectorSize) {
            block.sectorSize = sectorSize;
            return this;
        }

        public Builder withFdcStatusRegister1(int fdcStatusRegister1) {
            block.fdcStatusRegister1 = fdcStatusRegister1;
            return this;
        }

        public Builder withFdcStatusRegister2(int fdcStatusRegister2) {
            block.fdcStatusRegister2 = fdcStatusRegister2;
            return this;
        }

        public Builder withPhysicalPosition(int physicalPosition) {
            block.physicalPosition = physicalPosition;
            return this;
        }

        public SectorInformationBlock build() {
            return block;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

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

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Integer.valueOf(track).byteValue());
        buffer.put(Integer.valueOf(side).byteValue());
        buffer.put(Integer.valueOf(sectorId).byteValue());
        buffer.put(Integer.valueOf(sectorSize >>> 8).byteValue());
        buffer.put(Integer.valueOf(fdcStatusRegister1).byteValue());
        buffer.put(Integer.valueOf(fdcStatusRegister2).byteValue());
        return buffer.array();
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
