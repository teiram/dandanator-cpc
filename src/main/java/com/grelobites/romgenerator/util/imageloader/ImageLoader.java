package com.grelobites.romgenerator.util.imageloader;

import java.io.File;
import java.io.IOException;

public interface ImageLoader {
    boolean supportsFile(File file);
    byte[] asByteArray(File file) throws IOException;
}
