package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;
import com.grelobites.romgenerator.util.dsk.Track;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Command;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Constants;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/*
    -== Read Id ==-
    COMMAND
    - B0.     0   MF  0   0   1   0   1   0
    - B1.     x   x   x   x   x   HD  US1 US0
    ---------------------------------------------------------------------------
                  |               |    -----
                  |               |      |--  US1,US0. Drive Unit (0, 1, 2, 3)
                  |               |---------  HD. Physical head number (0 or 1)
                  |-------------------------- MF. 0 for MF, 1 for MFM
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
public class ReadIdCommand extends Nec765BaseCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadIdCommand.class);

    private int currentCommandWord = 0;
    private int currentResultWord = 0;
    private SectorInformationBlock sectorInfo;

    private void prepareExecution() {
        LOGGER.debug("Read Id operation on unit {}, head {}",
                unit, physicalHeadNumber);
        Optional<DskContainer> dskOpt = controller.getDskContainer(unit);
        if (dskOpt.isPresent()) {
            controller.getStatus0Register().setDiskUnit(unit);
            controller.getStatus0Register().setHeadAddress(physicalHeadNumber);
            SectorInformationBlock lastSectorInfo = controller.getDriveStatus(unit).getLastSector();
            if (lastSectorInfo != null) {
                Track dskTrack = dskOpt.get().getTrack(sectorInfo.getTrack());
                //Get the first sector, maybe this is enough
                sectorInfo = dskTrack.getInformation().getSectorInformation(0);
                controller.getStatus0Register().setNotReady(false);
            } else {
                LOGGER.debug("No lastSector info on current disk unit");
                controller.getStatus0Register().setNotReady(true);
            }
        } else {
            LOGGER.debug("No disk is attached to the required unit");
            //No disk is attached to the required unit
            controller.getStatus0Register().setNotReady(true);
            controller.getStatus0Register().setDiskUnit(unit);
            controller.getStatus0Register().setHeadAddress(physicalHeadNumber);
        }
        controller.setCurrentPhase(Nec765Phase.RESULT);
    }

    protected void setCommandData(int data) {
        switch (currentCommandWord) {
            case 0:
                setPrimaryFlags(data);
                break;
            case 1:
                setSecondaryFlags(data);
                prepareExecution();
                break;
            default:
                throw new IllegalStateException("Too many command bytes provided");
        }
        currentCommandWord++;
    }

    @Override
    public void write(int data) {
        switch (controller.getCurrentPhase()) {
            case COMMAND:
                setCommandData(data);
                break;
            case RESULT:
            case EXECUTION:
                LOGGER.error("Writing data to ReadId Command in non-command phase");
                break;
        }
    }

    @Override
    public int read() {
        int value = 0;
        switch (controller.getCurrentPhase()) {
            case COMMAND:
            case EXECUTION:
                LOGGER.error("Trying to read from Controller in non-result phase");
                break;
            case RESULT:
                value = getCommandResult();
        }
        return value;
    }

    protected int getCommandResult() {
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
                value = sectorInfo.getTrack();
                break;
            case 4:
                value = sectorInfo.getSide();
                break;
            case 5:
                value = sectorInfo.getSectorId();
                break;
            case 6:
                value = sectorInfo.getSectorSize();
                done = true;
                break;
            default:
                throw new IllegalStateException("Result status exhausted");
        }
        currentResultWord++;
        return value;
    }
}
