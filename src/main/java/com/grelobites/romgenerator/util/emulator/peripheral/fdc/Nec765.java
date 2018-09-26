package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.command.Nec765CommandFactory;

import java.util.Optional;

public class Nec765 {
    private static final int NUM_DRIVES = 4;

    private Nec765CommandFactory commandFactory = new Nec765CommandFactory();
    private Nec765MainStatus mainStatusRegister = new Nec765MainStatus(0);
    private Nec765Status0 status0Register = new Nec765Status0(0);
    private Nec765Status1 status1Register = new Nec765Status1(0);
    private Nec765Status2 status2Register = new Nec765Status2(0);
    private Nec765Status3 status3Register = new Nec765Status3(0);

    private boolean motorOn;
    private DriveStatus[] driveStatuses = new DriveStatus[NUM_DRIVES];

    private DskContainer[] attachedDskContainers = new DskContainer[NUM_DRIVES];
    private Nec765Phase currentPhase;
    private Nec765Command currentCommand;

    public Nec765() {
        this.currentPhase = Nec765Phase.COMMAND;
        mainStatusRegister.setExecMode(false);
        mainStatusRegister.setDataInput(true);
        mainStatusRegister.setRQM(true);
        motorOn = false;
        for (int i = 0; i < driveStatuses.length; i++) {
            driveStatuses[i] = new DriveStatus();
        }
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
        switch (currentPhase) {
            case COMMAND:
                mainStatusRegister.setDataInput(true);
                mainStatusRegister.setExecMode(false);
                break;
            case EXECUTION:
                mainStatusRegister.setDataInput(false);
                mainStatusRegister.setExecMode(true);
                break;
            case RESULT:
                mainStatusRegister.setDataInput(false);
                mainStatusRegister.setExecMode(false);
                break;
        }
    }

    public Nec765Command getCurrentCommand() {
        return currentCommand;
    }

    public void setCurrentCommand(Nec765Command currentCommand) {
        this.currentCommand = currentCommand;
    }

    public DriveStatus getDriveStatus(int drive) {
        return driveStatuses[drive];
    }

    public Optional<DskContainer> getDskContainer(int drive) {
        return Optional.ofNullable(attachedDskContainers[drive]);
    }

    public void attachDskContainer(int drive, DskContainer container) {
        if (drive < NUM_DRIVES) {
            attachedDskContainers[drive] = container;
        }
    }

    public void writeControlRegister(int value) {
        motorOn = (value & 1) == 1;
    }

    public void writeDataRegister(int value) {
        if (currentPhase == Nec765Phase.COMMAND) {
            if (currentCommand == null) {
                currentCommand = commandFactory.getCommand(value);
                currentCommand.setFdcController(this);
                mainStatusRegister.setFdcBusy(true);
            }
            currentCommand.write(value);
        }
    }

    public int readDataRegister() {
        int value = 0;
        if (currentCommand != null) {
            switch (currentPhase) {
                case COMMAND:
                    //What happens in this case?
                    break;
                case EXECUTION:
                case RESULT:
                    value = currentCommand.read();
            }
            if (currentCommand.isDone()) {
                currentCommand = null;
                setCurrentPhase(Nec765Phase.COMMAND);
                mainStatusRegister.setFdcBusy(false);
            }
        }
        return value;
    }

    public int readStatusRegister() {
        return mainStatusRegister.value();
    }
}
