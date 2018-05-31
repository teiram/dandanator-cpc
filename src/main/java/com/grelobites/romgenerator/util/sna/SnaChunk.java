package com.grelobites.romgenerator.util.sna;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.sna.SnaCompressedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SnaChunk {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnaChunk.class);

    public static final String CHUNK_MEM0 = "MEM0";
    public static final String CHUNK_MEM1 = "MEM1";

    private String name;
    private byte[] data;

    public SnaChunk(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public static SnaChunk fromBuffer(ByteBuffer buffer) throws IOException {
        byte[] id = new byte[4];
        buffer.get(id);
        String name = new String(id);
        int size = buffer.getInt();
        LOGGER.debug("Detected chunk with name {} and size {}", name, size);

        byte[] bufferAsArray = buffer.array();
        SnaCompressedInputStream cs = new SnaCompressedInputStream(
                new ByteArrayInputStream(bufferAsArray, buffer.position(),
                        bufferAsArray.length - buffer.position()),
                    (int) size);
        SnaChunk chunk = new SnaChunk(name, Util.fromInputStream(cs));
        LOGGER.debug("Shifting buffer position {}", cs.getSourceMark());
        buffer.position(buffer.position() + cs.getSourceMark());
        return chunk;
    }

    @Override
    public String toString() {
        return "SnaChunk{" +
                "name='" + name + '\'' +
                ", data.length=" + data.length +
                '}';
    }
}
