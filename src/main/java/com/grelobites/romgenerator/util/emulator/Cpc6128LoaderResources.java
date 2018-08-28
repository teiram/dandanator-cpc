package com.grelobites.romgenerator.util.emulator;

public class Cpc6128LoaderResources extends LoaderResourcesBase {
    private static final String LOW_ROM_RESOURCE = "rom/6128/OS_6128.ROM";
    private static final String HIGH_ROM_RESOURCE = "rom/6128/BASIC_1.1.ROM";
    private static final String SNA_LOADER_RESOURCE = "sna/6128loader.sna";


    public Cpc6128LoaderResources() {
        super(LOW_ROM_RESOURCE, HIGH_ROM_RESOURCE,
                SNA_LOADER_RESOURCE);
    }
}
