package com.grelobites.romgenerator.util.winape;

import com.grelobites.romgenerator.util.winape.model.PokeValueType;
import com.grelobites.romgenerator.util.winape.model.WinApePokeFormat;
import com.grelobites.romgenerator.util.winape.model.format.WinApeBcdPokeFormat;
import com.grelobites.romgenerator.util.winape.model.format.WinApeDecimalPokeFormat;
import com.grelobites.romgenerator.util.winape.model.format.WinApeNumericPokeFormat;
import com.grelobites.romgenerator.util.winape.model.format.WinApeStringPokeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WinApePokeFormatFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApePokeFormatFactory.class);
    public static WinApePokeFormat forPokeValueType(PokeValueType valueType) {
        switch (valueType) {
            case BCD:
                return new WinApeBcdPokeFormat();
            case DECIMAL:
                return new WinApeDecimalPokeFormat();
            case NUMERIC:
                return new WinApeNumericPokeFormat();
            case STRING:
                return new WinApeStringPokeFormat();
            default:
                LOGGER.warn("Running default WinApePokeFormat");
                return new WinApeDecimalPokeFormat();
        }
    }
}
