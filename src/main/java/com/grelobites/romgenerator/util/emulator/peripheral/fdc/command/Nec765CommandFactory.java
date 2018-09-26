package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nec765CommandFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(Nec765CommandFactory.class);

    public static Nec765Command getCommand(int data) {
        LOGGER.debug("Requesting command creation with data {}", String.format("0x%02x", data & 0xff));
        switch (data & 0x1f) {
            case 0x06:
                LOGGER.debug("Read Data command");
                return new ReadDataCommand();
            case 0x05:
                LOGGER.debug("Write Data command");
                return null;
            case 0x0c:
                LOGGER.debug("Read Deleted Data command");
                return null;
            case 0x09:
                LOGGER.debug("Write Deleted Data command");
                return null;
            case 0x02:
                LOGGER.debug("Read Track command");
                return null;
            case 0x0d:
                LOGGER.debug("Format Track command");
                return null;
            case 0xa0:
                LOGGER.debug("Read Id command");
                return null;
            case 0x11:
            case 0x15:
            case 0x19:
            case 0x1d:
                LOGGER.debug("Scan command");
                return null;
            case 0x07:
                LOGGER.debug("Recalibrate command");
                return null;
            case 0x0f:
                LOGGER.debug("Head reposition command");
                return null;
            case 0x08:
                LOGGER.debug("Read Interrupt status register command");
                return null;
            case 0x04:
                LOGGER.debug("Read drive status command");
                return null;
            case 0x03:
                LOGGER.debug("Specify Unit Data command");
                return null;
            default:
                LOGGER.debug("Invalid command issued");
                return null;
        }
    }
}
