package com.grelobites.romgenerator.util.compress.sna;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class SnaCompressedInputStream extends InputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnaCompressedInputStream.class);
    private InputStream source;

    private int cachedValue = 0;
    private int cachedCount = 0;
    private int sourceMark = 0;
    private int sourceSize;

    private static final int CONTROL_BYTE = 0xE5;

    private int readNextValue() throws SourceStreamEOFException, IOException {
        if (sourceSize-- > 0) {
            int value = source.read();
            if (value < 0) {
                throw new SourceStreamEOFException();
            } else {
                sourceMark++;
                return value;
            }
        } else {
            LOGGER.debug("End of declared source stream");
            throw new SourceStreamEOFException();
        }
    }

    private int getNextValue() throws IOException {
        try {
            int value = readNextValue();
            if (value == CONTROL_BYTE) {
                cachedCount = readNextValue() - 1;
                if (cachedCount == -1) {
                    return CONTROL_BYTE;
                } else {
                    cachedValue = readNextValue();
                    return cachedValue;
                }
            } else {
                return value;
            }
        } catch (SourceStreamEOFException ssee) {
            return -1;
        }
    }

    public SnaCompressedInputStream(InputStream source, int sourceSize) {
        this.source = source;
        this.sourceSize = sourceSize;
    }

    public SnaCompressedInputStream(InputStream source) {
        this.source = source;
        this.sourceSize = Integer.MAX_VALUE;
    }

    public int getSourceMark() {
        return sourceMark;
    }

    @Override
    public int read() throws IOException {
        if (cachedCount > 0) {
            cachedCount--;
            return cachedValue;
        } else {
            return getNextValue();
        }
    }

    private static class SourceStreamEOFException extends Exception {}
}
