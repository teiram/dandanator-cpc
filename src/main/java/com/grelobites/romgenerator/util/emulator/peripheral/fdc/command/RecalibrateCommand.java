package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/*
    -== Recalibrate ==-
    COMMAND
    - B0.     0   0   0   0   0   1   1   1
    - B1.     x   x   x   x   x   HD  US1 US0
    ---------------------------------------------------------------------------
                                  |    -----
                                  |      |--  US1,US0. Drive Unit (0, 1, 2, 3)
                                  |---------  HD. Physical head number (0 or 1)
    ---------------------------------------------------------------------------
    RESULT
    - No results
 */
public class RecalibrateCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecalibrateCommand.class);
    private Nec765 controller;
    private int currentCommandWord = 0;

    private void setTrackZero(int unit) {
        controller.setLastSelectedUnit(unit);
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

    private void setCommandData(int data) {
        switch (currentCommandWord++) {
            case 0:
                //Nothing to get here
                break;
            case 1:
                setTrackZero((data & 0x03));
                controller.clearCurrentCommand();
                break;
            default:
        }
    }

    @Override
    public void initialize(Nec765 controller) {
        this.controller = controller;
    }

    @Override
    public void write(int data) {
        switch (controller.getCurrentPhase()) {
            case COMMAND:
                setCommandData(data);
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

}
