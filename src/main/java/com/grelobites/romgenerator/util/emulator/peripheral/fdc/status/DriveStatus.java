package com.grelobites.romgenerator.util.emulator.peripheral.fdc.status;

import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;

public class DriveStatus {
    private int headLoadTime;
    private int headUnloadTime;
    private int stepRateTime;
    private boolean dma;

    private SectorInformationBlock currentSector;

    public SectorInformationBlock getCurrentSector() {
        return currentSector;
    }

    public void setCurrentSector(SectorInformationBlock currentSector) {
        this.currentSector = currentSector;
    }

    public int getHeadLoadTime() {
        return headLoadTime;
    }

    public void setHeadLoadTime(int headLoadTime) {
        this.headLoadTime = headLoadTime;
    }

    public int getHeadUnloadTime() {
        return headUnloadTime;
    }

    public void setHeadUnloadTime(int headUnloadTime) {
        this.headUnloadTime = headUnloadTime;
    }

    public int getStepRateTime() {
        return stepRateTime;
    }

    public void setStepRateTime(int stepRateTime) {
        this.stepRateTime = stepRateTime;
    }

    public boolean isDma() {
        return dma;
    }

    public void setDma(boolean dma) {
        this.dma = dma;
    }
}
