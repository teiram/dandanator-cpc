package com.grelobites.romgenerator.util.tape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BinaryRecordIterator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryRecordIterator.class);

    private static final int SEGMENT_SIZE = 256;
    private static final int RECORD_MAX_SEGMENTS = 8;
    private static final int RECORD_MAX_SIZE = SEGMENT_SIZE * RECORD_MAX_SEGMENTS;
    private static final int TYPE_SIZE = 1;
    private static final int TRAILER_SIZE = 4;
    private static final int CRC_SIZE = 2;
    private static final int PAYLOAD_SIZE = TYPE_SIZE + SEGMENT_SIZE + CRC_SIZE + TRAILER_SIZE;
    private static final int HEADER_ID = 0x2C;
    private static final int DATA_ID = 0x16;
    private static final int FILE_TYPE_BINARY = 2;
    private Binary binary;
    private boolean dumpHeader;
    private int currentRecord;

    public BinaryRecordIterator(Binary binary) {
        this.binary = binary;
        this.dumpHeader = true;
        this.currentRecord = 0;
    }

    private static int crc16(byte[] data, int offset, int len) {
        int crc = 0xffff;
        for (int p = 0; p < len; p++) {
            crc ^= (data[p + offset] & 0xff) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = ((crc << 1) & 0xffff) ^ 0x1021;
                } else {
                    crc = (crc << 1) & 0xffff;
                }
            }
        }
        return crc ^ 0xffff;
    }


    private byte[] getPaddedName(String name) {
        byte[] result = new byte[16];
        System.arraycopy(name.getBytes(), 0, result, 0, name.length());
        return result;
    }

    public byte[] next() {
        int remaining = binary.getData().length - RECORD_MAX_SIZE * currentRecord;
        LOGGER.debug("Asking for record with remaining {}", remaining);
        if (remaining > 0) {
            int dataLength = Math.min(remaining, RECORD_MAX_SIZE);
            if (dumpHeader) {
                ByteBuffer buffer = ByteBuffer.allocate(PAYLOAD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
                buffer.put(Integer.valueOf(HEADER_ID).byteValue());
                buffer.put(getPaddedName(binary.getName()));
                buffer.put(Integer.valueOf(currentRecord).byteValue());
                buffer.put(Integer.valueOf(remaining <= RECORD_MAX_SIZE ? 255 : 0).byteValue()); //Last block?
                buffer.put(Integer.valueOf(FILE_TYPE_BINARY).byteValue());
                buffer.putShort(Integer.valueOf(dataLength).shortValue());
                buffer.putShort(Integer.valueOf(binary.getLoadAddress()).shortValue());
                buffer.put(Integer.valueOf(currentRecord == 0 ? 255 : 0).byteValue()); //First block?

                buffer.putShort(Integer.valueOf(binary.getData().length).shortValue());
                buffer.putShort(Integer.valueOf(binary.getExecAddress()).shortValue());

                buffer.position(TYPE_SIZE + SEGMENT_SIZE);
                buffer.order(ByteOrder.BIG_ENDIAN);
                buffer.putShort(Integer.valueOf(crc16(buffer.array(), TYPE_SIZE, SEGMENT_SIZE)).shortValue());
                buffer.putInt(0xFFFFFFFF);
                dumpHeader = false;
                return buffer.array();
            } else {
                int segments = (dataLength + SEGMENT_SIZE - 1) / SEGMENT_SIZE;
                LOGGER.debug("segments {}, dataLength {}, currentRecord {}", segments, dataLength, currentRecord);
                ByteBuffer buffer = ByteBuffer.allocate(TYPE_SIZE + (segments * (SEGMENT_SIZE + CRC_SIZE)) + TRAILER_SIZE)
                        .order(ByteOrder.BIG_ENDIAN);
                buffer.put(Integer.valueOf(DATA_ID).byteValue());
                int baseOffset = currentRecord * RECORD_MAX_SIZE;
                for (int i = 0; i < segments; i++) {
                    LOGGER.debug("Writing segment with remaining {}", remaining);
                    buffer.put(binary.getData(), baseOffset + SEGMENT_SIZE * i, Math.min(SEGMENT_SIZE, remaining));
                    remaining -= SEGMENT_SIZE;
                    LOGGER.debug("CRC to position {}", TYPE_SIZE + SEGMENT_SIZE + (i > 0 ? i * (SEGMENT_SIZE + CRC_SIZE) : 0));
                    buffer.position(TYPE_SIZE + SEGMENT_SIZE + (i > 0 ? i * (SEGMENT_SIZE + CRC_SIZE) : 0));
                    int crc = crc16(buffer.array(), i * (SEGMENT_SIZE + CRC_SIZE) + TYPE_SIZE, SEGMENT_SIZE);
                    LOGGER.debug("Calculated CRC {}", String.format("0x%04x", crc));
                    buffer.putShort(Integer.valueOf(crc).shortValue());
                }
                buffer.position(TYPE_SIZE + segments * (SEGMENT_SIZE + CRC_SIZE));
                buffer.putInt(0xFFFFFFFF);
                dumpHeader = true;
                currentRecord++;
                return buffer.array();
            }
        } else {
            return null;
        }
    }
}
