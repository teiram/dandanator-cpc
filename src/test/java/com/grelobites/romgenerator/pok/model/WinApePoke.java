package com.grelobites.romgenerator.pok.model;

import com.grelobites.romgenerator.pok.PokInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class WinApePoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApePoke.class);
    private static final int USER_VALUE = 65535;
    private int address;
    private Integer[] values;

    private static Integer parseValue(int value) {
        return value == USER_VALUE ? null : value;
    }

    public WinApePoke(WinApePoke c) {
        LOGGER.debug("Copy constructor on {}", c);
        this.address = c.address;
        this.values = new Integer[c.values.length];
        for (int i = 0; i < c.values.length; i++) {
            this.values[i] = c.values[i];
        }
    }

    public WinApePoke() {}

    public static WinApePoke fromPokInputStream(PokInputStream is) throws IOException {
        WinApePoke poke = new WinApePoke();
        int size = Util.readAsLittleEndian(is);
        poke.setAddress(Util.readAsLittleEndian(is));
        Integer[] values = new Integer[size];
        for (int i = 0; i < size; i++) {
            values[i] = parseValue(Util.readAsLittleEndian(is));
        }
        poke.setValues(values);
        return poke;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public Integer[] getValues() {
        return values;
    }

    public void setValues(Integer[] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "Poke{" +
                "address=" + address +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}
