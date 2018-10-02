package com.grelobites.romgenerator.util.emulator.peripheral.fdc.status;

import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;

public class DriveStatus {

    private SectorInformationBlock currentSector;

    public SectorInformationBlock getCurrentSector() {
        return currentSector;
    }

    public void setCurrentSector(SectorInformationBlock currentSector) {
        this.currentSector = currentSector;
    }

}
