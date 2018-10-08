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
    -== Read Track ==-
    COMMAND
    - B0.     MT  MF  SK  0   0   0   1   0
    - B1.     x   x   x   x   x   HD  US1 US0
    ---------------------------------------------------------------------------
              |   |   |           |    -----
              |   |   |           |      |--  US1,US0. Drive Unit (0, 1, 2, 3)
              |   |   |           |---------  HD. Physical head number (0 or 1)
              |   |   |---------------------  SK. Skip deleted data
              |   |-------------------------  MF. 0 for MF, 1 for MFM
              |-----------------------------  MT. Multitrack command

    - B2. C. Cylinder number. Stands for the current /selected cylinder
             (track) numbers 0 through 76 of the medium
    - B3. H. Head Address. H stands for the logical head number (0 or 1)
             specified in ID field
    - B4. R. Record. R stands for the sector number which will be read
             or written
    - B5. N. Number. N stands for the number of data bvtes written
             in a sector
    - B6. EOT. End Of Track. Last sector number id.
    - B7. GPL. Gap 3 length
    - B8. DTL. When N is defined as 00. DTL stands for the data
            length which users are going to read out or write
            into the sector
    ---------------------------------------------------------------------------
    EXECUTION
    - Bx. Bytes from sector are read
    ---------------------------------------------------------------------------
    RESULT
    - B0.   ST0
    - B1.   ST1
    - B2.   ST2
    - B3.   C
    - B4.   H
    - B5.   R
    - B6.   N
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
            controller.getDriveStatus(unit).setCurrentSector(sectorInfoList[0]);
            if (sectorInfoList[0].getSectorId() == firstSector) {
                indexes.add(0);
                for (int i = 1; i < dskTrack.getInformation().getSectorCount(); i++) {
                    SectorInformationBlock sectorInfo = dskTrack.getInformation().getSectorInformation(i);
                    controller.getDriveStatus(unit).setCurrentSector(sectorInfo);
                    indexes.add(i);
                    if (sectorInfo.getSectorId() == lastSector) {
                        matched = true;
                        break;
                    }
                }
                if (matched) {
                    prepareTrackData(dskTrack, indexes);
                    controller.getMainStatusRegister().setDataReady(true);
                    controller.getMainStatusRegister().setExecMode(true);
                    controller.setCurrentPhase(Nec765Phase.EXECUTION);
                    return;
                } else {
                    LOGGER.debug("No sector matched the provided last sector id");
                }
            } else {
                LOGGER.debug("First track sector id doesn't match the provided sector id");
            }
        } else {
            LOGGER.info("No track found with id {}", track);
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
                    controller.getStatistics().incBytesRead();
                    if (sectorDataIndex == sectorData.length) {
                        controller.getMainStatusRegister().setExecMode(false);
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
