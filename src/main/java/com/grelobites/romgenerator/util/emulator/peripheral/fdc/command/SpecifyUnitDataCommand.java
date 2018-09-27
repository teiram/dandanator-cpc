package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.status.DriveStatus;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    -== Specify Unit Data ==-
    COMMAND
    - B0.     0   0   0   0   0   0   1   1
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
public class SpecifyUnitDataCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecifyUnitDataCommand.class);
    private Nec765 controller;
    private int unit = 0;
    private int currentCommandWord = 0;
    private int stepRateTime;
    private int headLoadTime;
    private int headUnloadTime;
    private boolean dma;

    private boolean done = false;


    private void setCommandData(int data) {
        switch (currentCommandWord++) {
            case 0:
                //Nothing to get here
                break;
            case 1:
                unit = data & 0x03;
                break;
            case 2:
                stepRateTime = (data & 0xf0) >>> 4;
                headUnloadTime = (data & 0x0f);
                break;
            case 3:
                headLoadTime = (data & 0xfe) >>> 1;
                dma = (data & 0x01) != 0;
                DriveStatus driveStatus = controller.getDriveStatus(unit);
                driveStatus.setHeadLoadTime(headLoadTime);
                driveStatus.setHeadUnloadTime(headUnloadTime);
                driveStatus.setStepRateTime(stepRateTime);
                driveStatus.setDma(dma);
                controller.setCurrentPhase(Nec765Phase.RESULT);
                done = true;
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
                LOGGER.debug("Unexpected data during SpecifyUnitData command");
        }
    }

    @Override
    public int read() {
        LOGGER.error("Trying to read during SpecifyUnitData command");
        return 0;
    }

    @Override
    public boolean isDone() {
        return done;
    }
}
