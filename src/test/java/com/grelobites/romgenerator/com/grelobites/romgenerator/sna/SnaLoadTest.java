package com.grelobites.romgenerator.com.grelobites.romgenerator.sna;


import com.grelobites.romgenerator.util.sna.SnaFactory;
import com.grelobites.romgenerator.util.sna.SnaImage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SnaLoadTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnaLoadTest.class);
    @Test
    public void testLoadSna() throws IOException {
        SnaImage image =  SnaFactory.fromInputStream(
                SnaLoadTest.class.getResourceAsStream("/antiriad.sna"));

        LOGGER.debug("Image is {}", image);
    }
}
