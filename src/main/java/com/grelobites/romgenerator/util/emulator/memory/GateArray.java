package com.grelobites.romgenerator.util.emulator.memory;

public class GateArray {
    private static final int BANK_SIZE = 0x4000;
    private static final int BORDER_PALETTE_INDEX = 16;
    private static final int[][] MEMORY_CONFIGURATIONS = new int[][] {
            {0, 1, 2, 3},
            {0, 1, 2, 7},
            {4, 5, 6, 7},
            {0, 3, 2, 7},
            {0, 4, 2, 3},
            {0, 5, 2, 3},
            {0, 6, 2, 3},
            {0, 7, 2, 3}};
    private static final int PEN_SELECTION_FN = 0;
    private static final int PALETTE_DATA_FN = 1;
    private static final int SCREEN_MODE_AND_ROM_CFG_FN = 2;
    private static final int RAM_BANKING_FN = 3;


    private int[] palette = new int[17];

    private Integer ramBankingRegister;
    private int screenModeAndRomConfigurationRegister;

    private int selectedPen;

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        GateArray gateArray = new GateArray();

        public Builder withRamBankingRegister(int ramBankingRegister) {
            gateArray.ramBankingRegister = ramBankingRegister;
            return this;
        }

        public Builder withScreenModeAndRomConfigurationRegister(int screenModeAndRomConfigurationRegister) {
            gateArray.screenModeAndRomConfigurationRegister = screenModeAndRomConfigurationRegister;
            return this;
        }

        public GateArray build() {
            return gateArray;
        }

        public Builder withCpc464DefaultValues() {
            gateArray.ramBankingRegister = null;
            gateArray.screenModeAndRomConfigurationRegister = 0;
            return this;
        }
    }

    private GateArray() {}

    public int getMemoryBankSlot(int address) {
        return MEMORY_CONFIGURATIONS[ramBankingRegister != null ?
                ramBankingRegister & 0x07 : 0]
                [address / BANK_SIZE];

    }

    public boolean isLowRomEnabled() {
        return (screenModeAndRomConfigurationRegister & 0x04) == 0;
    }

    public boolean isHighRomEnabled() {
        return (screenModeAndRomConfigurationRegister & 0x08) == 0;
    }

    public boolean hasRamBanking() {
        return ramBankingRegister != null;
    }

    public int[] getPalette() {
        return palette;
    }

    private static int decodeSelectedPen(int value) {
        return (value & 0x10) != 0 ?
                BORDER_PALETTE_INDEX :
                value & 0x07;
    }

    public void onPortWriteOperation(int value) {
        final int function = (value >> 6) & 3;
        switch (function) {
            case PEN_SELECTION_FN:
                selectedPen = decodeSelectedPen(value);
                break;
            case PALETTE_DATA_FN:
                palette[selectedPen] = value & 0x1f;
                break;
            case SCREEN_MODE_AND_ROM_CFG_FN:
                screenModeAndRomConfigurationRegister = value;
                break;
            case RAM_BANKING_FN:
                ramBankingRegister = value;
                break;
        }
    }
}
