package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Constants;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class ReadWriteBaseCommand extends Nec765BaseCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadWriteBaseCommand.class);
    protected int currentCommandWord = 0;
    protected int currentResultWord = 0;
    protected int track;
    protected int head;
    protected int firstSector;
    protected int lastSector;
    protected int sectorSize;
    protected int sectorBytes;
    protected int gap3length;

    protected void preExecutionOperation(DskContainer dsk) {}

    private void prepareExecution() {
        LOGGER.debug("Read/Write operation on unit {}, head {}, track {}, firstSector {}, lastSector {}, sectorBytes {}",
                unit, head, track, Util.asByteHexString(firstSector), Util.asByteHexString(lastSector), sectorBytes);
        controller.setLastSelectedUnit(unit);
        Optional<DskContainer> dskOpt = controller.getDskContainer(unit);
        if (dskOpt.isPresent()) {
            controller.getStatus0Register().setNotReady(false);
            controller.getStatus0Register().setDiskUnit(unit);
            controller.getStatus0Register().setHeadAddress(physicalHeadNumber);
            try {
                preExecutionOperation(dskOpt.get());
                controller.getMainStatusRegister().setExecMode(true);
                controller.setCurrentPhase(Nec765Phase.EXECUTION);
            } catch (Exception e) {
                LOGGER.error("In preExecutionOperation", e);
                controller.setCurrentPhase(Nec765Phase.RESULT);
            }
        } else {
            LOGGER.info("No disk is attached to the addressed unit");
            //No disk is attached to the required unit
            controller.getStatus0Register().setNotReady(true);
            controller.getStatus0Register().setDiskUnit(unit);
            controller.getStatus0Register().setHeadAddress(physicalHeadNumber);
            controller.setCurrentPhase(Nec765Phase.RESULT);
        }
    }

    protected void setCommandData(int data) {
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
                sectorBytes = Nec765Constants.BASE_SECTOR_SIZE << sectorSize;
                break;
            case 6:
                lastSector = data;
                break;
            case 7:
                gap3length = data;
                break;
            case 8:
                //Only when sectorSize is zero
                sectorBytes = (sectorBytes == 0) ? data: sectorBytes;
                prepareExecution();
                break;
            default:
                throw new IllegalStateException("Too many command bytes provided");
        }
        currentCommandWord++;
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
                controller.clearCurrentCommand();
                break;
            default:
                throw new IllegalStateException("Result status exhausted");
        }
        currentResultWord++;
        return value;
    }

}
