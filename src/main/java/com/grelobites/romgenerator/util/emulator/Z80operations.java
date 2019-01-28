package com.grelobites.romgenerator.util.emulator;

public interface Z80operations {
    int fetchOpcode(int address);

    int peek8(int address);
    void poke8(int address, int value);
    int peek16(int address);
    void poke16(int address, int word);

    int inPort(int port);
    void outPort(int port, int value);

    void breakpoint();
}
