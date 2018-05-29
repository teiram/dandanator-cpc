package com.grelobites.romgenerator.handlers.dandanatorcpc.v1;

import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class GameHeaderV1Serializer {

    public static void serialize(SnapshotGame game, OutputStream os) throws IOException {
        GameHeader header = game.getGameHeader();
        ByteBuffer buffer = ByteBuffer.allocate(V1Constants.GAME_HEADER_SIZE)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(Integer.valueOf(header.getAfRegister()).shortValue());
        buffer.putShort(Integer.valueOf(header.getBcRegister()).shortValue());
        buffer.putShort(Integer.valueOf(header.getDeRegister()).shortValue());
        buffer.putShort(Integer.valueOf(header.getHlRegister()).shortValue());
        buffer.put(Integer.valueOf(header.getrRegister()).byteValue());
        buffer.put(Integer.valueOf(header.getiRegister()).byteValue());
        buffer.put(Integer.valueOf(header.getIff0()).byteValue());

        buffer.putShort(Integer.valueOf(header.getIxRegister()).shortValue());
        buffer.putShort(Integer.valueOf(header.getIyRegister()).shortValue());
        buffer.putShort(Integer.valueOf(header.getSp()).shortValue());
        buffer.putShort(Integer.valueOf(header.getPc()).shortValue());

        buffer.put(Integer.valueOf(header.getInterruptMode()).byteValue());

        buffer.putShort(Integer.valueOf(header.getAltAfRegister()).shortValue());
        buffer.putShort(Integer.valueOf(header.getAltBcRegister()).shortValue());
        buffer.putShort(Integer.valueOf(header.getAltDeRegister()).shortValue());
        buffer.putShort(Integer.valueOf(header.getAltHlRegister()).shortValue());

        buffer.put(Integer.valueOf(header.getGateArraySelectedPen()).byteValue());
        buffer.put(header.getGateArrayCurrentPalette(), 0, 17);
        buffer.put(Integer.valueOf(header.getGateArrayMultiConfiguration()).byteValue());

        buffer.put(Integer.valueOf(header.getCurrentRamConfiguration()).byteValue());
        buffer.put(Integer.valueOf(header.getCrtcSelectedRegisterIndex()).byteValue());
        buffer.put(header.getCrtcRegisterData(), 0, 16);
        buffer.put(Integer.valueOf(header.getCurrentRomSelection()).byteValue());

        buffer.put(Integer.valueOf(header.getPpiPortA()).byteValue());
        buffer.put(Integer.valueOf(header.getPpiPortB()).byteValue());
        buffer.put(Integer.valueOf(header.getPpiPortC()).byteValue());
        buffer.put(Integer.valueOf(header.getPpiControlPort()).byteValue());

        buffer.put(Integer.valueOf(header.getPsgSelectedRegisterIndex()).byteValue());
        buffer.put(header.getPsgRegisterData(), 0, 16);

        buffer.put(Integer.valueOf(header.getCpcType()).byteValue());
        buffer.put(Integer.valueOf(header.getFddMotorDriveState()).byteValue());

        os.write(buffer.array());
    }

    public static GameHeader deserialize(InputStream is) throws IOException {
        GameHeader header = new GameHeader();
        ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(is,
                V1Constants.GAME_HEADER_SIZE)).order(ByteOrder.LITTLE_ENDIAN);

        header.setAfRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setBcRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setDeRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setHlRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setrRegister(Byte.toUnsignedInt(buffer.get()));
        header.setiRegister(Byte.toUnsignedInt(buffer.get()));
        header.setIff0(Byte.toUnsignedInt(buffer.get()));

        header.setIxRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setIyRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setSp(Short.toUnsignedInt(buffer.getShort()));
        header.setPc(Short.toUnsignedInt(buffer.getShort()));

        header.setInterruptMode(Byte.toUnsignedInt(buffer.get()));

        header.setAltAfRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setAltBcRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setAltDeRegister(Short.toUnsignedInt(buffer.getShort()));
        header.setAltHlRegister(Short.toUnsignedInt(buffer.getShort()));

        header.setGateArraySelectedPen(Byte.toUnsignedInt(buffer.get()));
        byte[] currentPalette = new byte[17];
        buffer.get(currentPalette);
        header.setGateArrayCurrentPalette(currentPalette);
        header.setGateArrayMultiConfiguration(Byte.toUnsignedInt(buffer.get()));

        header.setCurrentRamConfiguration(Byte.toUnsignedInt(buffer.get()));
        header.setCrtcSelectedRegisterIndex(Byte.toUnsignedInt(buffer.get()));
        byte[] crtcRegisterData = new byte[16];
        buffer.get(crtcRegisterData);
        header.setCrtcRegisterData(crtcRegisterData);
        header.setCurrentRomSelection(Byte.toUnsignedInt(buffer.get()));

        header.setPpiPortA(Byte.toUnsignedInt(buffer.get()));
        header.setPpiPortB(Byte.toUnsignedInt(buffer.get()));
        header.setPpiPortC(Byte.toUnsignedInt(buffer.get()));
        header.setPpiControlPort(Byte.toUnsignedInt(buffer.get()));

        header.setPsgSelectedRegisterIndex(Byte.toUnsignedInt(buffer.get()));
        byte[] psgRegisterData = new byte[16];
        buffer.get(psgRegisterData);
        header.setPsgRegisterData(psgRegisterData);

        header.setCpcType(Byte.toUnsignedInt(buffer.get()));
        header.setFddMotorDriveState(Byte.toUnsignedInt(buffer.get()));

        return header;
    }

}
