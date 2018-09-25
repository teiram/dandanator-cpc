package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.util.filesystem.CpmConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemParameters {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemParameters.class);
    private int sectorSize;
    private int trackCount;
    private int sectorsByTrack;
    private int blockSize;
    private int directoryEntries;
    private int blockCount;
    private int reservedTracks;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        FileSystemParameters parameters = new FileSystemParameters();

        public Builder withSectorSize(int sectorSize) {
            parameters.setSectorSize(sectorSize);
            return this;
        }

        public Builder withTrackCount(int trackCount) {
            parameters.setTrackCount(trackCount);
            return this;
        }

        public Builder withSectorsByTrack(int sectorsByTrack) {
            parameters.setSectorsByTrack(sectorsByTrack);
            return this;
        }

        public Builder withBlockSize(int blockSize) {
            parameters.setBlockSize(blockSize);
            return this;
        }

        public Builder withDirectoryEntries(int directoryEntries) {
            parameters.setDirectoryEntries(directoryEntries);
            return this;
        }

        public Builder withBlockCount(int blockCount) {
            parameters.setBlockCount(blockCount);
            return this;
        }

        public Builder withReservedTracks(int reservedTracks) {
            parameters.setReservedTracks(reservedTracks);
            return this;
        }

        public FileSystemParameters build() {
            int allocatedBlockEntries = parameters.getBlockCount() > 255 ? 8 : 16;
            if (allocatedBlockEntries * parameters.getBlockSize() < CpmConstants.LOGICAL_EXTENT_SIZE) {
                LOGGER.error("Invalid blockSize/blockCount  combination: " + parameters);
                throw new IllegalArgumentException("Invalid blockSize/blockCount combination");
            }
            /*
            if (parameters.getBlockSize() * parameters.getBlockCount() <
                    parameters.getTrackCount() * parameters.getSectorsByTrack() * parameters.getSectorSize()) {
                LOGGER.error("Not enough tracks to hold the block count " + parameters);
                throw new IllegalArgumentException("Invalid trackCount/blockCount combination");
            }
            */
            return parameters;
        }
    }

    public int getSectorSize() {
        return sectorSize;
    }

    public void setSectorSize(int sectorSize) {
        this.sectorSize = sectorSize;
    }

    public int getTrackCount() {
        return trackCount;
    }

    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    public int getSectorsByTrack() {
        return sectorsByTrack;
    }

    public void setSectorsByTrack(int sectorsByTrack) {
        this.sectorsByTrack = sectorsByTrack;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public int getDirectoryEntries() {
        return directoryEntries;
    }

    public void setDirectoryEntries(int directoryEntries) {
        this.directoryEntries = directoryEntries;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public int getReservedTracks() {
        return reservedTracks;
    }

    public void setReservedTracks(int reservedTracks) {
        this.reservedTracks = reservedTracks;
    }

    @Override
    public String toString() {
        return "FileSystemParameters{" +
                "sectorSize=" + sectorSize +
                ", trackCount=" + trackCount +
                ", sectorsByTrack=" + sectorsByTrack +
                ", blockSize=" + blockSize +
                ", directoryEntries=" + directoryEntries +
                ", blockCount=" + blockCount +
                ", reservedTracks=" + reservedTracks +
                '}';
    }
}
