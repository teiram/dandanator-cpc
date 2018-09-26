package com.grelobites.romgenerator.util.emulator.peripheral.fdc.command;

public class ScanEqualCommand extends ScanBaseCommand {

    @Override
    protected boolean compareSectorContents(byte[] sector) {
        for (byte sectorByte : sector) {
            if (sectorByte != byteToCompare.byteValue()) {
                return false;
            }
        }
        return true;
    }
}
