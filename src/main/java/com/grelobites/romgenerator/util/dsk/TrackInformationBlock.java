package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TrackInformationBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackInformationBlock.class);
    private static final byte[] TRACK_HEADER = "Track-Info\r\n".getBytes();

    public static final int BLOCK_SIZE = 256;

    private int trackNumber;
    private int sideNumber;
    private int sectorSize;
    private int sectorCount;
    private int gap3Length;
    private int fillerByte;
    private SectorInformationBlock[] sectorInformationList;

    public static class Builder {
        private TrackInformationBlock block = new TrackInformationBlock();

        public Builder withTrackNumber(int trackNumber) {
            block.trackNumber = trackNumber;
            return this;
        }

        public Builder withSideNumber(int sideNumber) {
            block.sideNumber = sideNumber;
            return this;
        }

        public Builder withSectorSize(int sectorSize) {
            block.sectorSize = sectorSize;
            return this;
        }

        public Builder withSectorCount(int sectorCount) {
            block.sectorCount = sectorCount;
            return this;
        }

        public Builder withGap3Length(int gap3Length) {
            block.gap3Length = gap3Length;
            return this;
        }

        public Builder withFillerByte(int fillerByte) {
            block.fillerByte = fillerByte;
            return this;
        }

        public Builder withSectorInformationList(int startSector) {
            SectorInformationBlock sectorInfos[] = new SectorInformationBlock[block.sectorCount];
            for (int i = 0; i < sectorInfos.length; i++) {
                sectorInfos[i] = SectorInformationBlock.builder()
                        .withSectorId(startSector++)
                        .withSectorSize(block.sectorSize)
                        .withSide(block.sideNumber)
                        .withTrack(block.trackNumber)
                        .build();
            }
            block.sectorInformationList = sectorInfos;
            return this;
        }

        public TrackInformationBlock build() {
            return block;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TrackInformationBlock fromInputStream(InputStream data) throws IOException {
        TrackInformationBlock block = fromByteArray(Util.fromInputStream(data, 0x100));
        LOGGER.trace("Track information is " + block);

        return block;
    }

    private static TrackInformationBlock fromByteArray(byte[] data) {
        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        byte[] trackHeader = new byte[12];
        header.get(trackHeader);
        if (!Arrays.equals(TRACK_HEADER, trackHeader)) {
            LOGGER.error("Track Header doesn't match expected DSK contents: "
                + new String(trackHeader));
            throw new IllegalArgumentException("Invalid track header");
        }

        header.position(0x10);

        TrackInformationBlock block = new TrackInformationBlock();
        block.trackNumber = header.get();
        block.sideNumber = header.get();
        header.getShort();
        block.sectorSize = 128 << header.get();
        block.sectorCount = header.get();
        block.gap3Length = header.get();
        block.fillerByte = header.get();

        block.sectorInformationList = new SectorInformationBlock[block.sectorCount];
        byte[] sectorInfoBytes = new byte[8];
        for (int i = 0; i < block.sectorCount; i++) {
            header.get(sectorInfoBytes);
            SectorInformationBlock sectorInformationBlock = SectorInformationBlock.fromByteArray(sectorInfoBytes);
            sectorInformationBlock.setPhysicalPosition(i);
            block.sectorInformationList[i] = sectorInformationBlock;
        }

        return block;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(TRACK_HEADER);
        buffer.position(buffer.position() + 4);
        buffer.put(Integer.valueOf(trackNumber).byteValue());
        buffer.put(Integer.valueOf(sideNumber).byteValue());
        buffer.position(buffer.position() + 2);
        buffer.put(Integer.valueOf(sectorSize >>> 8).byteValue());
        buffer.put(Integer.valueOf(sectorCount).byteValue());
        buffer.put(Integer.valueOf(gap3Length).byteValue());
        buffer.put(Integer.valueOf(fillerByte).byteValue());
        for (SectorInformationBlock block : sectorInformationList) {
            buffer.put(block.toByteArray());
        }
        return buffer.array();
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public int getSideNumber() {
        return sideNumber;
    }

    public int getSectorSize() {
        return sectorSize;
    }

    public int getSectorCount() {
        return sectorCount;
    }

    public int getGap3Length() {
        return gap3Length;
    }

    public int getFillerByte() {
        return fillerByte;
    }

    public SectorInformationBlock[] getSectorInformationList() {
        return sectorInformationList;
    }

    public SectorInformationBlock getSectorInformation(int index) {
        return sectorInformationList[index];
    }

    @Override
    public String toString() {
        return "TrackInformationBlock{" +
                "trackNumber=" + trackNumber +
                ", sideNumber=" + sideNumber +
                ", sectorSize=" + sectorSize +
                ", sectorCount=" + sectorCount +
                ", gap3Length=" + gap3Length +
                ", fillerByte=" + fillerByte +
                ", sectorInformationList=" + Arrays.toString(sectorInformationList) +
                '}';
    }
}
