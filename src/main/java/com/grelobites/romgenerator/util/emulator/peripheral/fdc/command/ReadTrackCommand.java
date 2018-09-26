package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;
import com.grelobites.romgenerator.util.dsk.Track;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Constants;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    - Read Track
    -   MT  MF  SK  0   0   1   1   0
    -   x   x   x   x   x   HD  US1 US0
    -   C. Cylinder number. Stands for the current /selected cylinder
        (track) numbers 0 through 76 of the medium
    -   H. Head Address. H stands for the logical head number (0 or 1)
           specified in ID field
    -   R. Record. R stands for the sector number which will be read
            or written
    -   N. Number. N stands for the number of data bvtes written
            in a sector
    -   EOT. End Of Track. Last sector number id.
    -   GPL. Gap 3 length
    -   DTL. When N is defined as 00. DTL stands for the data
            length which users are going to read out or write
            into the sector
    --------------------------------------------------------
    * MT. Indicates a multitrack operation.
    * MF. 0 for MF, 1 for MFM
    * SK. (Skip) SK stands for skip deleted data address mark
    * HD. Physical head number (0 or 1)
    * US1,US0. Drive unit (0, 1, 2, 3)
    --------------------------------------------------------
    RESULT
    - ST0
    - ST1
    - ST2
    - C
    - H
    - R
    - N
 */
public class ReadTrackCommand extends ReadWriteBaseCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadTrackCommand.class);

    private byte[] sectorData;
    private int sectorDataIndex = 0;

    private void prepareTrackData(Track dskTrack, List<Integer> indexes) {
        sectorData = new byte[indexes.size() * sectorBytes];
        int position = 0;
        for (int index : indexes) {
            System.arraycopy(dskTrack.getSectorData(index), 0,
                    sectorData, position * sectorBytes, sectorBytes);
            position++;
        }
        LOGGER.debug("Prepared track data of {} bytes", sectorData.length);
    }

    protected void preExecutionOperation(DskContainer dsk) {
        Track dskTrack = dsk.getTrack(track);
        if (dskTrack != null) {
            //First sectorId must match the one provided
            List<Integer> indexes = new ArrayList<>();
            boolean matched = false;
            SectorInformationBlock[] sectorInfoList = dskTrack.getInformation().getSectorInformationList();
            if (sectorInfoList[0].getSectorId() == firstSector) {
                indexes.add(0);
                for (int i = 1; i < dskTrack.getInformation().getSectorCount(); i++) {
                    SectorInformationBlock sectorInfo = dskTrack.getInformation().getSectorInformation(i);
                    indexes.add(i);
                    if (sectorInfo.getSectorId() == lastSector) {
                        matched = true;
                        break;
                    }
                }
                if (matched) {
                    prepareTrackData(dskTrack, indexes);
                    return;
                } else {
                    LOGGER.debug("No sector matched the provided last sector id");
                }
            } else {
                LOGGER.debug("First track sector id doesn't match the provided sector id");
            }
        } else {
            LOGGER.debug("No track found with id {}", track);
        }
        controller.getStatus1Register().setNoData(true);
        controller.getStatus0Register().setInterruptCode(Nec765Constants.ICODE_ABNORMAL_TERMINATION);
        throw new IllegalStateException("No sector found");
    }

    @Override
    public void write(int data) {
        switch (controller.getCurrentPhase()) {
            case COMMAND:
                setCommandData(data);
                break;
            case EXECUTION:
            case RESULT:
                LOGGER.error("Writing data to ReadDataCommand in non-command phase");
                break;
        }
    }

    @Override
    public int read() {
        int value = 0;
        switch (controller.getCurrentPhase()) {
            case COMMAND:
                LOGGER.error("Trying to read from Controller in Command phase");
                break;
            case EXECUTION:
                if (sectorDataIndex < sectorData.length) {
                    value = sectorData[sectorDataIndex++];
                    if (sectorDataIndex == sectorData.length) {
                        controller.setCurrentPhase(Nec765Phase.RESULT);
                    }
                } else {
                    throw new IllegalStateException("Data exhausted and still in execution phase");
                }
                break;
            case RESULT:
                value = getCommandResult();
        }
        return value;
    }
}
