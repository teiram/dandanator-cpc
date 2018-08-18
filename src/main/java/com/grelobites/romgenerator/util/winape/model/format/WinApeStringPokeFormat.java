package com.grelobites.romgenerator.util.winape.model.format;

import com.grelobites.romgenerator.util.winape.model.PokConstants;
import com.grelobites.romgenerator.util.winape.model.WinApePoke;
import com.grelobites.romgenerator.util.winape.model.WinApePokeFormat;
import com.grelobites.romgenerator.util.winape.model.WinApePokeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WinApeStringPokeFormat implements WinApePokeFormat {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApeStringPokeFormat.class);

    @Override
    public String render(WinApePoke poke) {
        StringBuilder sb = new StringBuilder();
        for (Integer p : poke.getValue().values()) {
            sb.append(p != PokConstants.USER_VALUE ? String.format("%c", p) : "?");
            sb.append(" ");
        }
        LOGGER.debug("Rendering poke {} as {}", poke, sb.toString());
        return sb.toString();
    }

    @Override
    public boolean validate(WinApePoke poke, WinApePokeValue value) {
        if (value.size() == poke.getRequiredSize()) {
            for (Integer p : value.values()) {
                if (p == null) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public WinApePokeValue generate(WinApePoke poke, String value) {
        Integer[] values = new Integer[poke.getRequiredSize()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (int) value.charAt(i);
        }
        return WinApePokeValue.fromValues(values);
    }
}
