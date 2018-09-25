package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Command;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Phase;

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
    private static final int REQUIRED_COMMAND_WORDS = 9;
    private Nec765 controller;
    private int currentCommandWord = 0;
    private int currentResultWord = 0;
    private int track;
    private int head;
    private int firstSector;
    private int lastSector;
    private int sectorBytes;
    private int gap3length;
    private boolean done = false;

    @Override
    public void setFdcController(Nec765 controller) {
        if (controller.getCurrentCommand() == null) {
            this.controller = controller;
            controller.setCurrentPhase(Nec765Phase.COMMAND);
        } else {
            throw new IllegalStateException("Controller executing command");
        }
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
                sectorBytes = 128 << data;
                break;
            case 6:
                lastSector = data;
                break;
            case 7:
                gap3length = data;
                break;
            case 8:
                sectorBytes = (sectorBytes == 0) ? 128 << data: sectorBytes;
                break;
            default:
                throw new IllegalStateException("Too many command bytes provided");
        }
        currentCommandWord++;
    }

    @Override
    public boolean isPrepared() {
        return currentCommandWord == REQUIRED_COMMAND_WORDS;
    }

    @Override
    public boolean hasExecutionPhase() {
        return true;
    }

    @Override
    public boolean isExecuted() {
        return false;
    }

    @Override
    public int execute() {
        return 0;
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
                value = 2;
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
