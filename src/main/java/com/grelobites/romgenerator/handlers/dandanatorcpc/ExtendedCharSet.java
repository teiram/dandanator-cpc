package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.Constants;

public class ExtendedCharSet {

    public static final byte[] SYMBOL_128K_0 = new byte[]{
            (byte) 0x3F,
            (byte) 0x37,
            (byte) 0x26,
            (byte) 0x37,
            (byte) 0x37,
            (byte) 0x22,
            (byte) 0x1F,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_128K_1 = new byte[]{
            (byte) 0xFC,
            (byte) 0x76,
            (byte) 0xAA,
            (byte) 0xB6,
            (byte) 0x6A,
            (byte) 0x36,
            (byte) 0xFE,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_64K_0 = new byte[]{
            (byte) 0x3F,
            (byte) 0x2E,
            (byte) 0x2E,
            (byte) 0x22,
            (byte) 0x2B,
            (byte) 0x23,
            (byte) 0x1F,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_64K_1 = new byte[]{
            (byte) 0xFC,
            (byte) 0xAA,
            (byte) 0xA6,
            (byte) 0x2E,
            (byte) 0xA6,
            (byte) 0xAA,
            (byte) 0xFE,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_ROM_0 = new byte[]{
            (byte) 0x3F,
            (byte) 0x23,
            (byte) 0x2A,
            (byte) 0x22,
            (byte) 0x26,
            (byte) 0x2B,
            (byte) 0x1F,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_ROM_1 = new byte[]{
            (byte) 0xFC,
            (byte) 0x6A,
            (byte) 0xA2,
            (byte) 0xA2,
            (byte) 0xAA,
            (byte) 0x6A,
            (byte) 0xFE,
            (byte) 0x00
    };

    public static final int SYMBOL_SPACE = 32;
    public static final int CHARSET_OFFSET = SYMBOL_SPACE;

    public static final int SYMBOL_128K_0_CODE = 128;
    public static final int SYMBOL_128K_1_CODE = 129;

    public static final int SYMBOL_64K_0_CODE = 130;
    public static final int SYMBOL_64K_1_CODE = 131;

    public static final int SYMBOL_ROM_0_CODE = 132;
    public static final int SYMBOL_ROM_1_CODE = 133;

    private byte[] charset;

    private static void appendSymbolChars(byte[] charset) {
        System.arraycopy(SYMBOL_128K_0, 0, charset, (SYMBOL_128K_0_CODE - CHARSET_OFFSET) * 8, SYMBOL_128K_0.length);
        System.arraycopy(SYMBOL_128K_1, 0, charset, (SYMBOL_128K_1_CODE - CHARSET_OFFSET) * 8, SYMBOL_128K_1.length);
        System.arraycopy(SYMBOL_64K_0, 0, charset, (SYMBOL_64K_0_CODE - CHARSET_OFFSET) * 8, SYMBOL_64K_0.length);
        System.arraycopy(SYMBOL_64K_1, 0, charset, (SYMBOL_64K_1_CODE - CHARSET_OFFSET) * 8, SYMBOL_64K_1.length);
        System.arraycopy(SYMBOL_ROM_0, 0, charset, (SYMBOL_ROM_0_CODE - CHARSET_OFFSET) * 8, SYMBOL_ROM_0.length);
        System.arraycopy(SYMBOL_ROM_1, 0, charset, (SYMBOL_ROM_1_CODE - CHARSET_OFFSET) * 8, SYMBOL_ROM_1.length);
    }

    public ExtendedCharSet(byte[] charset) {
        this.charset = new byte[DandanatorCpcConstants.EXTENDED_CHARSET_SIZE];
        System.arraycopy(charset, 0, this.charset, 0, Constants.CHARSET_SIZE);
        appendSymbolChars(this.charset);
    }

    public byte[] getCharSet() {
        return charset;
    }
}
