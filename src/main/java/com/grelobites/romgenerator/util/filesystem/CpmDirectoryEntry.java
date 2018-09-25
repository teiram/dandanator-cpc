package com.grelobites.romgenerator.util.filesystem;


import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.dsk.FileSystemParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class CpmDirectoryEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpmDirectoryEntry.class);
    private EnumSet<ArchiveFlags> flags = EnumSet.noneOf(ArchiveFlags.class);
    private String name;
    private String extension;
    private int userArea;

    private int extent;
    private int recordCount;
    private int byteCount;
    private int[] allocatedBlocks;


    public CpmDirectoryEntry(String name, String extension, int userArea,
                             EnumSet<ArchiveFlags> flags,
                             int extent, int[] allocatedBlocks,
                             int recordCount, int byteCount) {
        this.name = name;
        this.extension = extension;
        this.extent = extent;
        this.userArea = userArea;
        this.allocatedBlocks = allocatedBlocks;
        this.flags = flags;
        this.recordCount = recordCount;
        this.byteCount = byteCount;
    }

    private static EnumSet<ArchiveFlags> getArchiveFlagsFromExtension(byte[] extension) {
        if (extension.length == 3) {
            List<ArchiveFlags> flags = new ArrayList<>();
            if ((extension[0] & 0x80) != 0) {
                flags.add(ArchiveFlags.READ_ONLY);
            }
            if ((extension[1] & 0x80) != 0) {
                flags.add(ArchiveFlags.SYSTEM);
            }
            if ((extension[2] & 0x80) != 0) {
                flags.add(ArchiveFlags.ARCHIVED);
            }
            return flags.isEmpty() ? EnumSet.noneOf(ArchiveFlags.class) : EnumSet.copyOf(flags);
        } else {
            throw new IllegalArgumentException("File extension with invalid size provided");
        }
    }

    private static byte[] getExtensionByteArray(String extension, EnumSet<ArchiveFlags> flags) {
        byte[] extensionByteArray = Util.paddedByteArray(extension.getBytes(StandardCharsets.US_ASCII),
                CpmConstants.FILEEXTENSION_MAXLENGTH, (byte) 32);
        if (flags.contains(ArchiveFlags.READ_ONLY)) {
            extensionByteArray[0] |= 0x80;
        }
        if (flags.contains(ArchiveFlags.SYSTEM)) {
            extensionByteArray[1] |= 0x80;
        }
        if (flags.contains(ArchiveFlags.ARCHIVED)) {
            extensionByteArray[2] |= 0x80;
        }
        return extensionByteArray;
    }

    private static byte[] getNameByteArray(String name) {
        return Util.paddedByteArray(name.getBytes(StandardCharsets.US_ASCII), 8, (byte) 32);
    }

    private static String stripAttributeBits(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] &= 0x7f;
        }
        return new String(data, StandardCharsets.US_ASCII);
    }

    public static CpmDirectoryEntry fromByteArray(FileSystemParameters parameters, byte[] data) {
        return fromByteArray(parameters, data, 0);
    }

    public static CpmDirectoryEntry fromByteArray(FileSystemParameters parameters, byte[] data, int offset) {
        LOGGER.debug("Creating directory entry from byte array with offset " + offset);
        int userArea;
        byte[] nameByteArray = new byte[CpmConstants.FILENAME_MAXLENGTH];
        byte[] extensionByteArray = new byte[CpmConstants.FILEEXTENSION_MAXLENGTH];
        int byteCount = 0;
        int recordCount = 0;
        int extentLow = 0;
        int extentHigh = 0;
        int extent = 0;
        EnumSet<ArchiveFlags> flags = EnumSet.noneOf(ArchiveFlags.class);
        boolean bigDisk = parameters.getBlockCount() > 255;

        int[] allocatedBlocks = new int[bigDisk ? 8 : 16];

        ByteBuffer byteBuffer = ByteBuffer.wrap(data)
                .order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.position(offset);

        userArea = byteBuffer.get();
        if (userArea != CpmConstants.UNUSED_ENTRY_USER) {
            byteBuffer.get(nameByteArray)
                    .get(extensionByteArray);
            extentLow = byteBuffer.get();
            byteCount = byteBuffer.get();
            extentHigh = byteBuffer.get();
            recordCount = byteBuffer.get() & 0xff;
            if (recordCount == 0) {
                recordCount = 128;  //CP/M 2.2 compatibility
            }
            if (bigDisk) {
                short[] blocks = new short[8];
                byteBuffer.asShortBuffer().get(blocks);
                for (int i = 0; i < blocks.length; i++) {
                    allocatedBlocks[i] = blocks[i];
                }
            } else {
                byte[] blocks = new byte[16];
                byteBuffer.get(blocks);
                for (int i = 0; i < blocks.length; i++) {
                    allocatedBlocks[i] = blocks[i] & 0xff; //Avoid sign
                }
            }

            extent = ((extentHigh & 0x3F) >> 3) | (extentLow & 0x1F);
            flags = getArchiveFlagsFromExtension(extensionByteArray);
        }
        return new CpmDirectoryEntry(stripAttributeBits(nameByteArray),
                stripAttributeBits(extensionByteArray),
                userArea, flags, extent,
                allocatedBlocks,
                recordCount, byteCount);
    }

    public byte[] asByteArray(FileSystemParameters parameters) {
        boolean bigDisk = parameters.getBlockCount() > 255;

        ByteBuffer buffer = ByteBuffer.allocate(CpmConstants.DIRECTORY_ENTRY_SIZE)
                .put(Integer.valueOf(userArea).byteValue())
                .put(getNameByteArray(name))
                .put(getExtensionByteArray(extension, flags))
                .put(Integer.valueOf(extent & 0x1F).byteValue())
                .put(Integer.valueOf(byteCount).byteValue())
                .put(Integer.valueOf((extent & 0x03E0) >> 5).byteValue())
                .put(Integer.valueOf(recordCount).byteValue());
        for (int i = 0; i < allocatedBlocks.length; i++) {
            if (bigDisk) {
                buffer.putShort(Integer.valueOf(allocatedBlocks[i]).shortValue());
            } else {
                buffer.put(Integer.valueOf(allocatedBlocks[i]).byteValue());
            }
        }
        return buffer.array();
    }

    public EnumSet<ArchiveFlags> getFlags() {
        return flags;
    }

    public void setFlags(EnumSet<ArchiveFlags> flags) {
        this.flags = flags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getUserArea() {
        return userArea;
    }

    public void setUserArea(int userArea) {
        this.userArea = userArea;
    }

    public int getExtent() {
        return extent;
    }

    public void setExtent(int extent) {
        this.extent = extent;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public int getByteCount() {
        return byteCount;
    }

    public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }

    public int[] getAllocatedBlocks() {
        return allocatedBlocks;
    }

    public void setAllocatedBlocks(int[] allocatedBlocks) {
        this.allocatedBlocks = allocatedBlocks;
    }

    @Override
    public String toString() {
        return "CpmDirectoryEntry{" +
                "flags=" + flags +
                ", name='" + name + '\'' +
                ", extension='" + extension + '\'' +
                ", userArea=" + userArea +
                ", extent=" + extent +
                ", recordCount=" + recordCount +
                ", byteCount=" + byteCount +
                ", allocatedBlocks=" + Arrays.toString(allocatedBlocks) +
                '}';
    }
}
