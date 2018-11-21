package com.grelobites.romgenerator.util.tape.cdtblock;

import com.grelobites.romgenerator.util.tape.CdtBlock;
import com.grelobites.romgenerator.util.tape.CdtBlockId;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PauseBlock implements CdtBlock {
    private int pauseLength;

    public static class Builder {
        private PauseBlock block = new PauseBlock();

        public Builder withPauseLength(int pauseLength) {
            block.pauseLength = pauseLength;
            return this;
        }
        public PauseBlock build() {
            return block;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void dump(OutputStream os) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Integer.valueOf(CdtBlockId.SILENCE).byteValue());
        buffer.putShort(Integer.valueOf(pauseLength).shortValue());
        os.write(buffer.array());
    }

}
