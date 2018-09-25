package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nec765CommandFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(Nec765CommandFactory.class);

    public static Nec765Command getCommand(int data) {
        LOGGER.debug("Requesting command creation with data {}", String.format("0x%02x", data & 0xff));
        Nec765Command command = null;
        if ((data & 0x06) == 0x06) {
            command = new ReadDataCommand();
        } else {
            throw new IllegalArgumentException("Unable to create Nec765 command with provided data");
        }

        command.addCommandData(data);
        return command;
    }
}
