package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.status.DriveParameters;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.status.DriveStatus;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    -== Specify Unit Data ==-
    COMMAND
    - B0.     0   0   0   0   0   0   1   1
    - B1.    SR3 SR2 SR1 SR0  HU3 HU2 HU1 HU0
    - B2.    HL6 HL5 HL4 HL3  HL2 HL1 HL0 DMA
    -        SR - Step Rate Time
    -        HU - Head Unload Time
    -        HL - Head Load Time
    -        DMA - 0 DMA, 1 NODMA
    ---------------------------------------------------------------------------
    RESULT
    - No results
 */
public class SpecifyUnitDataCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecifyUnitDataCommand.class);
    private Nec765 controller;
    private int currentCommandWord = 0;
    private int stepRateTime;
    private int headLoadTime;
    private int headUnloadTime;
    private boolean dma;

    private void setCommandData(int data) {
        switch (currentCommandWord++) {
            case 0:
                //Nothing to get here
                break;
            case 1:
                stepRateTime = (data & 0xf0) >>> 4;
                headUnloadTime = (data & 0x0f);
                LOGGER.trace("Set stepRateTime {}, headUnloadTime {}", stepRateTime, headUnloadTime);
                break;
            case 2:
                headLoadTime = (data & 0xfe) >>> 1;
                dma = (data & 0x01) == 0;
                DriveParameters driveParameters = controller.getDriveParameters();
                driveParameters.setHeadLoadTime(headLoadTime);
                driveParameters.setHeadUnloadTime(headUnloadTime);
                driveParameters.setStepRateTime(stepRateTime);
                driveParameters.setDma(dma);
                LOGGER.trace("Set headLoadTime {}, dma {}", headLoadTime, dma);
                controller.clearCurrentCommand();
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
                LOGGER.error("Unexpected data during SpecifyUnitData command");
        }
    }

    @Override
    public int read() {
        LOGGER.error("Trying to read during SpecifyUnitData command");
        return 0;
    }

}
