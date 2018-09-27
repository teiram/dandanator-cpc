package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;
import com.grelobites.romgenerator.util.dsk.Track;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Constants;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
    -== Format Track ==-
    COMMAND
    - B0.     0   MF  0   0   1   1   0   1
    - B1.     x   x   x   x   x   HD  US1 US0
    ---------------------------------------------------------------------------
                  |               |    -----
                  |               |      |--  US1,US0. Drive Unit (0, 1, 2, 3)
                  |               |---------  HD. Physical head number (0 or 1)
                  |-------------------------  MF. 0 for MF, 1 for MFM


    - B2. Sector Size. (LOG2 SIZE) - 7
    - B3. Sectors per track
    - B4. Gap 3 Size
    - B5. Filler byte
    ---------------------------------------------------------------------------
    EXECUTION
    For each sector to format:
    - B0. Cylinder number
    - B1. Head number
    - B2. Sector number
    - B3. Sector size (LOG2 size) - 7
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
public class FormatTrackCommand extends ReadWriteBaseCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormatTrackCommand.class);
    private int sectorsPerTrack;
    private int fillerByte;
    private int executionBytes;

    protected void setCommandData(int data) {
        switch (currentCommandWord++) {
            case 0:
                setPrimaryFlags(data);
                break;
            case 1:
                setSecondaryFlags(data);
                break;
            case 2:
                sectorSize = data;
                sectorBytes = Nec765Constants.BASE_SECTOR_SIZE << sectorSize;
                break;
            case 3:
                sectorsPerTrack = data;
                executionBytes = sectorsPerTrack * 4;
                break;
            case 4:
                gap3length = data;
                break;
            case 5:
                fillerByte = data;
                controller.setCurrentPhase(Nec765Phase.EXECUTION);
                break;
            default:
                throw new IllegalStateException("Too many command bytes provided");
        }
    }

    @Override
    public void write(int data) {
        switch (controller.getCurrentPhase()) {
            case COMMAND:
                setCommandData(data);
                break;
            case EXECUTION:
                if (--executionBytes == 0) {
                    controller.setCurrentPhase(Nec765Phase.RESULT);
                }
                break;
            case RESULT:
                LOGGER.error("Writing data to FormatTrack command in result phase");
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
}
