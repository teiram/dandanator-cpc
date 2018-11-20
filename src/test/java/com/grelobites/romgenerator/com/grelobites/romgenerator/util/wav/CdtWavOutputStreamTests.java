package com.grelobites.romgenerator.com.grelobites.romgenerator.util.wav;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.wav.CdtWavOutputStream;
import com.grelobites.romgenerator.util.wav.WavFormat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CdtWavOutputStreamTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdtWavOutputStreamTests.class);
    @Test
    public void cdtToWavTest() throws Exception {
        InputStream cdt = CdtWavOutputStreamTests.class.getResourceAsStream("/cdt/lala.cdt");
        OutputStream output = new FileOutputStream(new File("C:\\Users\\mteira\\Desktop\\lala.wav"));
        CdtWavOutputStream converter = new CdtWavOutputStream(WavFormat.DEFAULT_FORMAT, cdt, output);
        converter.flush();
    }

    // Bit-order MSB first (left to right).
    private static int slowCrc16MsbFirst(byte[] data, int initialValue, int poly) {
        int crc = initialValue;
        for (int p = 0; p < data.length; p++) {
            crc ^= (data[p] & 0xff) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = ((crc << 1) & 0xffff) ^ poly;
                } else {
                    crc = (crc << 1) & 0xffff;
                }
            }
        }
        return crc ^ 0xffff;
    }

    @Test
    public void crc16Test() throws Exception {
        InputStream cdt = CdtWavOutputStreamTests.class.getResourceAsStream("/cdt/lala.cdt");
        cdt.skip(0x21); //To the real block
        byte[] block = Util.fromInputStream(cdt, 256);
        LOGGER.debug("CRC16 is {}", String.format("0x%04x", slowCrc16MsbFirst(block, 0xffff, 0x1021)));
    }

}
