package com.grelobites.romgenerator.util.emulator;

public class Cpc464LoaderResources extends LoaderResourcesBase {
    private static final String LOW_ROM_RESOURCE = "rom/464/OS_464.ROM";
    private static final String HIGH_ROM_RESOURCE = "rom/464/BASIC_1.0.ROM";
    private static final String SNA_LOADER_RESOURCE = "sna/464loader.sna";

    public Cpc464LoaderResources() {
        super(LOW_ROM_RESOURCE, HIGH_ROM_RESOURCE,
                SNA_LOADER_RESOURCE);
    }
}
