package com.grelobites.romgenerator.util.emulator.memory;

import com.grelobites.romgenerator.util.emulator.Memory;

public class CpcMemory implements Memory {
    private static final int BANK_SIZE = 0x4000;
    private static final int LOW_ROM = 0;
    private static final int HIGH_ROM = 3;
    private GateArray gateArray;
    private byte[][] ramBanks;
    private byte[][] romBanks;

    public CpcMemory(GateArray gateArray) {
        this.gateArray = gateArray;
        this.ramBanks = new byte[gateArray.hasRamBanking() ? 8 : 4][];
        for (int i = 0; i < ramBanks.length; i++) {
            ramBanks[i] = new byte[BANK_SIZE];
        }
        this.romBanks = new byte[4][];
        this.romBanks[LOW_ROM] = new byte[BANK_SIZE];
        this.romBanks[HIGH_ROM] = new byte[BANK_SIZE];
    }

    private byte[] bankSlot(int address, boolean write) {
        int bankSlot = gateArray.getMemoryBankSlot(address);
        byte[][] target = !write && (
                (bankSlot == 0 && gateArray.isLowRomEnabled()) ||
                        (bankSlot == 3 && gateArray.isHighRomEnabled())) ?
                romBanks : ramBanks;
        return target[bankSlot];
    }

    private int bankAddress(int address) {
        return address % BANK_SIZE;
    }

    @Override
    public int peek8(int address) {
        return bankSlot(address, false)[bankAddress(address)];
    }

    @Override
    public void poke8(int address, int value) {
        bankSlot(address, true)[bankAddress(address)] = (byte) value;
    }

    @Override
    public int peek16(int address) {
        int lsb = peek8(address);
        int msb = peek8(address + 1);
        return (msb << 8) | lsb;
    }

    @Override
    public void poke16(int address, int word) {
        poke8(address, word);
        poke8(address + 1, word >>> 8);
    }

    private void loadRom(byte[] source, int romId) {
        System.arraycopy(source, 0, romBanks[romId], 0, BANK_SIZE);
    }

    public void loadLowRom(byte[] source) {
        loadRom(source, LOW_ROM);
    }

    public void loadHighRom(byte[] source) {
        loadRom(source, HIGH_ROM);
    }

}
