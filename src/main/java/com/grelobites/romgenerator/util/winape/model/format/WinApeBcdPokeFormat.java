package com.grelobites.romgenerator.util.winape.model.format;

import com.grelobites.romgenerator.util.winape.model.PokConstants;
import com.grelobites.romgenerator.util.winape.model.WinApePoke;
import com.grelobites.romgenerator.util.winape.model.WinApePokeFormat;
import com.grelobites.romgenerator.util.winape.model.WinApePokeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WinApeBcdPokeFormat implements WinApePokeFormat {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApeBcdPokeFormat.class);
    private static String asBcdPair(int value) {
        StringBuilder sb = new StringBuilder();
        sb.append((char)('0' + ((value & 0xf0) >> 4)));
        sb.append((char)('0' + (value & 0x0f)));
        return sb.toString();
    }

    @Override
    public String render(WinApePoke poke) {
        StringBuilder sb = new StringBuilder();
        for (Integer p : poke.getValue().values()) {
            sb.append(p != PokConstants.USER_VALUE ? asBcdPair(p) : "? ");
        }
        LOGGER.debug("Rendering poke {} as {}", poke, sb.toString());
        return sb.toString();
    }

    @Override
    public boolean validate(WinApePoke poke, WinApePokeValue value) {
        if (value.size() == poke.getRequiredSize()) {
            for (Integer p : value.values()) {
                if (p != null) {
                    if (!((p & 0xf) < 10 && (((p & 0xf0) >> 4) < 10))) {
                        return false;
                    }
                } else {
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
            values[i] = ((Character.getNumericValue(value.charAt(2 * i)) & 0x0f) << 4) |
                    (Character.getNumericValue(value.charAt(2 * i + 1)) & 0x0f);
        }
        return WinApePokeValue.fromValues(values);
    }

}
