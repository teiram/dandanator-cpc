package com.grelobites.romgenerator.util.tape.cdtblock;

import com.grelobites.romgenerator.util.tape.CdtBlock;
import com.grelobites.romgenerator.util.tape.CdtBlockId;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class StandardSpeedDataBlock implements CdtBlock {
    private int pauseLength = 1000;
    private byte[] data;

    public static class Builder {
        private StandardSpeedDataBlock block = new StandardSpeedDataBlock();

        public Builder withPauseLength(int pauseLength) {
            block.pauseLength = pauseLength;
            return this;
        }

        public Builder withData(byte[] data) {
            block.data = data;
            return this;
        }

        public StandardSpeedDataBlock build() {
            return block;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void dump(OutputStream os) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Integer.valueOf(CdtBlockId.STANDARD_SPEED).byteValue());
        buffer.putShort(Integer.valueOf(pauseLength).shortValue());
        buffer.putShort(Integer.valueOf(data.length).shortValue());
        os.write(buffer.array());
        os.write(data);
    }

}
