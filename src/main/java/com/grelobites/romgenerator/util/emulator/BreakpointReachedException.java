package com.grelobites.romgenerator.util.emulator;

public class BreakpointReachedException extends RuntimeException {
    public BreakpointReachedException(String message) {
        super(message);
    }
}
