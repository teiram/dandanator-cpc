package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.Nec765CommandFactory;

public class Nec765 {

    private Nec765CommandFactory commandFactory;
    private Nec765MainStatus mainStatusRegister;
    private Nec765Status0 status0Register;
    private Nec765Status1 status1Register;
    private Nec765Status2 status2Register;
    private Nec765Status3 status3Register;

    private boolean motorOn;

    private Nec765Phase currentPhase;
    private Nec765Command currentCommand;

    public Nec765() {
        this.currentPhase = Nec765Phase.COMMAND;
        mainStatusRegister.setExecMode(false);
        mainStatusRegister.setDataInput(true);
        mainStatusRegister.setRQM(true);
    }

    public Nec765MainStatus getMainStatusRegister() {
        return mainStatusRegister;
    }

    public void setMainStatusRegister(Nec765MainStatus mainStatusRegister) {
        this.mainStatusRegister = mainStatusRegister;
    }

    public Nec765Status0 getStatus0Register() {
        return status0Register;
    }

    public void setStatus0Register(Nec765Status0 status0Register) {
        this.status0Register = status0Register;
    }

    public Nec765Status1 getStatus1Register() {
        return status1Register;
    }

    public void setStatus1Register(Nec765Status1 status1Register) {
        this.status1Register = status1Register;
    }

    public Nec765Status2 getStatus2Register() {
        return status2Register;
    }

    public void setStatus2Register(Nec765Status2 status2Register) {
        this.status2Register = status2Register;
    }

    public Nec765Status3 getStatus3Register() {
        return status3Register;
    }

    public void setStatus3Register(Nec765Status3 status3Register) {
        this.status3Register = status3Register;
    }

    public Nec765Phase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(Nec765Phase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public Nec765Command getCurrentCommand() {
        return currentCommand;
    }

    public void setCurrentCommand(Nec765Command currentCommand) {
        this.currentCommand = currentCommand;
    }

    public void writeControlRegister(int value) {
        motorOn = (value & 1) == 1;
    }

    public void writeDataRegister(int value) {
        if (currentCommand != null) {
            currentCommand.addCommandData(value);
            if (currentCommand.isPrepared()) {
                currentPhase = currentCommand.hasExecutionPhase() ? Nec765Phase.EXECUTION :
                        Nec765Phase.RESULT;
            }
        } else {
            try {
                currentCommand = commandFactory.getCommand(value);
            } catch (Exception e) {


            }
        }
    }

    public int readDataRegister() {
        if (currentCommand != null) {
            switch (currentPhase) {
                case COMMAND:
                    //Error here (see how to provide the result in MSR)
                    break;
                case EXECUTION:
                    return currentCommand.execute();
                case RESULT:
                    return currentCommand.result();
            }
            if (currentCommand.isDone()) {
                currentCommand = null;
            }
        }
        return 0;
    }

    public int readStatusRegister() {
        return mainStatusRegister.value();
    }
}
