package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

public class Nec765Statistics {
    private long issuedCommands;
    private long bytesRead;

    public void incIssuedCommands() {
        issuedCommands++;
    }

    public void incBytesRead() {
        bytesRead++;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public long getIssuedCommands() {
        return issuedCommands;
    }

    @Override
    public String toString() {
        return "Nec765Statistics{" +
                "issuedCommands=" + issuedCommands +
                ", bytesRead=" + bytesRead +
                '}';
    }
}
