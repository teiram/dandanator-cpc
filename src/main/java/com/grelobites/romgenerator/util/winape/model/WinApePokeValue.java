package com.grelobites.romgenerator.util.winape.model;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.winape.PokInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class WinApePokeValue {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApePokeValue.class);
    private Integer[] values;


    public static WinApePokeValue fromPokInputStream(PokInputStream is, int size)
        throws IOException {
        WinApePokeValue poke = new WinApePokeValue();
        poke.values = new Integer[size];
        for (int i = 0; i < size; i++) {
            poke.values[i] = Util.readAsLittleEndian(is);
        }
        return poke;
    }

    public static WinApePokeValue fromValues(Integer[] values) {
        WinApePokeValue poke = new WinApePokeValue();
        poke.values = values;
        return poke;
    }

    public static WinApePokeValue fromString(String candidate) {
        WinApePokeValue poke = new WinApePokeValue();
        String[] stringValues = candidate.split(PokConstants.POKE_SEPARATOR);
        poke.values = new Integer[stringValues.length];
        for (int i = 0; i < stringValues.length; i++) {
            try {
                poke.values[i] = Integer.decode(stringValues[i]);
            } catch (Exception e) {
                LOGGER.warn("Unable to convert to integer provided value {}", stringValues[i]);
                poke.values[i] = PokConstants.USER_VALUE;
            }
        }
        return poke;
    }

    public WinApePokeValue(WinApePokeValue c) {
        values = new Integer[c.values.length];
        for (int i = 0; i < c.values.length; i++) {
            values[i] = c.values[i];
        }
    }

    private WinApePokeValue() {}

    public int size() {
        return values.length;
    }

    public boolean exportable() {
        for (Integer value : values) {
            if (value == PokConstants.USER_VALUE) {
                return false;
            }
        }
        return true;
    }

    public Integer[] values() {
        return values;
    }

    public String render() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            value.append(values[i] == 65535 ? PokConstants.USER_POKE_STR : values[i]);
            if (i < (values.length - 1)) {
                value.append(PokConstants.POKE_SEPARATOR + " ");
            }
        }
        return value.toString();
    }

    @Override
    public String toString() {
        return "WinApePokeValue{" +
                "values=" + Arrays.toString(values) +
                '}';
    }
}
