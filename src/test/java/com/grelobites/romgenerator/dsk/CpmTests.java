package com.grelobites.romgenerator.dsk;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.Track;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;

public class CpmTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpmTests.class);
    @Test
    public void dumpLoadSector() throws IOException {
        DskContainer container = DskContainer.fromInputStream(
                CpmTests.class.getResourceAsStream("/dsk/cpm3.dsk"));
        //Search sector 0x41
        Track track0 = container.getTrack(0);
        for (int i = 0; i < track0.getInformation().getSectorCount(); i++) {
            if (track0.getInformation().getSectorInformation(i).getSectorId() == 0x41) {
                LOGGER.debug("Found boot sector at index {}", i);
                try (FileOutputStream fos = new FileOutputStream("output/bootcpm.img")) {
                    fos.write(track0.getSectorData(i));
                }
                return;
            }
        }
        LOGGER.error("Sector id 0x41 not found in track 0");
    }
}
