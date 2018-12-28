package com.grelobites.romgenerator.handlers.dandanatorcpc.v2;

public class V2Constants {
    protected static final int VERSION_SIZE = 8;
    protected static final int CBLOCKS_OFFSET = 16362;
    protected static final int GAME_STRUCT_SIZE = 217;
    protected static final int VERSION_OFFSET = 16354;

    protected static final int BASEROM_SIZE = 4096;
    protected static final int GAME_HEADER_SIZE = 90;
    protected static final int GAME_LAUNCHCODE_SIZE = 9;
    protected static final int GAME_STRUCT_OFFSET = BASEROM_SIZE + 1;
    protected static final int EXTRA_ROM_PRESENT_OFFSET = 16350;
    protected static int EXTRA_ROM_SLOT = 32;
    protected static int INTERNAL_ROM_SLOT = 33;
}
