package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.RamGameCompressor;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.zx7.Zx7OutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class DandanatorCpcRamGameCompressor implements RamGameCompressor {
    private static final int COMPRESSED_SLOT_THRESHOLD = Constants.SLOT_SIZE - 6;
    private static final int COMPRESSED_CHUNKSLOT_THRESHOLD = Constants.SLOT_SIZE -
            DandanatorCpcConstants.GAME_CHUNK_SIZE;
    private static final int DELTA_LIMITED_SLOT = 1;
    private static final int DELTA_LIMITED_SLOT_MAXDELTA = 16;

    private Compressor compressor = DandanatorCpcConfiguration.getInstance().getCompressor();
    private Integer compressionDelta;

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        OutputStream compressingStream = compressor.getCompressingOutputStream(os);
        compressingStream.write(data);
        compressingStream.flush();
        if (compressingStream instanceof Zx7OutputStream) {
            compressionDelta = ((Zx7OutputStream) compressingStream).getCompressionDelta();
        }
        return os.toByteArray();
    }

    private byte[] compressSlotInternal(byte[] data) {
        try {
            return compress(data);
        } catch (Exception e) {
            throw new RuntimeException("During compression of game data", e);
        }
    }

    private byte[] filterCompression(byte[] data, byte[] compressedData, int slot) {
        if (slot == DELTA_LIMITED_SLOT) {
            return compressionDelta > DELTA_LIMITED_SLOT_MAXDELTA ?
                    data : compressedData;
        }
        if (slot == DandanatorCpcConstants.GAME_CHUNK_SLOT) {
            return compressedData.length > COMPRESSED_CHUNKSLOT_THRESHOLD ?
                    data : compressedData;
        } else {
            return compressedData.length > COMPRESSED_SLOT_THRESHOLD ?
                    data : compressedData;
        }
    }

    @Override
    public byte[] compressSlot(int slot, byte[] data) {
        byte[] targetData = (slot == DandanatorCpcConstants.GAME_CHUNK_SLOT) ?
                Arrays.copyOfRange(data, 0, Constants.SLOT_SIZE - DandanatorCpcConstants.GAME_CHUNK_SIZE) :
                data;
        return filterCompression(targetData, compressSlotInternal(targetData), slot);
    }

}
