package com.grelobites.romgenerator.util.filesystem;

public class CpmConstants {
    public static final byte EMPTY_BYTE = (byte) 0xE5;
    public static final byte UNUSED_ENTRY_USER = EMPTY_BYTE;
    public static final int DIRECTORY_ENTRY_SIZE = 32;
    public static final int FILENAME_MAXLENGTH = 8;
    public static final int FILEEXTENSION_MAXLENGTH = 3;
    public static final int LOGICAL_EXTENT_SHIFT = 14;
    public static final int LOGICAL_EXTENT_SIZE = 1 << LOGICAL_EXTENT_SHIFT;
    public static final int RECORD_SHIFT = 7;
    public static final int RECORD_SIZE = 1 << RECORD_SHIFT;

}
