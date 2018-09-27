package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.FormatTrackCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.HeadRepositionCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.InvalidCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.Nec765Command;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.ReadDataCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.ReadIdCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.ReadTrackCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.RecalibrateCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.ScanEqualCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.ScanHighOrEqualCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.ScanLowOrEqualCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.SenseDriveStatusCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.SenseInterruptStatusCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.SpecifyUnitDataCommand;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.WriteDataCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Nec765CommandFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(Nec765CommandFactory.class);

    public Nec765Command getCommand(int data) {
        LOGGER.debug("Requesting command creation with data {}", String.format("0x%02x", data & 0xff));
        switch (data & 0x1f) {
            case 0x06:
                LOGGER.debug("Read Data command");
                return new ReadDataCommand();
            case 0x05:
                LOGGER.debug("Write Data command");
                return new WriteDataCommand();
            case 0x0c:
                LOGGER.debug("Read Deleted Data command");
                return new ReadDataCommand();
            case 0x09:
                LOGGER.debug("Write Deleted Data command");
                return new WriteDataCommand();
            case 0x02:
                LOGGER.debug("Read Track command");
                return new ReadTrackCommand();
            case 0x0d:
                LOGGER.debug("Format Track command");
                return new FormatTrackCommand();
            case 0xa0:
                LOGGER.debug("Read Id command");
                return new ReadIdCommand();
            case 0x11:
                LOGGER.debug("Scan equal command");
                return new ScanEqualCommand();
            case 0x19:
                LOGGER.debug("Scan Low or Equal command");
                return new ScanLowOrEqualCommand();
            case 0x1d:
                LOGGER.debug("Scan High or Equal command");
                return new ScanHighOrEqualCommand();
            case 0x07:
                LOGGER.debug("Recalibrate command");
                return new RecalibrateCommand();
            case 0x0f:
                LOGGER.debug("Head reposition command");
                return new HeadRepositionCommand();
            case 0x08:
                LOGGER.debug("Read Interrupt status register command");
                return new SenseInterruptStatusCommand();
            case 0x04:
                LOGGER.debug("Read drive status command");
                return new SenseDriveStatusCommand();
            case 0x03:
                LOGGER.debug("Specify Unit Data command");
                return new SpecifyUnitDataCommand();
            default:
                LOGGER.debug("Invalid command issued with code {}", String.format("0x%02x", data));
                return new InvalidCommand();
        }
    }
}
