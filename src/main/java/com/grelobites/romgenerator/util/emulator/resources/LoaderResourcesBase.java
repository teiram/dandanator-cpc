package com.grelobites.romgenerator.util.emulator.resources;

import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoaderFactory;
import com.grelobites.romgenerator.util.gameloader.GameImageType;

import java.io.IOException;

public class LoaderResourcesBase implements LoaderResources {
    private static final int ROM_SIZE = 0x4000;

    private String lowRomResource;
    private String highRomResource;
    private String snaLoaderResource;
    private byte[] lowRom;
    private byte[] highRom;
    private SnapshotGame snaLoader;

    public LoaderResourcesBase(String lowRomResource, String highRomResource,
                               String snaLoaderResource) {
        this.lowRomResource = lowRomResource;
        this.highRomResource = highRomResource;
        this.snaLoaderResource = snaLoaderResource;
    }

    protected byte[] loadRom(String romResource) throws IOException {
        return Util.fromInputStream(LoaderResourcesBase.class
                .getResourceAsStream(romResource), ROM_SIZE);
    }

    @Override
    public byte[] lowRom() throws IOException {
        if (lowRom == null) {
            lowRom = loadRom(lowRomResource);
        }
        return lowRom;
    }

    @Override
    public byte[] highRom() throws IOException {
        if (highRom == null) {
            highRom = loadRom(highRomResource);
        }
        return highRom;
    }

    @Override
    public SnapshotGame snaLoader() throws IOException {
        if (snaLoader == null) {
            snaLoader = (SnapshotGame) GameImageLoaderFactory
                    .getLoader(GameImageType.SNA)
                    .load(LoaderResourcesBase.class
                            .getResourceAsStream(snaLoaderResource));

        }
        return snaLoader;
    }
}
