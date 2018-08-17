package com.grelobites.romgenerator.util.winape.model;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.winape.PokInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WinApePoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApePoke.class);
    private static final int USER_VALUE = 65535;
    private int address;
    private WinApePokeValue originalValue;
    private WinApePokeValue value;
    private int requiredSize;
    private WinApeTrainer trainer;

    public WinApePoke() {}

    public static WinApePoke fromPokInputStream(WinApeTrainer trainer, PokInputStream is) throws IOException {
        WinApePoke poke = new WinApePoke();
        int size = Util.readAsLittleEndian(is);
        poke.setTrainer(trainer);
        poke.setRequiredSize(size);
        poke.setAddress(Util.readAsLittleEndian(is));
        poke.originalValue = WinApePokeValue.fromPokInputStream(is, size);
        poke.value = new WinApePokeValue(poke.originalValue);
        return poke;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public WinApePokeValue getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(WinApePokeValue originalValue) {
        this.originalValue = originalValue;
    }

    public int getRequiredSize() {
        return requiredSize;
    }

    public void setRequiredSize(int requiredSize) {
        this.requiredSize = requiredSize;
    }

    public WinApePokeValue getValue() {
        return value;
    }

    public void setValue(WinApePokeValue value) {
        this.value = value;
    }

    public WinApeTrainer getTrainer() {
        return trainer;
    }

    public void setTrainer(WinApeTrainer trainer) {
        this.trainer = trainer;
    }

    public boolean commitValues(WinApePokeValue newValue) {
        if (newValue.size() == requiredSize && newValue.exportable()) {
            this.value = newValue;
            return true;
        }
        return false;
    }

    public boolean exportable() {
        return value.exportable();
    }

    @Override
    public String toString() {
        return "WinApePoke{" +
                "address=" + address +
                ", originalValue=" + originalValue +
                ", value=" + value +
                ", requiredSize=" + requiredSize +
                '}';
    }
}
