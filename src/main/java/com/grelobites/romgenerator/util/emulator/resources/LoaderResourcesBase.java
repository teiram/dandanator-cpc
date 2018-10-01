package com.grelobites.romgenerator.util.emulator.resources;

import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoaderFactory;
import com.grelobites.romgenerator.util.gameloader.GameImageType;

import java.io.IOException;

public abstract class LoaderResourcesBase implements LoaderResources {
    private static final int ROM_SIZE = 0x4000;

    private String osRomResource;
    private String basicRomResource;
    private String snaLoaderResource;
    private byte[] osRom;
    private byte[] basicRom;
    private SnapshotGame snaLoader;

    public LoaderResourcesBase(String osRomResource, String basicRomResource,
                               String snaLoaderResource) {
        this.osRomResource = osRomResource;
        this.basicRomResource = basicRomResource;
        this.snaLoaderResource = snaLoaderResource;
    }

    protected byte[] loadRom(String romResource) throws IOException {
        return Util.fromInputStream(LoaderResourcesBase.class
                .getResourceAsStream(romResource), ROM_SIZE);
    }

    @Override
    public byte[] osRom() throws IOException {
        if (osRom == null) {
            osRom = loadRom(osRomResource);
        }
        return osRom;
    }

    @Override
    public byte[] basicRom() throws IOException {
        if (basicRom == null) {
            basicRom = loadRom(basicRomResource);
        }
        return basicRom;
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
