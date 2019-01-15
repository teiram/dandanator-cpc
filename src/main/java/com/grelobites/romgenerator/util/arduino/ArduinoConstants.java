package com.grelobites.romgenerator.util.arduino;

import java.io.InputStream;

public class ArduinoConstants {
    private static final String HEX_RESOURCE = "/cpld-programmer/jtag-updater.hex";
    private static final String XSVF_RESOURCE = "/cpld-programmer/dandanator-cpld.xsvf";

    public static InputStream hexResource() {
        return ArduinoConstants.class.getResourceAsStream(HEX_RESOURCE);
    }

    public static InputStream xsvfResource() {
        return ArduinoConstants.class.getResourceAsStream(XSVF_RESOURCE);
    }
}
