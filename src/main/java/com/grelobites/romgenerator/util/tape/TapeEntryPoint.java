package com.grelobites.romgenerator.util.tape;

import java.util.Optional;

public enum TapeEntryPoint {
    CAS_INITIALIZE(0xBC65),
    CAS_SET_SPEED(0xBC68),
    CAS_NOISY(0xBC6B),
    CAS_IN_OPEN(0xBC77),
    CAS_OUT_OPEN(0xBC8C),
    CAS_IN_CLOSE(0xBC7A),
    CAS_IN_ABANDON(0xBC7D),
    CAS_OUT_CLOSE(0xBC8F),
    CAS_OUT_ABANDON(0xBC92),
    CAS_IN_CHAR(0xBC80),
    CAS_OUT_CHAR(0xBC95),
    CAS_TEST_EOF(0xBC89),
    CAS_RETURN(0xBC86),
    CAS_IN_DIRECT(0xBC83),
    CAS_OUT_DIRECT(0xBC98),
    CAS_CATALOG(0xBC9B),
    CAS_READ(0xBCA1),
    CAS_WRITE(0xBC9E),
    CAS_CHECK(0xBCA4),
    CAS_START_MOTOR(0xBC6E),
    CAS_STOP_MOTOR(0xBC71),
    CAS_RESTORE_MOTOR(0xBC74);

    private int address;
    TapeEntryPoint(int address) {
        this.address = address;
    }

    public int address() {
        return address;
    }

    public static Optional<TapeEntryPoint> forAddress(int address) {
        for (TapeEntryPoint entryPoint : TapeEntryPoint.values()) {
            if (entryPoint.address == address) {
                return Optional.of(entryPoint);
            }
        }
        return Optional.empty();
    }
}
