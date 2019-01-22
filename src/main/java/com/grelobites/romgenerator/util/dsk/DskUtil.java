package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.filesystem.AmsdosHeader;
import com.grelobites.romgenerator.util.filesystem.Archive;
import com.grelobites.romgenerator.util.filesystem.CpmConstants;
import com.grelobites.romgenerator.util.filesystem.CpmFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DskUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskUtil.class);

    private static class DiskSpec {
        private final int formatId;
        private final int sides;
        private final int tracksBySide;
        private final int sectorsByTrack;
        private final int physicalSectorShift;
        private final int reservedTracks;
        private final int blockShift;
        private final int directoryBlocks;

        public static DiskSpec fromData(byte[] data) {
            return new DiskSpec(data);
        }

        private DiskSpec(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            formatId = buffer.get();
            sides = buffer.get();
            tracksBySide = buffer.get();
            sectorsByTrack = buffer.get();
            physicalSectorShift = buffer.get();
            reservedTracks = buffer.get();
            blockShift = buffer.get();
            directoryBlocks = buffer.get();
        }

        public int getFormatId() {
            return formatId;
        }

        public int getSides() {
            return sides;
        }

        public int getTracksBySide() {
            return tracksBySide;
        }

        public int getSectorsByTrack() {
            return sectorsByTrack;
        }

        public int getPhysicalSectorShift() {
            return physicalSectorShift;
        }

        public int getReservedTracks() {
            return reservedTracks;
        }

        public int getBlockShift() {
            return blockShift;
        }

        public int getDirectoryBlocks() {
            return directoryBlocks;
        }

        @Override
        public String toString() {
            return "DiskSpec{" +
                    "formatId=" + formatId +
                    ", sides=" + sides +
                    ", tracksBySide=" + tracksBySide +
                    ", sectorsByTrack=" + sectorsByTrack +
                    ", physicalSectorShift=" + physicalSectorShift +
                    ", reservedTracks=" + reservedTracks +
                    ", blockShift=" + blockShift +
                    ", directoryBlocks=" + directoryBlocks +
                    '}';
        }
    }

    public static FileSystemParameters guessFileSystemParameters(DskContainer dsk) {
        int sectorId = dsk.getTrack(0)
                .getInformation().getSectorInformation(0).getSectorId();
        if (sectorId == 0x41) {
            return DskConstants.CPC_SYSTEM_FS_PARAMETERS;
        } else if (sectorId == 0xC1) {
            return DskConstants.CPC_DATA_FS_PARAMETERS;
        }

        //Check the spec now
        byte [] specData = Arrays.copyOf(dsk.getTrack(0).getSectorData(1), 16);
        if (specData[0] == CpmConstants.EMPTY_BYTE) {
            return DskConstants.PLUS3_FS_PARAMETERS;
        } else {
            DiskSpec spec = DiskSpec.fromData(specData);
            LOGGER.debug("Using Disk spec to guess format " + spec);
            int blockSize = 128 << spec.getBlockShift();
            int blockShift = spec.getBlockShift() - spec.getPhysicalSectorShift();
            return FileSystemParameters.newBuilder()
                    .withBlockCount(spec.getSides() * spec.getTracksBySide() * spec.getSectorsByTrack())
                    .withBlockSize(blockSize)
                    .withReservedTracks(spec.getReservedTracks())
                    .withDirectoryEntries((spec.getDirectoryBlocks() * blockSize) / CpmConstants.DIRECTORY_ENTRY_SIZE)
                    .withSectorSize(128 << spec.getPhysicalSectorShift())
                    .withSectorsByTrack(spec.getSectorsByTrack())
                    .withTrackCount(spec.getTracksBySide() * spec.getSides())
                    .build();
        }
    }

    public static boolean isDskFile(File dskFile) {
        try (FileInputStream fis = new FileInputStream(dskFile)) {
            DiskInformationBlock diskInformationBlock =
                DiskInformationBlock.fromInputStream(fis);
            String magic = diskInformationBlock.getMagic();
            return  magic.startsWith(DiskInformationBlock.STANDARD_DSK_PREFIX) ||
                    magic.startsWith(DiskInformationBlock.EXTENDED_DSK_PREFIX);
        } catch (Exception e) {
            LOGGER.error("Checking DSK file", e);
            return false;
        }
    }


}
