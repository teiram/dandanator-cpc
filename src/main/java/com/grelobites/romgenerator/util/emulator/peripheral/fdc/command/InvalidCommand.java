package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Command;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidCommand implements Nec765Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidCommand.class);
    private Nec765 controller;

    @Override
    public void setFdcController(Nec765 controller) {
        this.controller = controller;
    }

    @Override
    public void write(int data) {
        LOGGER.debug("Issuing invalid command");
        controller.getStatus0Register().setInterruptCode(Nec765Constants.ICODE_INVALID_COMMAND);
    }

    @Override
    public int read() {
        return 0;
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
