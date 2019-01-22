package com.grelobites.romgenerator.dsk;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.dsk.*;
import com.grelobites.romgenerator.util.filesystem.AmsdosHeader;
import com.grelobites.romgenerator.util.filesystem.Archive;
import com.grelobites.romgenerator.util.filesystem.CpmFileSystem;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class RescueDskTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(RescueDskTests.class);
    @Test
    public void generateRescueDsk() throws IOException {
        byte[] rescueEewriter = Constants.getRescueEewriter();
        AmsdosHeader header = AmsdosHeader.builder()
                .withName("RESCUE")
                .withExtension("BIN")
                .withExecAddress(0x6000)
                .withLoadAddress(0x6000)
                .withLogicalLength(rescueEewriter.length)
                .withFileLength(rescueEewriter.length)
                .withType(AmsdosHeader.Type.BINARY)
                .build();
        Archive archive = new Archive("RESCUE", "BIN", 0,
                Util.concatArrays(header.toByteArray(), rescueEewriter));
        FileSystemParameters parameters = DskConstants.CPC_DATA_FS_PARAMETERS;
        CpmFileSystem fileSystem = new CpmFileSystem(parameters);
        fileSystem.addArchive(archive);
        int trackDataSize = parameters.getSectorsByTrack() * parameters.getSectorSize();
        DiskInformationBlock diskInformationBlock = DiskInformationBlock.builder()
                .standard()
                .withSideCount(1)
                .withTrackCount(parameters.getTrackCount())
                .withTrackSize(TrackInformationBlock.BLOCK_SIZE + trackDataSize)
                .build();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(diskInformationBlock.toByteArray());
        LOGGER.debug("Adding Disk Info {}", diskInformationBlock);
        byte[] fsData = fileSystem.asByteArray();

        for (int i = 0; i < parameters.getTrackCount(); i++) {
            TrackInformationBlock trackInfo = TrackInformationBlock.builder()
                    .withTrackNumber(i)
                    .withSectorCount(parameters.getSectorsByTrack())
                    .withSectorSize(parameters.getSectorSize())
                    .withSideNumber(0)
                    .withSectorInformationList(0xC1)
                    .withFillerByte(0xE5)
                    .withGap3Length(0x4E)
                    .build();
            LOGGER.debug("Adding track info {}", trackInfo);
            bos.write(trackInfo.toByteArray());
            bos.write(fsData, trackDataSize * i, trackDataSize);
        }
        try (FileOutputStream fos = new FileOutputStream("output/rescue.dsk")) {
            fos.write(bos.toByteArray());
        }
    }
}
