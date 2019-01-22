package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DiskInformationBlock {
    public static final String STANDARD_DSK_PREFIX = "MV - CPC";
    public static final String EXTENDED_DSK_PREFIX = "EXTENDED";
    public static final int BLOCK_SIZE = 256;
    private static final int MAGIC_SIZE = 34;
    private static final int CREATOR_SIZE = 14;
    private static final String STANDARD_DSK_MAGIC = "MV - CPCEMU Disk-File\r\nDisk-Info\r\n";
    private static final String EXTENDED_DSK_MAGIC = "EXTENDED CPC DSK File\r\nDisk-Info\r\n";
    private static final String CREATOR = "Dandanator";
    private String magic;
    private String creator;
    private int trackCount;
    private int sideCount;
    private int trackSize;

    public static class Builder {
        DiskInformationBlock diskInformationBlock = new DiskInformationBlock();
        public Builder standard() {
            diskInformationBlock.magic = STANDARD_DSK_MAGIC;
            return this;
        }

        public Builder extended() {
            diskInformationBlock.magic = EXTENDED_DSK_MAGIC;
            return this;
        }

        public Builder withTrackCount(int trackCount) {
            diskInformationBlock.trackCount = trackCount;
            return this;
        }

        public Builder withSideCount(int sideCount) {
            diskInformationBlock.sideCount = sideCount;
            return this;
        }

        public Builder withTrackSize(int trackSize) {
            diskInformationBlock.trackSize = trackSize;
            return this;
        }

        public DiskInformationBlock build() {
            diskInformationBlock.creator = CREATOR;
            return diskInformationBlock;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
    public static DiskInformationBlock fromInputStream(InputStream data) throws IOException {
        return fromByteArray(Util.fromInputStream(data, 0x100));
    }

    public static DiskInformationBlock fromByteArray(byte[] data) {
        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        DiskInformationBlock block = new DiskInformationBlock();
        byte[] magicBytes = new byte[MAGIC_SIZE];
        header.get(magicBytes);
        byte[] creatorBytes = new byte[CREATOR_SIZE];
        header.get(creatorBytes);
        block.magic = new String(magicBytes, 0, 32, StandardCharsets.US_ASCII);
        block.creator = new String(creatorBytes, StandardCharsets.US_ASCII);
        block.trackCount = header.get();
        block.sideCount = header.get();
        block.trackSize = header.getShort();

        return block;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Util.paddedString(magic, MAGIC_SIZE, ' ').getBytes(StandardCharsets.US_ASCII));
        buffer.put(Util.paddedString(creator, CREATOR_SIZE, ' ').getBytes(StandardCharsets.US_ASCII));
        buffer.put(Integer.valueOf(trackCount).byteValue());
        buffer.put(Integer.valueOf(sideCount).byteValue());
        buffer.putShort(Integer.valueOf(trackSize).shortValue());
        return buffer.array();
    }

    public String getMagic() {
        return magic;
    }

    public String getCreator() {
        return creator;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public int getSideCount() {
        return sideCount;
    }

    public int getTrackSize() {
        return trackSize;
    }

    @Override
    public String toString() {
        return "DiskInformationBlock{" +
                "magic='" + magic + '\'' +
                ", creator='" + creator + '\'' +
                ", trackCount=" + trackCount +
                ", sideCount=" + sideCount +
                ", trackSize=" + trackSize +
                '}';
    }
}
