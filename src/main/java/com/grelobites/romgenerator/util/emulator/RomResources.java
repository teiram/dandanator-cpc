package com.grelobites.romgenerator.util.emulator;

import java.io.IOException;

public interface RomResources {
    byte[] lowRom() throws IOException;
    byte[] highRom() throws IOException;
}
