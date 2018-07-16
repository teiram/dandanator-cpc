package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.Constants;

public class ExtendedCharSet {

    public static final byte[] SYMBOL_128K_0 = new byte[]{
            (byte) 0x03,
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x03,
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_128K_1 = new byte[]{
            (byte) 0xFF,
            (byte) 0x77,
            (byte) 0x6A,
            (byte) 0x7B,
            (byte) 0x76,
            (byte) 0x23,
            (byte) 0xFF,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_128K_2 = new byte[]{
            (byte) 0xC0,
            (byte) 0x60,
            (byte) 0xA0,
            (byte) 0x60,
            (byte) 0xA0,
            (byte) 0x60,
            (byte) 0xE0,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_64K_0 = new byte[]{
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x02,
            (byte) 0x02,
            (byte) 0x02,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x00
    };


    public static final byte[] SYMBOL_64K_1 = new byte[]{
            (byte) 0xFF,
            (byte) 0xEA,
            (byte) 0xEA,
            (byte) 0x22,
            (byte) 0xBA,
            (byte) 0x3A,
            (byte) 0xFF,
            (byte) 0x00
    };


    public static final byte[] SYMBOL_64K_2 = new byte[]{
            (byte) 0xC0,
            (byte) 0xA0,
            (byte) 0x60,
            (byte) 0xE0,
            (byte) 0x60,
            (byte) 0xA0,
            (byte) 0xE0,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_ROM_0 = new byte[]{
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x02,
            (byte) 0x02,
            (byte) 0x02,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_ROM_1 = new byte[]{
            (byte) 0xFF,
            (byte) 0x36,
            (byte) 0xAA,
            (byte) 0x2A,
            (byte) 0x6A,
            (byte) 0xB6,
            (byte) 0xFF,
            (byte) 0x0
    };

    public static final byte[] SYMBOL_ROM_2 = new byte[]{
            (byte) 0xC0,
            (byte) 0xA0,
            (byte) 0x20,
            (byte) 0x20,
            (byte) 0xA0,
            (byte) 0xA0,
            (byte) 0xE0,
            (byte) 0x00
    };

    /*
    public static final byte[] SYMBOL_LEFT_ARROW = new byte[] {
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x30,
            (byte) 0x60,
            (byte) 0xFF,
            (byte) 0x60,
            (byte) 0x30,
            (byte) 0x00
    };

    public static final byte[] SYMBOL_RIGHT_ARROW = new byte[] {
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x0C,
            (byte) 0x06,
            (byte) 0xFF,
            (byte) 0x06,
            (byte) 0x0C,
            (byte) 0x00
    };
*/

    public static final byte[] SYMBOL_LEFT_ARROW = new byte[] {
            (byte) 0x10,
            (byte) 0x30,
            (byte) 0x5F,
            (byte) 0x81,
            (byte) 0x81,
            (byte) 0x5F,
            (byte) 0x30,
            (byte) 0x10
    };

    public static final byte[] SYMBOL_RIGHT_ARROW = new byte[] {
            (byte) 0x08,
            (byte) 0x0C,
            (byte) 0xFA,
            (byte) 0x81,
            (byte) 0x81,
            (byte) 0xFA,
            (byte) 0x0C,
            (byte) 0x08
    };
    public static final int SYMBOL_SPACE = 32;
    public static final int CHARSET_OFFSET = SYMBOL_SPACE;

    public static final int SYMBOL_128K_0_CODE = 128;
    public static final int SYMBOL_128K_1_CODE = 129;
    public static final int SYMBOL_128K_2_CODE = 130;


    public static final int SYMBOL_64K_0_CODE = 131;
    public static final int SYMBOL_64K_1_CODE = 132;
    public static final int SYMBOL_64K_2_CODE = 133;


    public static final int SYMBOL_ROM_0_CODE = 134;
    public static final int SYMBOL_ROM_1_CODE = 135;
    public static final int SYMBOL_ROM_2_CODE = 136;
    public static final int SYMBOL_LEFT_ARROW_CODE = 137;
    public static final int SYMBOL_RIGHT_ARROW_CODE = 138;

    private byte[] charset;

    private static void copySymbolToCharset(byte[] charset, byte[] symbol, int code) {
        System.arraycopy(symbol, 0, charset,  (code - CHARSET_OFFSET) * 8, symbol.length);

    }
    private static void appendSymbolChars(byte[] charset) {
        copySymbolToCharset(charset, SYMBOL_128K_0, SYMBOL_128K_0_CODE);
        copySymbolToCharset(charset, SYMBOL_128K_1, SYMBOL_128K_1_CODE);
        copySymbolToCharset(charset, SYMBOL_128K_2, SYMBOL_128K_2_CODE);

        copySymbolToCharset(charset, SYMBOL_64K_0, SYMBOL_64K_0_CODE);
        copySymbolToCharset(charset, SYMBOL_64K_1, SYMBOL_64K_1_CODE);
        copySymbolToCharset(charset, SYMBOL_64K_2, SYMBOL_64K_2_CODE);

        copySymbolToCharset(charset, SYMBOL_ROM_0, SYMBOL_ROM_0_CODE);
        copySymbolToCharset(charset, SYMBOL_ROM_1, SYMBOL_ROM_1_CODE);
        copySymbolToCharset(charset, SYMBOL_ROM_2, SYMBOL_ROM_2_CODE);

        copySymbolToCharset(charset, SYMBOL_LEFT_ARROW, SYMBOL_LEFT_ARROW_CODE);
        copySymbolToCharset(charset, SYMBOL_RIGHT_ARROW, SYMBOL_RIGHT_ARROW_CODE);
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
