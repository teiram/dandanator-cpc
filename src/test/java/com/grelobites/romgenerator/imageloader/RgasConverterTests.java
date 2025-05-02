package com.grelobites.romgenerator.imageloader;

import com.grelobites.romgenerator.util.imageloader.loaders.RgasImageLoader;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

import static junit.framework.TestCase.fail;

public class RgasConverterTests {

    @Test
    public void convertToBinaryFile() throws Exception {
        RgasImageLoader imageLoader = new RgasImageLoader();
        File input = new File("src/test/resources/image/test.rgas");
        if (imageLoader.supportsFile(input)) {
            try (FileOutputStream fos = new FileOutputStream("image.bin")) {
                fos.write(imageLoader.asByteArray(input));
            }
        } else {
            fail("Unsupported input file");
        }
    }
}

