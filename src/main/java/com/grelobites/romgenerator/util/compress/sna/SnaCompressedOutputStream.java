package com.grelobites.romgenerator.util.compress.sna;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SnaCompressedOutputStream extends FilterOutputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnaCompressedOutputStream.class);

    private static final int CONTROL_BYTE = 0xE5;
    private int cachedValue;
    private int cachedValueCount;

    public SnaCompressedOutputStream(OutputStream out) {
        super(out);
    }

    private void flushCached() throws IOException {
        if (cachedValueCount > (cachedValue == CONTROL_BYTE ? 1 : 2)) {
            out.write(CONTROL_BYTE);
            out.write(cachedValueCount);
            out.write(cachedValue);
        } else {
            while (cachedValueCount-- > 0) {
                out.write(cachedValue);
            }
        }
        cachedValueCount = 0;
    }

    @Override
    public void write(int b) throws IOException {
        b &= 0xff;
        if (b == CONTROL_BYTE) {
            flushCached();
            out.write(CONTROL_BYTE);
            out.write(0);
        } else {
            if (cachedValueCount > 0) {
                if (cachedValue == b) {
                    cachedValueCount++;
                    if (cachedValueCount == 255) {
                        flushCached();
                    }
                } else {
                    flushCached();
                    cachedValue = b;
                    cachedValueCount = 1;
                }
            } else {
                cachedValue = b;
                cachedValueCount = 1;
            }
        }
    }

    @Override
    public void flush() throws IOException {
        flushCached();
        super.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
