package com.grelobites.romgenerator.util.romsethandler;

import com.grelobites.romgenerator.handlers.dandanatorcpc.v2.DandanatorCpcV2RomSetHandler;

public enum RomSetHandlerType {
    DDNTR_V2(DandanatorCpcV2RomSetHandler.class, "Dandanator Cpc V2", true);

    private Class<? extends RomSetHandler> handler;
    private String displayName;
    private boolean enabled;

    RomSetHandlerType(Class<? extends RomSetHandler> handler, String displayName, boolean enabled) {
        this.handler = handler;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public static RomSetHandlerType fromString(String type) {
        return RomSetHandlerType.valueOf(type.toUpperCase());
    }

    public Class<? extends RomSetHandler> handler() {
        return handler;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
