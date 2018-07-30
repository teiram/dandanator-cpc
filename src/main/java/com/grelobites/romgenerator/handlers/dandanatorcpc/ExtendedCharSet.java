package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.Constants;

import java.io.IOException;

public class ExtendedCharSet {
    public static final int SYMBOL_SPACE = 32;
    public static final int CHARSET_OFFSET = SYMBOL_SPACE;

    public static final int BASE_SYMBOLS_CODE = 128;
    public static final int SYMBOL_128K_0_CODE = BASE_SYMBOLS_CODE;
    public static final int SYMBOL_64K_0_CODE = BASE_SYMBOLS_CODE + 3;
    public static final int SYMBOL_ROM_0_CODE = BASE_SYMBOLS_CODE + 6;
    public static final int SYMBOL_LEFT_ARROW_CODE = BASE_SYMBOLS_CODE + 9;
    public static final int SYMBOL_RIGHT_ARROW_CODE = BASE_SYMBOLS_CODE + 10;

    private byte[] charset;

    public ExtendedCharSet(byte[] sourceCharset) throws IOException {
        charset = new byte[Constants.CHARSET_SIZE + Constants.ICONS_SIZE];
        System.arraycopy(sourceCharset, 0, charset, 0, Constants.CHARSET_SIZE);
        System.arraycopy(Constants.getIcons(), 0, this.charset, Constants.CHARSET_SIZE,
                Constants.ICONS_SIZE);
    }

    public byte[] getCharSet() {
        return charset;
    }
}
