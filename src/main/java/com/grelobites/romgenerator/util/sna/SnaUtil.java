package com.grelobites.romgenerator.util.sna;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.SnapshotGame;

import java.io.IOException;
import java.io.OutputStream;

public class SnaUtil {
    public static void toSnaImageV1(SnapshotGame game, OutputStream os) throws IOException {
        SnaImageV1 snaImage = new SnaImageV1();
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

        byte[] ram = new byte[game.getSlotCount() * Constants.SLOT_SIZE];
        for (int i = 0; i < game.getSlotCount(); i++) {
            System.arraycopy(game.getSlot(i), 0, ram, i * Constants.SLOT_SIZE, Constants.SLOT_SIZE);
        }
        snaImage.setMemory(ram);
        snaImage.dump(os);

    }
}
