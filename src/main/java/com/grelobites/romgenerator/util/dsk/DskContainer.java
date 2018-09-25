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

    public static DskContainer fromInputStream(InputStream data) throws IOException {
        DiskInformationBlock diskInformationBlock = DiskInformationBlock.fromInputStream(data);
        LOGGER.debug("Disk Information block: " + diskInformationBlock);
        Track[] tracks = new Track[diskInformationBlock.getTrackCount()];
        for (int i = 0; i < diskInformationBlock.getTrackCount(); i++) {
            TrackInformationBlock trackInformationBlock = TrackInformationBlock.fromInputStream(data);
            Track track = new Track(trackInformationBlock);
            for (int j = 0; j < trackInformationBlock.getSectorCount(); j++) {
                track.setSectorData(j, Util.fromInputStream(data, trackInformationBlock.getSectorSize()));
            }
            tracks[i] = track;
        }
        return new DskContainer(diskInformationBlock, tracks);
    }

    public void dumpRawData(OutputStream os) throws IOException {
        for (Track track : tracks) {
            LOGGER.debug("Dumping information for track " + track.getInformation());
            for (int i : track.orderedSectorList()) {
                LOGGER.debug("Dumping data for sector " + track.getInformation().getSectorInformation(i));
                os.write(track.getSectorData(i));
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
