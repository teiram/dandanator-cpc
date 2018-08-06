package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.zx7.Zx7InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

public class RomSetUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RomSetUtil.class);
    private static final String LOADER_NAME = "DivIDELoader";
    private static final String PAUSE_FILENAME = "pause.wav";
    private static final String PAUSE_RESOURCE = "/player/" + PAUSE_FILENAME;
    private static final String PLAYLIST_NAME = "loader.m3u";
    private static final int LOAD_ADDRESS = 0x6f00;
    private static final int BLOCK_SIZE = 0x8000;
    private static final int BLOCK_COUNT = 16;
    private static final String BLOCK_NAME_PREFIX = "block";
    private static final String MULTILOADER_SIGNATURE = "MLD";


    private static Optional<InputStream> getRomScreenResource(ByteBuffer buffer, int slot) {
        buffer.position(Constants.SLOT_SIZE * slot);
        byte[] magic = new byte[3];
        buffer.get(magic);
        if (MULTILOADER_SIGNATURE.equals(new String(magic))) {
            int version = Byte.toUnsignedInt(buffer.get());
            int offset = Short.toUnsignedInt(buffer.getShort());
            int size = Short.toUnsignedInt(buffer.getShort());
            LOGGER.debug("Detected Multiload ROMSet with version " + version);
            LOGGER.debug("Compressed screen at offset " + offset + ", size " + size);
            return Optional.of(new Zx7InputStream(new ByteArrayInputStream(buffer.array(),
                    offset + Constants.SLOT_SIZE * slot, size)));
        } else {
            return Optional.empty();
        }
    }

    public static byte[] getCompressedScreen(byte[] screenWithPalette) throws IOException {
        byte[] packedScreen = Arrays.copyOf(screenWithPalette, DandanatorCpcConstants.PACKED_SCREEN_SIZE);
        System.arraycopy(screenWithPalette, DandanatorCpcConstants.PACKED_SCREEN_SIZE,
                packedScreen, DandanatorCpcConstants.PACKED_SCREEN_SIZE - DandanatorCpcConstants.PALETTE_SIZE,
                DandanatorCpcConstants.PALETTE_SIZE);
        return Util.compress(packedScreen);
    }

    public static byte[] decodeCharset(byte[] encodedCharset) throws IOException {
        byte[] charset = new byte[Constants.CHARSET_SIZE + Constants.ICONS_SIZE];
        for (int i = 0; i < Constants.CHARSET_SIZE; i++) {
            charset[i] = Integer.valueOf((encodedCharset[2 * i] & 0xF0) |
                    (encodedCharset[(2 * i) + 1] & 0x0F)).byteValue();
        }
        System.arraycopy(Constants.getIcons(), 0, charset, Constants.CHARSET_SIZE,
                Constants.ICONS_SIZE);
        return charset;
    }

    public static byte[] encodeCharset(byte [] charset) {
        byte[] encoded = new byte[Constants.CHARSET_SIZE * 2 + Constants.ICONS_SIZE];
        for (int i = 0; i < Constants.CHARSET_SIZE; i++) {
            encoded[2 * i] = Integer.valueOf((((charset[i] & 0xF0))) >> 4 |
                    (charset[i] & 0xF0)).byteValue();
            encoded[(2 * i) + 1] = Integer.valueOf((((charset[i] & 0x0F)) << 4) |
                    (charset[i] & 0x0F)).byteValue();
        }
        System.arraycopy(charset, Constants.CHARSET_SIZE, encoded, Constants.CHARSET_SIZE * 2,
                Constants.ICONS_SIZE);
        return encoded;
    }

    private static boolean isPrintableAscii(String name) {
        for (int i = 0; i < name.length(); i++) {
            int code = name.charAt(i);
            if (code < 32 || code > 127) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidGameName(String name) {
        return name != null &&
                name.length() < DandanatorCpcConstants.GAMENAME_EFFECTIVE_SIZE &&
                isPrintableAscii(name);
    }

    public static boolean isValidPokeName(String name) {
        return name != null &&
                name.length() < DandanatorCpcConstants.POKE_EFFECTIVE_NAME_SIZE &&
                isPrintableAscii(name);
    }

}
