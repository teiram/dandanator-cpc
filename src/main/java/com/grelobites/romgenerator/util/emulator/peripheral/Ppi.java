package com.grelobites.romgenerator.util.emulator.peripheral;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Ppi {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ppi.class);
    private static final int PSG_REGISTERS = 16;
    public static final int KEYSCAN_PSG_REGISTER = 14;
    private static final int KEYBOARD_SCANLINES = 0x10;

    private PsgFunction psgFunction = PsgFunction.NONE;
    private int selectedPsgRegister;
    private byte[] psgRegisterData = new byte[PSG_REGISTERS];
    private boolean motorOn = false;
    private int keyboardLineToScan;
    private boolean casseteDataOutput;
    private boolean expansionPortAsserted = false;
    private boolean printerBusy = false;
    private boolean casseteDataInput;
    private boolean portAInputDirection = false;
    private boolean refreshRate50Hz = true;
    private boolean vSyncActive = false;
    private int vendor = 0x7; //Amstrad
    private int portACurrentValue = 0;
    private int portBCurrentValue;
    private int portCCurrentValue;
    private int controlCurrentValue;
    private byte[] keyStatus = new byte[KEYBOARD_SCANLINES];
    private List<MotorStateChangeListener> motorStateChangeListeners = new ArrayList<>();
    private List<PsgFunctionListener> psgFunctionListeners = new ArrayList<>();

    public Ppi() {
        for (int i = 0; i < keyStatus.length; i++) {
            keyStatus[i] = (byte) 0xff;
        }
    }

    public void pressKey(KeyboardCode code) {
        keyStatus[code.line()] &= ~code.mask();
    }

    public void releaseKey(KeyboardCode code) {
        keyStatus[code.line()] |= code.mask();
    }

    public int getSelectedPsgRegister() {
        return selectedPsgRegister;
    }

    public void setSelectedPsgRegister(int selectedPsgRegister) {
        this.selectedPsgRegister = selectedPsgRegister;
    }

    public byte[] getPsgRegisterData() {
        return psgRegisterData;
    }

    public void setPsgRegisterData(byte[] psgRegisterData) {
        System.arraycopy(psgRegisterData, 0,
                this.psgRegisterData, 0, PSG_REGISTERS);
    }

    public boolean isMotorOn() {
        return motorOn;
    }

    public void setMotorOn(boolean motorOn) {
        this.motorOn = motorOn;
    }

    public int getKeyboardLineToScan() {
        return keyboardLineToScan;
    }

    public void setKeyboardLineToScan(int keyboardLineToScan) {
        this.keyboardLineToScan = keyboardLineToScan;
    }

    public boolean isCasseteDataOutput() {
        return casseteDataOutput;
    }

    public void setCasseteDataOutput(boolean casseteDataOutput) {
        this.casseteDataOutput = casseteDataOutput;
    }

    public boolean isExpansionPortAsserted() {
        return expansionPortAsserted;
    }

    public void setExpansionPortAsserted(boolean expansionPortAsserted) {
        this.expansionPortAsserted = expansionPortAsserted;
    }

    public boolean isPrinterBusy() {
        return printerBusy;
    }

    public void setPrinterBusy(boolean printerBusy) {
        this.printerBusy = printerBusy;
    }

    public boolean isCasseteDataInput() {
        return casseteDataInput;
    }

    public void invertCasseteDataInput() {
        casseteDataInput = !casseteDataInput;
    }

    public void setCasseteDataInput(boolean casseteDataInput) {
        this.casseteDataInput = casseteDataInput;
    }

    public boolean isPortAInputDirection() {
        return portAInputDirection;
    }

    public void setPortAInputDirection(boolean portAInputDirection) {
        this.portAInputDirection = portAInputDirection;
    }

    public boolean isRefreshRate50Hz() {
        return refreshRate50Hz;
    }

    public void setRefreshRate50Hz(boolean refreshRate50Hz) {
        this.refreshRate50Hz = refreshRate50Hz;
    }

    public boolean isvSyncActive() {
        return vSyncActive;
    }

    public void setvSyncActive(boolean vSyncActive) {
        this.vSyncActive = vSyncActive;
    }

    public int getVendor() {
        return vendor;
    }

    public void setVendor(int vendor) {
        this.vendor = vendor;
    }

    public int getPortACurrentValue() {
        return portACurrentValue;
    }

    public void setPortACurrentValue(int value) {
        this.portACurrentValue = value;
    }

    public int getPortBCurrentValue() {
        return portBCurrentValue;
    }

    public void setPortBCurrentValue(int value) {
        this.portBCurrentValue = value;
    }

    public int getPortCCurrentValue() {
        return portCCurrentValue;
    }

    public void setPortCCurrentValue(int value) {
        this.portCCurrentValue = value;
    }

    public int getControlCurrentValue() {
        return controlCurrentValue;
    }

    public void setControlCurrentValue(int value) {
        this.controlCurrentValue = value;
    }

    public void portAOutput(int value) {
        if (!portAInputDirection) {
            portACurrentValue = value;
        }
    }

    private void updatePsgRegisters() {
        if (selectedPsgRegister == KEYSCAN_PSG_REGISTER) {
            psgRegisterData[KEYSCAN_PSG_REGISTER] = keyStatus[keyboardLineToScan];
        }
    }

    public int portAInput() {
        if (portAInputDirection) {
            updatePsgRegisters();
            return psgRegisterData[selectedPsgRegister] & 0xff;
        } else {
            return portACurrentValue; //Right?
        }
    }

    public int portBInput() {
        portBCurrentValue = ((casseteDataInput ? 1 : 0) << 7) |
                ((printerBusy ? 1 : 0) << 6) |
                ((expansionPortAsserted ? 0 : 1) << 5) |
                ((refreshRate50Hz ? 1 : 0) << 4) |
                (vendor << 1) |
                (vSyncActive ? 1 : 0);
        return portBCurrentValue;
    }

    private void applyPsgFunction() {
        notifyPsgFunctionListeners(psgFunction);
        switch (psgFunction) {
            case SELECT:
                selectedPsgRegister = portACurrentValue & 0x0f;
                break;
            case READ:
                portACurrentValue = psgRegisterData[selectedPsgRegister];
                break;
            case WRITE:
                psgRegisterData[selectedPsgRegister] = (byte) portACurrentValue;
        }
    }

    public void portCOutput(int value) {
        portCCurrentValue = value & 0xff;
        psgFunction = PsgFunction.fromId((value & 0xC0) >>> 6);
        keyboardLineToScan = value & 0x0F;
        casseteDataOutput = (value & 0x20) != 0;
        boolean newMotorOnValue = (value & 0x10) != 0;
        if (newMotorOnValue != motorOn) {
            motorOn = newMotorOnValue;
            notifyMotorStateChangeListeners();
        }
        applyPsgFunction();
    }

    public int portCInput() {
        return portCCurrentValue;
    }

    public void controlOutput(int value) {
        controlCurrentValue = value;
        if ((value & 0x80) != 0) {
            portACurrentValue = portBCurrentValue = portCCurrentValue = 0;
            portAInputDirection = (value & 0x10) != 0;
        } else {
            int bitToSet = (value >>> 1) & 0x7;
            boolean bitValue = (value & 0x1) != 0;
            if (bitToSet > 5) {
                //Recalculate PSG function
                psgFunction = bitValue ?
                        PsgFunction.fromId(psgFunction.id() | (1 << (bitToSet - 5))) :
                        PsgFunction.fromId(psgFunction.id() & ~(1 << (bitToSet - 5)));
                LOGGER.warn("Recalculated PSG Function from Control port");
                applyPsgFunction();
            } else if (bitToSet == 5) {
                casseteDataOutput = bitValue;
            } else if (bitToSet == 4) {
                if (bitValue != motorOn) {
                    motorOn = bitValue;
                    notifyMotorStateChangeListeners();
                }
            } else {
                keyboardLineToScan = bitValue ?
                        keyboardLineToScan | (1 << bitToSet) :
                        keyboardLineToScan & ~(1 << bitToSet);
                keyboardLineToScan &= 0x0F;
            }
        }
    }

    public void addMotorStateChangeListener(MotorStateChangeListener listener) {
        this.motorStateChangeListeners.add(listener);
    }

    public void removeMotorStateChangeListener(MotorStateChangeListener listener) {
        if (!this.motorStateChangeListeners.remove(listener)) {
            LOGGER.warn("Trying to remove unregistered MotorStateChangeListener");
        }
    }

    public void addPsgFunctionListener(PsgFunctionListener listener) {
        this.psgFunctionListeners.add(listener);
    }

    public void removePsgFunctionListener(PsgFunctionListener listener) {
        if (!this.psgFunctionListeners.remove(listener)) {
            LOGGER.warn("Trying to remove unregistered PsgFunctionListener");
        }
    }

    private void notifyPsgFunctionListeners(PsgFunction function) {
        for (PsgFunctionListener listener: psgFunctionListeners) {
            listener.onPsgFunctionExecution(function);
        }
    }

    private void notifyMotorStateChangeListeners() {
        for (MotorStateChangeListener listener: motorStateChangeListeners) {
            listener.onMotorStateChange(motorOn);
        }
    }


}
