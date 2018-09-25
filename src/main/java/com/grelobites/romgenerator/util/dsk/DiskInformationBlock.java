package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class DiskInformationBlock {
    public static final String STANDARD_DSK_PREFIX = "MV - CPC";
    public static final String EXTENDED_DSK_PREFIX = "EXTENDED";
    private String magic;
    private String creator;
    private int trackCount;
    private int sideCount;
    private int trackSize;

    public static DiskInformationBlock fromInputStream(InputStream data) throws IOException {
        return fromByteArray(Util.fromInputStream(data, 0x100));
    }

    public static DiskInformationBlock fromByteArray(byte[] data) {
        ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        DiskInformationBlock block = new DiskInformationBlock();
        byte[] magicBytes = new byte[34];
        header.get(magicBytes);
        byte[] creatorBytes = new byte[14];
        header.get(creatorBytes);
        block.magic = new String(magicBytes, 0, 32, Charset.defaultCharset());
        block.creator = new String(creatorBytes, Charset.defaultCharset());
        block.trackCount = header.get();
        block.sideCount = header.get();
        block.trackSize = header.getShort();

        return block;
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
