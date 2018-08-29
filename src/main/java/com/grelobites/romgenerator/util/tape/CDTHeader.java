package com.grelobites.romgenerator.util.tape;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

public class CDTHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CDTHeader.class);

    private static final byte[] MAGIC_HEADER = {0x5A, 0x58, 0x54, 0x61, 0x70, 0x65, 0x21, 0x1A};
    private static final int CDT_HEADER_SIZE = 10;

    private final int majorVersion;
    private final int minorVersion;

    private CDTHeader(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public int majorVersion() {
        return majorVersion;
    }

    public int minorVersion() {
        return minorVersion;
    }

    public static Optional<CDTHeader> fromInputStream(InputStream is) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(is, CDT_HEADER_SIZE))
                    .order(ByteOrder.LITTLE_ENDIAN);
            byte[] header = new byte[MAGIC_HEADER.length];
            buffer.get(header);
            if (Arrays.equals(MAGIC_HEADER, header)) {
                int majorVersion = buffer.get();
                int minorVersion = buffer.get();
                return Optional.of(new CDTHeader(majorVersion, minorVersion));
            } else {
                LOGGER.warn("Unexpected CDT header found: {}", Util.dumpAsHexString(header));
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to get a CDTHeader from stream", e);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "CDTHeader{" +
                "majorVersion=" + majorVersion +
                ", minorVersion=" + minorVersion +
                '}';
    }
}
