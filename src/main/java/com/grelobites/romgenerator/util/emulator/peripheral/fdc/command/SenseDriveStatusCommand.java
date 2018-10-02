package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    -== Sense Drive Status ==-
    COMMAND
    - B0.     0   0   0   0   0   1   0   0
    - B1.     x   x   x   x   x   HD  US1 US0
    ---------------------------------------------------------------------------
                                  |    -----
                                  |      |--  US1,US0. Drive Unit (0, 1, 2, 3)
                                  |---------  HD. Physical head number (0 or 1)
    ---------------------------------------------------------------------------
    RESULT
    - ST3 of the selected drive
 */
public class SenseDriveStatusCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(SenseDriveStatusCommand.class);
    private Nec765 controller;
    private int unit = 0;
    private int currentCommandWord = 0;

    private void setCommandData(int data) {
        switch (currentCommandWord++) {
            case 0:
                //Nothing to get here
                break;
            case 1:
                unit = data & 0x03;
                break;
            case 2:
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
                LOGGER.debug("Unexpected data during SenseDriveStatus command");
        }
    }

    @Override
    public int read() {
        int value = 0;
        switch (controller.getCurrentPhase()) {
            case RESULT:
                //Actually we should have a different status for each of the attached drives
                //But let's assume everything is nice and sunny
                value = controller.getStatus3Register().value();
                controller.clearCurrentCommand();
            default:
                LOGGER.error("Trying to read from SenseDriveStatus Command");
        }
        return value;
    }

}
