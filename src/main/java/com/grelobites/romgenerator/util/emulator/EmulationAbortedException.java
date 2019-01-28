package com.grelobites.romgenerator.util.emulator;

public class EmulationAbortedException extends RuntimeException {
    public EmulationAbortedException() {
    }

    public EmulationAbortedException(String message) {
        super(message);
    }

    public EmulationAbortedException(String message, Throwable cause) {
        super(message, cause);
    }
}
