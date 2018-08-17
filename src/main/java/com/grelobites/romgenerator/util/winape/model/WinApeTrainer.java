package com.grelobites.romgenerator.util.winape.model;


import com.grelobites.romgenerator.util.winape.PokInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WinApeTrainer implements Comparable<WinApeTrainer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApeTrainer.class);
    private String description;
    private String comment;
    private PokeValueType valueType;
    private boolean reversed;
    private int ramBank;
    private List<WinApePoke> pokes = new ArrayList<>();

    public static WinApeTrainer fromPokInputStream(PokInputStream is) throws IOException {
        WinApeTrainer trainer = new WinApeTrainer();
        trainer.setDescription(is.nextString());
        trainer.setComment(is.nextString());
        trainer.setValueType(PokeValueType.fromId(is.read()));
        trainer.setReversed(is.read() != 0);
        trainer.setRamBank(is.read());
        int numPokes = is.nextNumber();
        for (int i = 0; i < numPokes; i++) {
            trainer.getPokes().add(WinApePoke.fromPokInputStream(trainer, is));
        }
        return trainer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public PokeValueType getValueType() {
        return valueType;
    }

    public void setValueType(PokeValueType valueType) {
        this.valueType = valueType;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public int getRamBank() {
        return ramBank;
    }

    public void setRamBank(int ramBank) {
        this.ramBank = ramBank;
    }

    public int getNumPokes() {
        return pokes.size();
    }

    public List<WinApePoke> getPokes() {
        return pokes;
    }

    public void setPokes(List<WinApePoke> pokes) {
        this.pokes = pokes;
    }

    public boolean exportable() {
        for (WinApePoke poke : pokes) {
            if (!poke.exportable()) {
                LOGGER.debug("Some poke not exportable");
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(WinApeTrainer o) {
        if (o != null) {
            return description.compareTo(o.getDescription());
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return "WinApeTrainer{" +
                "description='" + description + '\'' +
                ", comment='" + comment + '\'' +
                ", valueType=" + valueType +
                ", reversed=" + reversed +
                ", ramBank=" + String.format("0x%02x", ramBank) +
                ", pokes=" + pokes +
                '}';
    }

}
