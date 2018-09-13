package com.grelobites.romgenerator.util.emulator;

@FunctionalInterface
public interface InterruptAckListener {
    void onInterruptAck(long tstates);
}
