package com.grelobites.romgenerator.util.tape;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

public class CdtHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdtHeader.class);
    private static final int DEFAULT_MAJOR_VERSION = 1;
    private static final int DEFAULT_MINOR_VERSION = 12;
    private static final byte[] MAGIC_HEADER = {0x5A, 0x58, 0x54, 0x61, 0x70, 0x65, 0x21, 0x1A};
    private static final int CDT_HEADER_SIZE = 10;

    private final int majorVersion;
    private final int minorVersion;

    public CdtHeader() {
        this(DEFAULT_MAJOR_VERSION, DEFAULT_MINOR_VERSION);
    }

    public CdtHeader(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public int majorVersion() {
        return majorVersion;
    }

    public int minorVersion() {
        return minorVersion;
    }

    public void dump(OutputStream os) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(CDT_HEADER_SIZE);
        buffer.put(MAGIC_HEADER);
        buffer.put(Integer.valueOf(majorVersion).byteValue());
        buffer.put(Integer.valueOf(minorVersion).byteValue());
        os.write(buffer.array());
    }

    public static Optional<CdtHeader> fromInputStream(InputStream is) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(is, CDT_HEADER_SIZE))
                    .order(ByteOrder.LITTLE_ENDIAN);
            byte[] header = new byte[MAGIC_HEADER.length];
            buffer.get(header);
            if (Arrays.equals(MAGIC_HEADER, header)) {
                int majorVersion = buffer.get();
                int minorVersion = buffer.get();
                return Optional.of(new CdtHeader(majorVersion, minorVersion));
            } else {
                LOGGER.warn("Unexpected CDT header found: {}", Util.dumpAsHexString(header));
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to get a CdtHeader from stream", e);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "CdtHeader{" +
                "majorVersion=" + majorVersion +
                ", minorVersion=" + minorVersion +
                '}';
    }
}
