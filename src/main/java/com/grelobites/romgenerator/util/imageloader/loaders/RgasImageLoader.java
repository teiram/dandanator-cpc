package com.grelobites.romgenerator.util.imageloader.loaders;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.CpcColor;
import com.grelobites.romgenerator.util.imageloader.ImageLoader;
import com.grelobites.romgenerator.util.imageloader.loaders.rgas.ByteArrayTypeAdapter;
import com.grelobites.romgenerator.util.imageloader.loaders.rgas.RgasByteArray;
import com.grelobites.romgenerator.util.imageloader.loaders.rgas.RgasFile;
import com.grelobites.romgenerator.util.imageloader.loaders.rgas.RgasImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class RgasImageLoader implements ImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(RgasImageLoader.class);

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter())
            .create();

    private static Optional<RgasImage> getSuitableImage(RgasFile rgasFile) {
        if (rgasFile.getImageList() != null) {
            return rgasFile.getImageList().getValues().stream()
                    .filter(i -> i.getWidth() == 160 && i.getHeight() == 200)
                    .findAny();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean supportsFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            RgasFile rgasFile = gson.fromJson(new InputStreamReader(fis, StandardCharsets.UTF_8), RgasFile.class);
            if (rgasFile != null) {
                if (rgasFile.getMode() == 0 && getSuitableImage(rgasFile).isPresent()) {
                    return true;
                } else {
                    LOGGER.info("Invalid mode or no suitable image found in rgas file {}", rgasFile);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Testing RGAS file candidate", e);
        }
        return false;
    }

    private static void toMode0(RgasImage image, byte[] dst, int offset) {
        // bit7 bit6 bit5 bit4 bit3 bit2 bit1 bit0
        // p0b0 p1b0 p0b2 p1b2 p0b1 p1b1 p0b3 p1b3
        byte[] pixelmasks = {
                0x00, 0x40, 0x04, 0x44,
                0x10, 0x50, 0x14, 0x54,
                0x01, 0x41, 0x05, 0x45,
                0x11, 0x51, 0x15, 0x55};

        byte[] src = image.getPixels().getValue();
        for (int y = 0; y < image.getHeight(); y++) {
            int scanlineWidth = image.getWidth() >> 1;
            int scanLineOffset = (((y / 8) * scanlineWidth)) + ((y % 8) * 2048);
            for (int x = 0; x < scanlineWidth; x++) {
                dst[offset + scanLineOffset + x] = (byte)
                        ((pixelmasks[src[(y * image.getWidth()) + (2 * x)]] << 1) |
                        pixelmasks[src[(y * image.getWidth()) + (2 * x) + 1]]);
            }
        }
    }

    private static void transformPaletteEntries(byte[] data, int offset) {
        for (int i = 0; i < 16; i++) {
            data[offset + i] = Integer.valueOf(CpcColor.snaValue(data[offset + i])).byteValue();
        }
    }

    private static byte[] rgasToByteArray(RgasImage image, RgasByteArray inks) {
        byte[] result = new byte[Constants.CPC_SCREEN_WITH_PALETTE_SIZE];
        toMode0(image, result, 0);
        System.arraycopy(inks.getValue(), 0, result, Constants.CPC_SCREEN_SIZE, 16);
        transformPaletteEntries(result, Constants.CPC_SCREEN_SIZE);
        return result;
    }

    @Override
    public byte[] asByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            RgasFile rgasFile = gson.fromJson(new InputStreamReader(fis, StandardCharsets.UTF_8), RgasFile.class);
            if (rgasFile != null) {
                return getSuitableImage(rgasFile)
                        .map(i -> rgasToByteArray(i, rgasFile.getInks()))
                        .orElseThrow(() -> new IllegalArgumentException("No suitable image found"));
            } else {
                throw new IllegalArgumentException("Unable to interpret rgas stream");
            }
        } catch (Exception e) {
            LOGGER.error("Reading file {}", file, e);
            throw new IOException(e);
        }
    }
}
