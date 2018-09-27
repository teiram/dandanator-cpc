package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/*
    -== Head Reposition ==-
    COMMAND
    - B0.     0   0   0   0   1   1   1   1
    - B1.     x   x   x   x   x   HD  US1 US0
    ---------------------------------------------------------------------------
                                  |    -----
                                  |      |--  US1,US0. Drive Unit (0, 1, 2, 3)
                                  |---------  HD. Physical head number (0 or 1)

    - B2. C. Cylinder number. Stands for the current /selected cylinder
             (track) numbers 0 through 76 of the medium
    ---------------------------------------------------------------------------
    RESULT
    - No results
 */
public class HeadRepositionCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadRepositionCommand.class);
    private Nec765 controller;
    private int unit = 0;
    private int currentCommandWord = 0;
    private boolean done = false;

    private void repositionToTrack(int track) {
        controller.setLastSelectedUnit(unit);
        Optional<DskContainer> dskOpt = controller.getDskContainer(unit);
        if (dskOpt.isPresent()) {
            SectorInformationBlock firstSector = dskOpt.get().getTrack(track)
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
                unit = data & 0x03;
                break;
            case 2:
                repositionToTrack(data);
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
                setCommandData(data);
                break;
            default:
                LOGGER.debug("Unexpected data during Head Reposition command");
        }
    }

    @Override
    public int read() {
        LOGGER.error("Trying to read during Head Reposition command");
       return 0;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
