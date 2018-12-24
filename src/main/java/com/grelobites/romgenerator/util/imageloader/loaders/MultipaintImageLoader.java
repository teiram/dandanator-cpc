package com.grelobites.romgenerator.util.imageloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.CpcColor;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.imageloader.ImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MultipaintImageLoader implements ImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipaintImageLoader.class);
    private static final int FILE_SIZE = 16486;
    private static final int LOADER_SIZE = 69;
    private static final int PALETTE_SIZE = 17;

    @Override
    public boolean supportsFile(File file) {
        if (file.canRead() && file.length() == FILE_SIZE) {
            return true;
        }
        return false;
    }

    private static void transformPaletteEntries(byte[] data, int offset) {
        for (int i = 0; i < PALETTE_SIZE; i++) {
            data[offset + i] = Integer.valueOf(CpcColor.snaValue(data[offset + i])).byteValue();
        }
    }

    @Override
    public byte[] asByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.skip(LOADER_SIZE);
            byte[] data = Util.fromInputStream(fis, Constants.CPC_SCREEN_WITH_PALETTE_SIZE);
            transformPaletteEntries(data, Constants.CPC_SCREEN_SIZE);
            return data;
        } catch (Exception e) {
            LOGGER.error("Reading file {}", file, e);
            throw new IOException(e);
        }
    }
}
