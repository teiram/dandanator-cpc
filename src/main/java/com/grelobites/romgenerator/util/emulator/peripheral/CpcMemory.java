package com.grelobites.romgenerator.util.emulator.peripheral;

import com.grelobites.romgenerator.util.emulator.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CpcMemory implements Memory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpcMemory.class);
    private static final int BANK_SIZE = 0x4000;
    private static final int LOW_ROM = 0;
    private static final int HIGH_ROM = 3;
    private byte[] basicRom;
    private int upperRomNumber = 0;
    private GateArray gateArray;
    private byte[][] ramBanks;
    private byte[][] romBanks;
    private Map<Integer, byte[]> highRoms;

    public CpcMemory(GateArray gateArray) {
        this.gateArray = gateArray;
        this.ramBanks = new byte[gateArray.hasRamBanking() ? 8 : 4][];
        for (int i = 0; i < ramBanks.length; i++) {
            ramBanks[i] = new byte[BANK_SIZE];
        }
        this.romBanks = new byte[4][];
        this.romBanks[LOW_ROM] = new byte[BANK_SIZE];
        this.basicRom = new byte[BANK_SIZE];
        this.romBanks[HIGH_ROM] = basicRom;
        this.highRoms = new HashMap<>();
    }

    private byte[] bankSlot(int address, boolean write) {
        try {
            int bankSlot = gateArray.getMemoryBankSlot(address);
            byte[][] target = !write && (
                    (bankSlot == LOW_ROM && gateArray.isLowRomEnabled()) ||
                            (bankSlot == HIGH_ROM && gateArray.isHighRomEnabled())) ?
                    romBanks : ramBanks;
            return target[bankSlot];
        } catch (Exception e) {
            LOGGER.error("Getting bank slot for address {}, write {}", String.format("0x%04X", address), write);
            throw e;
        }
    }

    private int bankAddress(int address) {
        return address % BANK_SIZE;
    }

    public boolean isAddressInRam(int address) {
        int bankSlot = gateArray.getMemoryBankSlot(address);
        return bankSlot == 1 || bankSlot == 2 ||
                (bankSlot == LOW_ROM && !gateArray.isLowRomEnabled()) ||
                (bankSlot == HIGH_ROM && !gateArray.isHighRomEnabled());
    }

    @Override
    public int peek8(int address) {
        return bankSlot(address, false)[bankAddress(address)] & 0xff;
    }

    @Override
    public void poke8(int address, int value) {
        bankSlot(address, true)[bankAddress(address)] = (byte) value;
    }

    @Override
    public int peek16(int address) {
        int lsb = peek8(address);
        int msb = peek8((address + 1) & 0xffff);
        return (msb << 8) | lsb;
    }

    @Override
    public void poke16(int address, int word) {
        poke8(address, word);
        poke8((address + 1) & 0xffff, word >>> 8);
    }

    public void loadRamBank(byte[] source, int slot) {
        if (slot < ramBanks.length) {
            if (source.length == BANK_SIZE) {
                System.arraycopy(source, 0, ramBanks[slot], 0,
                        BANK_SIZE);
            } else {
                throw new IllegalArgumentException("Only 16K blocks can be loaded");
            }
        } else {
            throw new IllegalArgumentException("Slot exceeds current RAM configuration");
        }
    }

    public byte[] getRamBank(int address) {
        return ramBanks[address / BANK_SIZE];
    }

    private void loadRom(byte[] source, byte[] destination) {
        System.arraycopy(source, 0, destination, 0, BANK_SIZE);
    }
    private void loadRom(byte[] source, int romId) {
        loadRom(source, romBanks[romId]);
    }

    public void loadLowRom(byte[] source) {
        loadRom(source, LOW_ROM);
    }

    public void registerHighRom(int id, byte[] source) {
        byte[] copy = new byte[BANK_SIZE];
        loadRom(source, copy);
        highRoms.put(id, copy);
    }

    public void registerBasicRom(byte[] source) {
        loadRom(source, basicRom);
    }

    public void setUpperRomNumber(int upperRomNumber) {
        this.upperRomNumber = upperRomNumber;
        if (highRoms.containsKey(upperRomNumber)) {
            romBanks[HIGH_ROM] = highRoms.get(upperRomNumber);
        } else {
            romBanks[HIGH_ROM] = basicRom;
        }
    }

    public int getUpperRomNumber() {
        return upperRomNumber;
    }

    public int getRamSize() {
        return ramBanks.length * BANK_SIZE;
    }

    public List<byte[]> toByteArrayList() {
        List<byte[]> banks = new ArrayList<>();
        for (byte[] ramBank : ramBanks) {
            //Detach the banks from the current RAM representation
            byte[] ramCopy = new byte[BANK_SIZE];
            System.arraycopy(ramBank, 0, ramCopy, 0, BANK_SIZE);
            banks.add(ramCopy);
        }
        return banks;
    }
}
