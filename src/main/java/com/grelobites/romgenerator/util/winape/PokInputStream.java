package com.grelobites.romgenerator.util.winape;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PokInputStream extends InputStream {
    private InputStream delegate;

    public PokInputStream(InputStream delegate) {
        this.delegate = delegate;
    }

    public String getHeader() throws IOException {
        byte[] h = new byte[4];
        delegate.read(h);
        return new String(h, StandardCharsets.US_ASCII);
    }

    public String nextString() throws IOException {
        int size = nextNumber();
        byte s[] = new byte[size];
        delegate.read(s);
        return new String(s, StandardCharsets.US_ASCII);
    }

    public int nextNumber() throws IOException {
        long nextValue;
        long result = 0;
        int offset = 0;
        boolean firstBlock = true;
        boolean negative = false;
        while ((nextValue = delegate.read()) != -1) {
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
        return delegate.read();
    }

}
