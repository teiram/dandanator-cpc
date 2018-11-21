package com.grelobites.romgenerator.util.tape.cdtblock;

import com.grelobites.romgenerator.util.tape.CdtBlock;
import com.grelobites.romgenerator.util.tape.CdtBlockId;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TurboSpeedDataBlock implements CdtBlock {
    private int pauseLength = 1000;
    private int pilotLength = 2168;
    private int pilotPulses = 3223; //Value for data block
    private int sync1Length = 667;
    private int sync2Length = 735;
    private int zeroLength = 855;
    private int oneLength = 1710;
    private int lastByteBits = 8;
    private byte[] data;

    public static class Builder {
        private TurboSpeedDataBlock block = new TurboSpeedDataBlock();

        public Builder withPauseLength(int pauseLength) {
            block.pauseLength = pauseLength;
            return this;
        }

        public Builder withPilotLength(int pilotLength) {
            block.pilotLength = pilotLength;
            return this;
        }

        public Builder withPilotPulses(int pilotPulses) {
            block.pilotPulses = pilotPulses;
            return this;
        }

        public Builder withSync1Length(int sync1Length) {
            block.sync1Length = sync1Length;
            return this;
        }

        public Builder withSync2Length(int sync2Length) {
            block.sync2Length = sync2Length;
            return this;
        }

        public Builder withZeroLength(int zeroLength) {
            block.zeroLength = zeroLength;
            return this;
        }

        public Builder withOneLength(int oneLength) {
            block.oneLength = oneLength;
            return this;
        }

        public Builder withLastByteBits(int lastByteBits) {
            block.lastByteBits = lastByteBits;
            return this;
        }

        public Builder withData(byte[] data) {
            block.data = data;
            return this;
        }

        public TurboSpeedDataBlock build() {
            return block;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void dump(OutputStream os) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(Integer.valueOf(CdtBlockId.TURBO_SPEED).byteValue());
        buffer.putShort(Integer.valueOf(pilotLength).shortValue());
        buffer.putShort(Integer.valueOf(sync1Length).shortValue());
        buffer.putShort(Integer.valueOf(sync2Length).shortValue());
        buffer.putShort(Integer.valueOf(zeroLength).shortValue());
        buffer.putShort(Integer.valueOf(oneLength).shortValue());
        buffer.putShort(Integer.valueOf(pilotPulses).shortValue());
        buffer.put(Integer.valueOf(lastByteBits).byteValue());
        buffer.putShort(Integer.valueOf(pauseLength).shortValue());
        buffer.putShort(Integer.valueOf(data.length & 0xffff).shortValue());
        buffer.put(Integer.valueOf(data.length >> 16).byteValue());
        os.write(buffer.array());
        os.write(data);
    }

}
