package com.grelobites.romgenerator.util.emulator;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;

public class RomResourcesBase implements RomResources {
    private static final int ROM_SIZE = 0x4000;


    private String lowRomResource;
    private String highRomResource;
    private byte[] lowRom;
    private byte[] highRom;

    public RomResourcesBase(String lowRomResource, String highRomResource) {
        this.lowRomResource = lowRomResource;
        this.highRomResource = highRomResource;
    }

    protected byte[] loadRom(String romResource) throws IOException {
        return Util.fromInputStream(RomResourcesBase.class
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
}
