package com.grelobites.romgenerator.rgas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grelobites.romgenerator.util.imageloader.loaders.rgas.ByteArrayTypeAdapter;
import com.grelobites.romgenerator.util.imageloader.loaders.rgas.RgasFile;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.fail;

public class RgasTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(RgasTests.class);

    @Test
    public void rgasLoadTest() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter())
                .create();
        InputStream fileStream = RgasTests.class.getResourceAsStream("/image/test.rgas");

        try {
            RgasFile rgasFile = gson.fromJson(new InputStreamReader(fileStream), RgasFile.class);
            LOGGER.debug("Read {}", rgasFile);
            assertNotNull(rgasFile.getImageList());
            assertNotNull(rgasFile.getImageList().getValues());
            assertEquals(1, rgasFile.getImageList().getValues().size());
            assertEquals(0, rgasFile.getMode());
            assertEquals(160, rgasFile.getImageList().getValues().get(0).getWidth());
            assertEquals(200, rgasFile.getImageList().getValues().get(0).getHeight());
            assertNotNull(rgasFile.getInks());
            assertNotNull(rgasFile.getInks().getValue());
            assertEquals(16, rgasFile.getInks().getValue().length);
        } catch (Exception e) {
           LOGGER.error("Processing RGAS file", e);
           fail();
        }
    }
}
