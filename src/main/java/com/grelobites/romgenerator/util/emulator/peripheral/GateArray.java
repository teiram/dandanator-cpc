package com.grelobites.romgenerator.util.emulator.peripheral;

import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.CpcColor;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class GateArray {
    private static final Logger LOGGER = LoggerFactory.getLogger(GateArray.class);
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

    private byte[] palette = new byte[17];
    private Integer ramBankingRegister;
    private int screenModeAndRomConfigurationRegister;
    private int selectedPen;

    private Set<GateArrayChangeListener> gateArrayChangeListeners = new HashSet<>();

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

        public Builder withCpc6128DefaultValues() {
            gateArray.ramBankingRegister = 1;
            gateArray.screenModeAndRomConfigurationRegister = 0;
            return this;
        }

        public Builder withHardwareDefaultValues(HardwareMode hardwareMode) {
            switch (hardwareMode) {
                case HW_CPC464:
                case HW_CPC464PLUS:
                    return withCpc464DefaultValues();
                case HW_CPC6128:
                case HW_CPC6128PLUS:
                    return withCpc6128DefaultValues();
                default:
                    return withCpc464DefaultValues();
            }
        }
    }

    private GateArray() {}

    public void addChangeListener(GateArrayChangeListener listener) {
        gateArrayChangeListeners.add(listener);
    }

    public void removeChangeListener(GateArrayChangeListener listener) {
        gateArrayChangeListeners.remove(listener);
    }

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

    public boolean isInterruptGenerationDelayed() {
        return (screenModeAndRomConfigurationRegister & 0x10) != 0;
    }

    public void setPalette(byte[] palette) {
        System.arraycopy(palette, 0, this.palette,
                0, Math.min(palette.length, this.palette.length));
    }

    public byte[] getPalette() {
        return palette;
    }

    public void setSelectedPen(int selectedPen) {
        this.selectedPen = selectedPen & 0xff;
    }

    public int getSelectedPen() {
        return selectedPen;
    }

    public int getScreenModeAndRomConfigurationRegister() {
        return screenModeAndRomConfigurationRegister;
    }

    public void setScreenModeAndRomConfigurationRegister(int value) {
        this.screenModeAndRomConfigurationRegister = value & 0xff;
    }

    public int getRamBankingRegister() {
        return ramBankingRegister != null ? ramBankingRegister : 0;
    }

    public void setRamBankingRegister(int value) {
        this.ramBankingRegister = value & 0xff;
    }

    private static int decodeSelectedPen(int value) {
        return (value & 0x10) != 0 ?
                BORDER_PALETTE_INDEX :
                value & 0x0F;
    }

    private boolean notifyListeners(GateArrayFunction function, int value) {
        for (GateArrayChangeListener listener: gateArrayChangeListeners) {
            if (!listener.onChange(function, value)) {
                return false;
            }
        }
        return true;
    }

    public void onPortWriteOperation(int value) {
        final GateArrayFunction function = GateArrayFunction.fromId((value >> 6) & 3);
        if (notifyListeners(function, value)) {
            switch (function) {
                case PEN_SELECTION_FN:
                    selectedPen = decodeSelectedPen(value);
                    break;
                case PALETTE_DATA_FN:
                    palette[selectedPen] = (byte) (value & 0x1f);
                    if ((selectedPen & 0x10)  == 0) {
                        LOGGER.debug("Setting pen {} to color {} ({})", selectedPen,
                                String.format("0x%02x", palette[selectedPen]),
                                CpcColor.hardIndexed(palette[selectedPen]));
                    }
                    break;
                case SCREEN_MODE_AND_ROM_CFG_FN:
                    screenModeAndRomConfigurationRegister = value;
                    break;
                case RAM_BANKING_FN:
                    ramBankingRegister = value != 0 ? value : null;
                    break;
            }
        }

    }

    @Override
    public String toString() {
        return "GateArray{" +
                "palette=" + Util.dumpAsHexString(palette) +
                ", ramBankingRegister=0x" + String.format("%02x", ramBankingRegister & 0xff) +
                ", screenModeAndRomConfigurationRegister=0x" + String.format("%02x", screenModeAndRomConfigurationRegister & 0xff) +
                ", selectedPen=" + selectedPen +
                '}';
    }
}
