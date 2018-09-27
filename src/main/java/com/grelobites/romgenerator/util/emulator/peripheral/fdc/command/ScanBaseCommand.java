package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;
import com.grelobites.romgenerator.util.dsk.Track;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Constants;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    -== Scan ==-
    COMMAND
    - B0.     MT  MF  SK  1   S1  S0  0   1
    - B1.     x   x   x   x   x   HD  US1 US0
    ---------------------------------------------------------------------------
              |   |   |           |    -----
              |   |   |           |      |--  US1,US0. Drive Unit (0, 1, 2, 3)
              |   |   |           |---------  HD. Physical head number (0 or 1)
              |   |   |---------------------  SK. Skip deleted data
              |   |-------------------------  MF. 0 for MF, 1 for MFM
              |-----------------------------  MT. Multitrack command
      S1, S0:
       1,  1: Greater or equal
       1,  0: Lesser or equal
       0,  0: Equal
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
    - B0. Byte to be compared against sector contents
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

public abstract class ScanBaseCommand extends ReadWriteBaseCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanBaseCommand.class);

    private Track targetTrack;
    protected Integer byteToCompare;
    private SectorInformationBlock matchingSector;

    protected void preExecutionOperation(DskContainer dsk) {
        targetTrack = dsk.getTrack(track);
        if (targetTrack == null) {
            controller.getStatus1Register().setNoData(true);
            controller.getStatus0Register().setInterruptCode(Nec765Constants.ICODE_ABNORMAL_TERMINATION);
            throw new IllegalStateException("Required track not found");
        }
    }

    protected abstract boolean compareSectorContents(byte[] sector);

    private void performSectorScan() {
        //Look for a sector matching the scan conditions
        boolean inScanZone = false;
        for (SectorInformationBlock sectorInfo : targetTrack.getInformation()
                .getSectorInformationList()) {
            controller.getDriveStatus(unit).setCurrentSector(sectorInfo);
            matchingSector = sectorInfo;
            if (matchingSector.getSectorId() == firstSector) {
                inScanZone = true;
            }
            if (inScanZone) {
                if (compareSectorContents(targetTrack.getSectorData(matchingSector.getPhysicalPosition()))) {
                    controller.getStatus2Register().setScanOk(true);
                    return;
                }
            }
            if (matchingSector.getSectorId() == lastSector) {
                break;
            }
        }
        controller.getStatus2Register().setScanNotSatisfied(true);
    }

    private void setResultValues() {
        track = matchingSector.getTrack();
        head = matchingSector.getSide();
        firstSector = matchingSector.getSectorId();
        sectorSize = matchingSector.getSectorSize();
    }

    @Override
    public void write(int data) {
        switch (controller.getCurrentPhase()) {
            case COMMAND:
                setCommandData(data);
                break;
            case EXECUTION:
                if (byteToCompare == null) {
                    byteToCompare = data;
                    performSectorScan();
                    setResultValues();
                    controller.setCurrentPhase(Nec765Phase.RESULT);
                }
            case RESULT:
                LOGGER.error("Writing data to ScanCommand in result phase");
                break;
        }
    }

    @Override
    public int read() {
        LOGGER.error("Trying to read from Scan Command");
        return 0;
    }
}
