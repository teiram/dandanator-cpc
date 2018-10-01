package com.grelobites.romgenerator.util.emulator.resources;

import com.grelobites.romgenerator.model.SnapshotGame;

import java.io.IOException;
import java.util.Map;

public interface LoaderResources {
    byte[] osRom() throws IOException;
    byte[] basicRom() throws IOException;
    SnapshotGame snaLoader() throws IOException;
    Map<Integer, byte[]> highRoms() throws IOException;
}
