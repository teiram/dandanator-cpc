package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.Constants;

import java.io.IOException;

public class ExtendedCharSet {
    public static final int SYMBOL_SPACE = 32;
    public static final int CHARSET_OFFSET = SYMBOL_SPACE;

    public static final int BASE_SYMBOLS_CODE = 128;
    public static final int[] SYMBOL_128K_CODES = {BASE_SYMBOLS_CODE, BASE_SYMBOLS_CODE + 1, BASE_SYMBOLS_CODE + 2};
    public static final int[] SYMBOL_64K_CODES = {BASE_SYMBOLS_CODE + 3, BASE_SYMBOLS_CODE + 4, BASE_SYMBOLS_CODE + 5};
    public static final int[] SYMBOL_ROM_CODES = {BASE_SYMBOLS_CODE + 6, BASE_SYMBOLS_CODE + 7, BASE_SYMBOLS_CODE + 8};
    public static final int SYMBOL_LEFT_ARROW_CODE = BASE_SYMBOLS_CODE + 9;
    public static final int SYMBOL_RIGHT_ARROW_CODE = BASE_SYMBOLS_CODE + 10;
    public static final int[] SYMBOL_MLD64_CODES = {BASE_SYMBOLS_CODE + 11, BASE_SYMBOLS_CODE + 12, BASE_SYMBOLS_CODE + 13};
    public static final int[] SYMBOL_MLD128_CODES = {BASE_SYMBOLS_CODE + 11, BASE_SYMBOLS_CODE + 14, BASE_SYMBOLS_CODE + 15};

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
