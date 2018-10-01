package com.grelobites.romgenerator.util.emulator.resources;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class Cpc6128LoaderResources extends LoaderResourcesBase {
    private static final String OS_ROM_RESOURCE = "/rom/6128/OS_6128.ROM";
    private static final String BASIC_ROM_RESOURCE = "/rom/6128/BASIC_1.1.ROM";
    private static final String AMSDOS_ROM_RESOURCE = "/rom/6128/AMSDOS_0.5.ROM";
    private static final String SNA_LOADER_RESOURCE = "/sna/6128loader.sna";

    private static Cpc6128LoaderResources instance;

    public static Cpc6128LoaderResources getInstance() {
        if (instance == null) {
            instance = new Cpc6128LoaderResources();
        }
        return instance;
    }

    private Cpc6128LoaderResources() {
        super(OS_ROM_RESOURCE, BASIC_ROM_RESOURCE,
                SNA_LOADER_RESOURCE);
    }

    @Override
    public Map<Integer, byte[]> highRoms() throws IOException {
        return Collections.singletonMap(7, loadRom(AMSDOS_ROM_RESOURCE));
    }
}
