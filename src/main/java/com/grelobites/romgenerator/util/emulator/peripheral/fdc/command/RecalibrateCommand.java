package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Command;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class RecalibrateCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecalibrateCommand.class);
    private Nec765 controller;
    private int currentCommandWord = 0;
    private boolean done = false;

    private void setTrackZero(int unit) {
        Optional<DskContainer> dskOpt = controller.getDskContainer(unit);
        if (dskOpt.isPresent()) {
            SectorInformationBlock firstSector = dskOpt.get().getTrack(0)
                    .getInformation().getSectorInformation(0);
            controller.getDriveStatus(unit).setCurrentSector(firstSector);
        } else {
            LOGGER.debug("No disk is attached to the required unit");
            controller.getStatus0Register().setNotReady(true);
            controller.getStatus0Register().setDiskUnit(unit);
        }
    }

    private void getCommandData(int data) {
        switch (currentCommandWord++) {
            case 0:
                //Nothing to get here
                break;
            case 1:
                setTrackZero((data & 0x03));
                controller.setCurrentPhase(Nec765Phase.RESULT);
                done = true;
                break;
            default:
        }
    }

    @Override
    public void setFdcController(Nec765 controller) {
        this.controller = controller;
    }

    @Override
    public void write(int data) {
        switch (controller.getCurrentPhase()) {
            case COMMAND:
                getCommandData(data);
                break;
            default:
                LOGGER.debug("Unexpected data during Recalibrate command");
        }
    }

    @Override
    public int read() {
        LOGGER.error("Trying to read during Recalibrate command");
       return 0;
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
