package com.grelobites.romgenerator.util.emulator;

/*
 * CRTC types 3 and 4 are not emulated properly regarding read capabilities
 * Since they return internal status register values for some of the requests
 * See: http://cpctech.cpc-live.com/docs/cpcplus.html
 */
public enum CrtcType {
    CRTC_TYPE_0(0x3F000, false, false),
    CRTC_TYPE_1(0x3C000, true, true),
    CRTC_TYPE_2(0x3C000, false, false),
    CRTC_TYPE_3(0x3F000, true, false),
    CRTC_TYPE_4(0x3F000, true, false);

    private int registerReadCapability;
    private boolean hasFunction2;
    private boolean hasReadStatusFunction;

    CrtcType(int registerReadCapability, boolean hasFunction2,
             boolean hasReadStatusFunction) {
        this.registerReadCapability = registerReadCapability;
        this.hasFunction2 = hasFunction2;
        this.hasReadStatusFunction = hasReadStatusFunction;
    }

    public boolean canReadRegister(int registerIndex) {
        return (registerReadCapability & (1 << registerIndex)) != 0;
    }

    public boolean hasReadStatusFunction() {
        return hasReadStatusFunction;
    }

    public boolean hasFunction2() {
        return hasFunction2;
    }
}
