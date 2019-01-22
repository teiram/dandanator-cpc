package com.grelobites.romgenerator.cdt;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.tape.Binary;
import com.grelobites.romgenerator.util.tape.CdtBuilder;
import org.junit.Test;

import java.io.FileOutputStream;

public class RescueCdtTests {

    @Test
    public void generateRescueCdtTest() throws Exception {
        CdtBuilder builder = new CdtBuilder();
        builder.addBinary(Binary.builder()
                .withData(Constants.getRescueEewriter())
                .withLoadAddress(0x6000)
                .withExecAddress(0x6000)
                .withName("RESCUE EEWRITER")
                .build());
        try (FileOutputStream fos = new FileOutputStream("output/rescue.cdt")) {
            builder.dump(fos);
        }
    }
}
