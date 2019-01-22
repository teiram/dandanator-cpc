package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.dsk.DiskInformationBlock;
import com.grelobites.romgenerator.util.dsk.DskConstants;
import com.grelobites.romgenerator.util.dsk.FileSystemParameters;
import com.grelobites.romgenerator.util.dsk.TrackInformationBlock;
import com.grelobites.romgenerator.util.filesystem.AmsdosHeader;
import com.grelobites.romgenerator.util.filesystem.Archive;
import com.grelobites.romgenerator.util.filesystem.CpmFileSystem;
import com.grelobites.romgenerator.util.tape.Binary;
import com.grelobites.romgenerator.util.tape.CdtBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RescueUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RescueUtil.class);
    private static final int RESCUE_LOADER_LOAD_ADDR = 0x4000;
    private static final int RESCUE_LOADER_EXEC_ADDR = 0x4000;
    private static final String RESCUE_LOADER_NAME = "RESCUE";
    private static final String RESCUE_LOADER_EXTENSION = "BIN";
    private static final String TAPE_RESCUE_LOADER_NAME = "RESCUE EEWRITER";
    private static final int RESCUE_LOADER_USER_AREA = 0;
    private static final int FILLER_BYTE = 0xE5;
    private static final int GAP3_LENGTH = 0x4E;
    private static final int FIRST_SECTOR_ID = 0xC1;
    private static final int DISK_SIDES = 1;

    public static void generateRescueDisk(OutputStream stream) throws IOException {
        byte [] rescueEewriter = Constants.getRescueEewriter();
        AmsdosHeader header = AmsdosHeader.builder()
                .withName(RESCUE_LOADER_NAME)
                .withExtension(RESCUE_LOADER_EXTENSION)
                .withExecAddress(RESCUE_LOADER_EXEC_ADDR)
                .withLoadAddress(RESCUE_LOADER_LOAD_ADDR)
                .withLogicalLength(rescueEewriter.length)
                .withFileLength(rescueEewriter.length)
                .withType(AmsdosHeader.Type.BINARY)
                .build();
        Archive archive = new Archive(RESCUE_LOADER_NAME, RESCUE_LOADER_EXTENSION,
                RESCUE_LOADER_USER_AREA,
                Util.concatArrays(
                        header.toByteArray(),
                        rescueEewriter));
        FileSystemParameters parameters = DskConstants.CPC_DATA_FS_PARAMETERS;
        CpmFileSystem fileSystem = new CpmFileSystem(parameters);
        fileSystem.addArchive(archive);
        int trackDataSize = parameters.getSectorsByTrack() * parameters.getSectorSize();

        DiskInformationBlock diskInformationBlock = DiskInformationBlock.builder()
                .standard()
                .withSideCount(DISK_SIDES)
                .withTrackCount(parameters.getTrackCount())
                .withTrackSize(TrackInformationBlock.BLOCK_SIZE + trackDataSize)
                .build();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(diskInformationBlock.toByteArray());
        byte[] fsData = fileSystem.asByteArray();

        for (int i = 0; i < parameters.getTrackCount(); i++) {
            TrackInformationBlock trackInfo = TrackInformationBlock.builder()
                    .withTrackNumber(i)
                    .withSectorCount(parameters.getSectorsByTrack())
                    .withSectorSize(parameters.getSectorSize())
                    .withSideNumber(DISK_SIDES - 1)
                    .withSectorInformationList(FIRST_SECTOR_ID)
                    .withFillerByte(FILLER_BYTE)
                    .withGap3Length(GAP3_LENGTH)
                    .build();
            LOGGER.debug("Adding track info {}", trackInfo);
            bos.write(trackInfo.toByteArray());
            bos.write(fsData, trackDataSize * i,
                    trackDataSize);
        }

        stream.write(bos.toByteArray());
    }

    public static void generateRescueCdt(OutputStream stream) throws IOException {
        CdtBuilder builder = new CdtBuilder();
        builder.addBinary(Binary.builder()
                .withData(Constants.getRescueEewriter())
                .withLoadAddress(RESCUE_LOADER_LOAD_ADDR)
                .withExecAddress(RESCUE_LOADER_LOAD_ADDR)
                .withName(TAPE_RESCUE_LOADER_NAME)
                .build());
        builder.dump(stream);
    }
}


