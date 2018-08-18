package com.grelobites.romgenerator.util.winape;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PokInputStream extends FilterInputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokInputStream.class);

    public PokInputStream(InputStream delegate) {
        super(delegate);
    }

    public String getHeader() throws IOException {
        byte[] h = new byte[4];
        in.read(h);
        return new String(h, StandardCharsets.US_ASCII);
    }

    public String nextString() throws IOException {
        int size = nextNumber();
        byte s[] = new byte[size];
        int read = in.read(s);
        return new String(s, StandardCharsets.US_ASCII);
    }

    public int nextNumber() throws IOException {
        long nextValue;
        long result = 0;
        int offset = 0;
        boolean firstBlock = true;
        boolean negative = false;
        while ((nextValue = in.read()) != -1) {
            boolean hasMore = (nextValue & 0x80) != 0;
            if (firstBlock) {
                result = nextValue & 0x3F;
                negative = (nextValue & 0x40) != 0;
                firstBlock = false;
                offset = 6;
            } else {
                result |= (nextValue & 0x7F) << offset;
                offset += 7;
            }
            if (!hasMore) break;
        }
        return Long.valueOf(negative ? -result : result).intValue();
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

}
