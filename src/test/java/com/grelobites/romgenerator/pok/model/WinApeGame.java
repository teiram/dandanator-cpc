package com.grelobites.romgenerator.pok.model;

import com.grelobites.romgenerator.pok.PokImportTest;
import com.grelobites.romgenerator.pok.PokInputStream;
import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WinApeGame implements Comparable<WinApeGame> {
    private String name;
    private byte[] idData;
    private int idAddr;
    private List<WinApeTrainer> trainers = new ArrayList<>();

    public static WinApeGame fromPokInputStream(PokInputStream is) throws IOException {
        WinApeGame game = new WinApeGame();
        game.setName(is.nextString());
        int idSize = Util.readAsLittleEndian(is);
        game.setIdAddr(Util.readAsLittleEndian(is));
        byte[] idData = new byte[idSize];
        is.read(idData);
        game.setIdData(idData);
        int numTrainers = is.nextNumber();
        for (int i = 0; i < numTrainers; i++) {
            game.getTrainers().add(WinApeTrainer.fromPokInputStream(is));
        }
        return game;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getIdData() {
        return idData;
    }

    public void setIdData(byte[] idData) {
        this.idData = idData;
    }

    public int getIdAddr() {
        return idAddr;
    }

    public void setIdAddr(int idAddr) {
        this.idAddr = idAddr;
    }

    public int getNumTrainers() {
        return trainers.size();
    }

    public List<WinApeTrainer> getTrainers() {
        return trainers;
    }

    public void setTrainers(List<WinApeTrainer> trainers) {
        this.trainers = trainers;
    }

    @Override
    public int compareTo(WinApeGame o) {
        if (o != null) {
            return name.compareTo(o.getName());
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return "WinApeGame{" +
                "name='" + name + '\'' +
                ", idData=" + Arrays.toString(idData) +
                ", idAddr=" + idAddr +
                ", trainers=" + trainers +
                '}';
    }
}
