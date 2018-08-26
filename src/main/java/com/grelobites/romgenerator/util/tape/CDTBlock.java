package com.grelobites.romgenerator.util.tape;

public class CDTBlock {
    public static final int STANDARD_SPEED = 0x10;
    public static final int TURBO_SPEED = 0x11;
    public static final int PURE_TONE = 0x12;
    public static final int PULSE_SEQUENCE = 0x13;
    public static final int PURE_DATA_BLOCK = 0x14;
    public static final int DIRECT_RECORDING = 0x15;
    public static final int CSW_RECORDING = 0x18;
    public static final int GENERALIZED_DATA = 0x19;
    public static final int SILENCE = 0x20;
    public static final int GROUP_START = 0x21;
    public static final int GROUP_END = 0x22;
    public static final int JUMP_TO_BLOCK = 0x23;
    public static final int LOOP_START = 0x24;
    public static final int LOOP_END = 0x25;
    public static final int CALL_SEQUENCE = 0x26;
    public static final int RETURN_FROM_SEQUENCE = 0x27;
    public static final int SELECT_BLOCK = 0x28;
    public static final int STOP_TAPE_48KMODE = 0x2A;
    public static final int SET_SIGNAL_LEVEL = 0x2B;
    public static final int TEXT_DESCRIPTION = 0x30;
    public static final int MESSAGE_BLOCK = 0x31;
    public static final int ARCHIVE_INFO = 0x32;
    public static final int HARDWARE_TYPE = 0x33;
    public static final int CUSTOM_INFO_BOCK = 0x35;
    public static final int GLUE_BLOCK = 0x5A;

}
