package com.grelobites.romgenerator.util.dsk;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Track {
    private TrackInformationBlock trackInformationBlock;
    private byte[][] data;

    public Track(TrackInformationBlock trackInformationBlock) {
        this.trackInformationBlock = trackInformationBlock;
        data = new byte[trackInformationBlock.getSectorCount()][trackInformationBlock.getSectorSize()];
    }

    public void setSectorData(int sector, byte[] data) {
        this.data[sector] = data;
    }

    public byte[] getSectorData(int sector) {
        return this.data[sector];
    }

    public List<Integer> orderedSectorList() {
        return Stream.of(trackInformationBlock.getSectorInformationList())
                .sorted(Comparator.comparingInt(SectorInformationBlock::getSectorId))
                .map(e -> e.getPhysicalPosition())
                .collect(Collectors.toList());
    }

    public TrackInformationBlock getInformation() {
        return trackInformationBlock;
    }
}
