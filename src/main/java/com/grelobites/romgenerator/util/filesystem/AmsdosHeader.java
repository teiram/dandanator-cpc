package com.grelobites.romgenerator.util.filesystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/*
 * Content of an AMSDOS Header:
	user            1            Offset 00      (0x00)
	name            8            Offset 01      (0x01-0x08)
	extension       3            Offset 09      (0x09-0x0b)
	unused0         4            Offset 12      (0x0c-0x0f)
	block_number    1            Offset 16      (0x10)
	last_block      1            Offset 17      (0x11)
	type;           1            Offset 18      (0x12)
	data_length     2            Offset 19      (0x13-0x14)
	load_address    2            Offset 21      (0x15-0x16)
	first_block     1            Offset 23      (0x17)
	logical_length  2            Offset 24      (0x18-0x19)
	exec_address    2            Offset 26      (0x1a-0x1b)
	unused1        36            Offset 28      (0x1c-0x3f)
	file_length     2            Offset 64      (0x40-0x41)
	unused2         1            Offset 66      (0x42)
	checksum        2            Offset 67      (0x43-0x44)
	unused3        59            Offset 69      (0x45-0x80)
 */
public class AmsdosHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmsdosHeader.class);

    public enum Type {
        BASIC(0),
        PROTECTED(1),
        BINARY(2),
        UNKNOWN(null);

        private Integer id;
        Type(Integer id) {
            this.id = id;
        }

        public static Type fromId(int id) {
            for (Type type : Type.values()) {
                if (id == type.id) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    private static final int AMSDOS_HEADER_LENGTH = 0x80;
    private static final int AMSDOS_NAME_LENGTH = 8;
    private static final int AMSDOS_EXTENSION_LENGTH = 3;
    private int user;
    private String name;
    private String extension;
    private int blockNumber;
    private int lastBlock;
    private Type type;
    private int dataLength;
    private int loadAddress;
    private int firstBlock;
    private int logicalLength;
    private int execAddress;
    private int fileLength;
    private int checksum;

    static int calculateChecksum(byte[] header) {
        int checksum = 0;
        for (int i = 0; i < 67; i++) {
            checksum += header[i] & 0xff;
            checksum &= 0xffff;
        }
        return checksum;
    }

    public static Optional<AmsdosHeader> fromByteArray(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data, 0, AMSDOS_HEADER_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN);
        byte[] nameBytes = new byte[AMSDOS_NAME_LENGTH];
        byte[] extensionBytes = new byte[AMSDOS_EXTENSION_LENGTH];
        AmsdosHeader header = new AmsdosHeader();
        header.user = Byte.valueOf(byteBuffer.get()).intValue();
        byteBuffer.get(nameBytes);
        byteBuffer.get(extensionBytes);
        header.name = new String(nameBytes, StandardCharsets.US_ASCII);
        header.extension = new String(extensionBytes, StandardCharsets.US_ASCII);
        byteBuffer.position(byteBuffer.position() + 4); //Unused0
        header.blockNumber = Byte.valueOf(byteBuffer.get()).intValue();
        header.lastBlock = Byte.valueOf(byteBuffer.get()).intValue();
        header.type = Type.fromId(Byte.valueOf(byteBuffer.get()).intValue());
        header.dataLength = Short.valueOf(byteBuffer.getShort()).intValue();
        header.loadAddress = Short.valueOf(byteBuffer.getShort()).intValue();
        header.firstBlock = Byte.valueOf(byteBuffer.get()).intValue();
        header.logicalLength = Short.valueOf(byteBuffer.getShort()).intValue();
        header.execAddress = Short.valueOf(byteBuffer.getShort()).intValue();
        byteBuffer.position(byteBuffer.position() + 36); //Unused1
        header.fileLength = Short.valueOf(byteBuffer.getShort()).intValue();
        byteBuffer.position(byteBuffer.position() + 1); //Unused2
        header.checksum = Short.valueOf(byteBuffer.getShort()).intValue();
        LOGGER.debug("AMSDOS Header {}, calculated checksum {}", header,
                String.format("0x%04x", calculateChecksum(data)));
        if (header.checksum == calculateChecksum(data)) {
            return Optional.of(header);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<AmsdosHeader> fromArchive(Archive archive) {
        return fromByteArray(archive.getData());
    }

    @Override
    public String toString() {
        return "AmsdosHeader{" +
                "user=" + user +
                ", name='" + name + '\'' +
                ", extension='" + extension + '\'' +
                ", blockNumber=" + blockNumber +
                ", lastBlock=" + lastBlock +
                ", type=" + type +
                ", dataLength=" + dataLength +
                ", loadAddress=" + String.format("0x%02x", loadAddress & 0xffff) +
                ", firstBlock=" + firstBlock +
                ", logicalLength=" + logicalLength +
                ", execAddress=" + String.format("0x%02x", execAddress & 0xffff) +
                ", fileLength=" + fileLength +
                ", checksum=" + String.format("0x%02x", checksum & 0xffff) +
                '}';
    }
}
