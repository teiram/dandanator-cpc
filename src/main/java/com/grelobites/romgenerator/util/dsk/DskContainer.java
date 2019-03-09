package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DskContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskContainer.class);
    private DiskInformationBlock diskInformationBlock;
    private Track[] tracks;

    private DskContainer(DiskInformationBlock diskInformationBlock,
                         Track[] tracks) {
        this.diskInformationBlock = diskInformationBlock;
        this.tracks = tracks;
    }

    public static DskContainer emptyDisk(FileSystemParameters parameters) {
        Track[] tracks = new Track[parameters.getTrackCount()];
        for (int i = 0; i < tracks.length; i++) {
            TrackInformationBlock trackInfo = TrackInformationBlock.builder()
                    .withTrackNumber(i)
                    .withSideNumber(0)
                    .withSectorSize(parameters.getSectorSize())
                    .withSectorCount(parameters.getSectorsByTrack())
                    .withSectorInformationList(0xc1) //Check this
                    .build();
            tracks[i] = new Track(trackInfo);
            LOGGER.debug("Created track {}", tracks[i]);
            for (int j = 0; j < parameters.getSectorsByTrack(); j++) {
                tracks[i].setSectorData(j, new byte[parameters.getSectorSize()]);
            }
        }
        DiskInformationBlock diskInformationBlock = DiskInformationBlock.builder()
                .withSideCount(1)
                .withTrackCount(parameters.getTrackCount())
                .withTrackSize(parameters.getSectorSize() * parameters.getSectorsByTrack())
                .build();
        return new DskContainer(diskInformationBlock, tracks);
    }

    public static DskContainer fromInputStream(InputStream data) throws IOException {
        DiskInformationBlock diskInformationBlock = DiskInformationBlock.fromInputStream(data);
        LOGGER.debug("Disk Information block: " + diskInformationBlock);
        Track[] tracks = new Track[diskInformationBlock.getTrackCount()];
        int i = 0;
        for (i = 0; i < diskInformationBlock.getTrackCount(); i++) {
            try {
                TrackInformationBlock trackInformationBlock = TrackInformationBlock.fromInputStream(data);
                Track track = new Track(trackInformationBlock);
                for (int j = 0; j < trackInformationBlock.getSectorCount(); j++) {
                    track.setSectorData(j, Util.fromInputStream(data, trackInformationBlock.getSectorSize()));
                }
                tracks[trackInformationBlock.getTrackNumber()] = track;
            } catch (Exception e) {
                LOGGER.warn("Unable to read track from Dsk container", e);
            }
        }
        return new DskContainer(diskInformationBlock, tracks);
    }

    public void dumpRawData(OutputStream os) throws IOException {
        for (Track track : tracks) {
            LOGGER.trace("Dumping information for track " + track.getInformation());
            for (int i : track.orderedSectorList()) {
                LOGGER.trace("Dumping data for sector " + track.getInformation().getSectorInformation(i));
                os.write(track.getSectorData(i));
            }
        }
    }

    public void populateWithRawData(FileSystemParameters parameters, InputStream is) throws IOException {
        boolean eof = false;
        int trackId = parameters.getReservedTracks();
        while (!eof) {
            LOGGER.debug("Populating data on track {}", trackId);
            Track track = tracks[trackId];
            for (int i : track.orderedSectorList()) {
                byte[] buffer = new byte[parameters.getSectorSize()];
                int byteCount = is.read(buffer);
                if (byteCount == buffer.length) {
                    LOGGER.debug("Populating sector {} with {} bytes", i, byteCount);

                    track.setSectorData(i, buffer);
                } else {
                    LOGGER.debug("Got only {} bytes from read. Assuming EOF", byteCount);
                    eof = true;
                    break;
                }
            }
            if (++trackId == parameters.getTrackCount()) {
                eof = true;
            }
        }
    }

    public DiskInformationBlock getDiskInformation() {
        return diskInformationBlock;
    }

    public Track getTrack(int index) {
        return tracks[index];
    }
}
