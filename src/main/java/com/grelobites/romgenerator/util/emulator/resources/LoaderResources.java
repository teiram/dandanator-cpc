package com.grelobites.romgenerator.util.emulator.resources;

import com.grelobites.romgenerator.model.SnapshotGame;

import java.io.IOException;

public interface LoaderResources {
    byte[] lowRom() throws IOException;
    byte[] highRom() throws IOException;
    SnapshotGame snaLoader() throws IOException;
}
