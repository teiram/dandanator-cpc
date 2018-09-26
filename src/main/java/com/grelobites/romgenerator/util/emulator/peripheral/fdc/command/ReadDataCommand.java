package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;
import com.grelobites.romgenerator.util.dsk.Track;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Command;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Constants;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/*
    - Read Data
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
    * MF.
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
public class ReadDataCommand extends Nec765CommandBase implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadDataCommand.class);
    private Nec765 controller;
    private int currentCommandWord = 0;
    private int currentResultWord = 0;
    private int track;
    private int head;
    private int firstSector;
    private int lastSector;
    private int sectorSize;
    private int sectorBytes;
    private int gap3length;
    private boolean done = false;
    private byte[] sectorData;
    private int currentByte = 0;

    @Override
    public void setFdcController(Nec765 controller) {
        if (controller.getCurrentCommand() == null) {
            this.controller = controller;
            controller.setCurrentPhase(Nec765Phase.COMMAND);
        } else {
            throw new IllegalStateException("Controller executing command");
        }
    }

    private void fetchSectorData(DskContainer dsk) {
        Track dskTrack = dsk.getTrack(track);
        if (dskTrack != null) {
            //Search for sectorId
            for (SectorInformationBlock sectorInfo : dskTrack.getInformation().getSectorInformationList()) {
                if (sectorInfo.getSectorId() == firstSector) {
                    sectorData = dskTrack.getSectorData(sectorInfo.getPhysicalPosition());
                    //Fill status registers stored in DSK
                    controller.getStatus1Register().setValue(sectorInfo.getFdcStatusRegister1());
                    controller.getStatus2Register().setValue(sectorInfo.getFdcStatusRegister2());
                    return;
                }
            }
            //Sector not found
            controller.getStatus1Register().setNoData(true);
            controller.getStatus0Register().setInterruptCode(Nec765Constants.ICODE_ABNORMAL_TERMINATION);
            throw new IllegalStateException("No sector found");
        }
    }

    private void prepareExecution() {
        LOGGER.debug("Read operation on unit {}, head {}, track {}, firstSector {}, lastSector {}, sectorBytes {}",
                unit, head, track, firstSector, lastSector, sectorBytes);
        Optional<DskContainer> dskOpt = controller.getDskContainer(unit);
        if (dskOpt.isPresent()) {
            controller.getStatus0Register().setNotReady(false);
            controller.getStatus0Register().setDiskUnit(unit);
            controller.getStatus0Register().setHeadAddress(physicalHeadNumber);
            try {
                fetchSectorData(dskOpt.get());
                controller.setCurrentPhase(Nec765Phase.EXECUTION);
            } catch (Exception e) {
                LOGGER.error("Fetching sector data");
            }
        } else {
            //No disk is attached to the required unit
            controller.getStatus0Register().setNotReady(true);
            controller.getStatus0Register().setDiskUnit(unit);
            controller.getStatus0Register().setHeadAddress(physicalHeadNumber);
        }
        controller.setCurrentPhase(Nec765Phase.RESULT);
    }

    @Override
    public void addCommandData(int data) {
        switch (currentCommandWord) {
            case 0:
                setPrimaryFlags(data);
                break;
            case 1:
                setSecondaryFlags(data);
                break;
            case 2:
                track = data;
                break;
            case 3:
                head = data;
                break;
            case 4:
                firstSector = data;
                break;
            case 5:
                sectorSize = data;
                sectorBytes = 128 << sectorSize;
                break;
            case 6:
                lastSector = data;
                break;
            case 7:
                gap3length = data;
                break;
            case 8:
                sectorBytes = (sectorBytes == 0) ? 128 << data: sectorBytes;
                prepareExecution();
                break;
            default:
                throw new IllegalStateException("Too many command bytes provided");
        }
        currentCommandWord++;
    }

    @Override
    public int execute() {
        int value;
        if (currentByte < sectorData.length) {
            value = sectorData[currentByte++];
            if (currentByte == sectorData.length) {
                controller.setCurrentPhase(Nec765Phase.RESULT);
            }
            return value;
        } else {
            throw new IllegalStateException("Trying to execute in result phase");
        }
    }

    @Override
    public int result() {
        int value;
        switch (currentResultWord) {
            case 0:
                value = controller.getStatus0Register().value();
                break;
            case 1:
                value = controller.getStatus1Register().value();
                break;
            case 2:
                value = controller.getStatus2Register().value();
                break;
            case 3:
                value = track;
                break;
            case 4:
                value = head;
                break;
            case 5:
                value = firstSector;
                break;
            case 6:
                value = sectorSize;
                done = true;
                break;
            default:
                throw new IllegalStateException("Result status exhausted");
        }
        currentResultWord++;
        return value;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
