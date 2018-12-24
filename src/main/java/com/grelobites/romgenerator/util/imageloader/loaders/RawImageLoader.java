package com.grelobites.romgenerator.util.imageloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.imageloader.ImageLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RawImageLoader implements ImageLoader {
    private static final int FILE_SIZE = Constants.CPC_SCREEN_WITH_PALETTE_SIZE;

    @Override
    public boolean supportsFile(File file) {
        if (file.canRead() && file.length() == FILE_SIZE) {
            return true;
        }
        return false;
    }

    @Override
    public byte[] asByteArray(File file) throws IOException {
        return Files.readAllBytes(Paths.get(file.getAbsolutePath()));
    }
}
