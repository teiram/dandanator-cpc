package com.grelobites.romgenerator.util.sna;

import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.sna.SnaCompressedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SnaFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnaFactory.class);


    public static SnaImage fromInputStream(InputStream is) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(is))
                .order(ByteOrder.LITTLE_ENDIAN);
        byte[] signature = new byte[SnaConstants.SNA_SIGNATURE.length()];
        buffer.get(signature);
        if (SnaConstants.SNA_SIGNATURE.equals(new String(signature))) {
            int version = Byte.toUnsignedInt(buffer.get(0x10));
            LOGGER.debug("SNA Version {}", version);
            switch (version) {
                case 1:
                    return SnaImageV1.fromBuffer(buffer);
                case 2:
                    return SnaImageV2.fromBuffer(buffer);
                case 3:
                    return SnaImageV3.fromBuffer(buffer);
                default:
                    throw new IllegalArgumentException("Unsupported SNA version " + version);
            }
        } else {
            throw new IllegalArgumentException("No SNA signature found in file");
        }
    }

    private static SnaChunk createSnaChunk(SnapshotGame game, int[] slots,
                                           String chunkName) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SnaCompressedOutputStream cs = new SnaCompressedOutputStream(os);
        for (int i : slots) {
            cs.write(game.getSlot(i));
        }
        cs.flush();
        return new SnaChunk(SnaChunk.CHUNK_MEM0, os.toByteArray());
    }

    public static SnaImage fromSnapshotGame(SnapshotGame game) throws IOException {
        SnaImageV3 snaImage = new SnaImageV3();
        GameHeader header = game.getGameHeader();
        snaImage.setAfRegister(header.getAfRegister());
        snaImage.setBcRegister(header.getBcRegister());
        snaImage.setDeRegister(header.getDeRegister());
        snaImage.setHlRegister(header.getHlRegister());
        snaImage.setIxRegister(header.getIxRegister());
        snaImage.setIyRegister(header.getIyRegister());
        snaImage.setiRegister(header.getiRegister());
        snaImage.setrRegister(header.getrRegister());
        snaImage.setPc(header.getPc());
        snaImage.setSp(header.getSp());
        snaImage.setIff0(header.getIff0());
        snaImage.setIff1(header.getIff0());
        snaImage.setInterruptMode(header.getInterruptMode());
        snaImage.setAltAfRegister(header.getAltAfRegister());
        snaImage.setAltBcRegister(header.getAltBcRegister());
        snaImage.setAltDeRegister(header.getAltDeRegister());
        snaImage.setAltHlRegister(header.getAltHlRegister());
        snaImage.setGateArraySelectedPen(header.getGateArraySelectedPen());
        snaImage.setGateArrayMultiConfiguration(header.getGateArrayMultiConfiguration());
        snaImage.setGateArrayCurrentPalette(header.getGateArrayCurrentPalette());
        snaImage.setCurrentRamConfiguration(header.getCurrentRamConfiguration());
        snaImage.setCrtcSelectedRegisterIndex(header.getCrtcSelectedRegisterIndex());
        snaImage.setCrtcRegisterData(header.getCrtcRegisterData());
        snaImage.setCurrentRomSelection(header.getCurrentRomSelection());
        snaImage.setPpiPortA(header.getPpiPortA());
        snaImage.setPpiPortB(header.getPpiPortB());
        snaImage.setPpiPortC(header.getPpiPortC());
        snaImage.setPpiControlPort(header.getPpiControlPort());
        snaImage.setPsgSelectedRegisterIndex(header.getPsgSelectedRegisterIndex());
        snaImage.setPsgRegisterData(header.getPsgRegisterData());

        snaImage.setCpcType(header.getCpcType());
        snaImage.setFddMotorDriveState(header.getFddMotorDriveState());

        if (header.getMemoryDumpSize() > 0) {
            snaImage.getSnaChunks().put(SnaChunk.CHUNK_MEM0,
                    createSnaChunk(game, new int[]{0, 1, 2, 3}, SnaChunk.CHUNK_MEM0));
            if (header.getMemoryDumpSize() > 64) {
                snaImage.getSnaChunks().put(SnaChunk.CHUNK_MEM1,
                        createSnaChunk(game, new int[]{4, 5, 6, 7}, SnaChunk.CHUNK_MEM1));
            }
        } else {
            throw new IllegalArgumentException("Illegal peripheral dump size");
        }
        return snaImage;
    }

}
