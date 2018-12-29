package com.grelobites.romgenerator.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum HardwareMode {
    HW_CPC464("CPC464", 0, true),
    HW_CPC664("CPC664", 1, true),
    HW_CPC6128("CPC6128", 2, true),
    HW_UNKNOWN("Unknown", 3, false),
    HW_CPC6128PLUS("CPC6128+", 4, false),
    HW_CPC464PLUS("CPC464+", 5, false),
    HW_GX4000("GX4000", 6, false);

    private static final Logger LOGGER = LoggerFactory.getLogger(HardwareMode.class);
    private int snaValue;
    private String displayName;
    private boolean supported;

    HardwareMode(String displayName, int snaValue, boolean supported) {
        this.snaValue = snaValue;
        this.displayName = displayName;
        this.supported = supported;
    }

    public int snaValue() {
        return snaValue;
    }

    public String displayName() {
        return displayName;
    }

    public boolean supported() {
        return supported;
    }

    public static HardwareMode fromSnaType(int type) {
        for (HardwareMode hardwareMode : values()) {
            if (hardwareMode.snaValue() == type) {
                return hardwareMode;
            }
        }
        LOGGER.warn("No hardware mode detected for type {}", type);
        return null;
    }
}
