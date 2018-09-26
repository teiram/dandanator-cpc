package com.grelobites.romgenerator.util.emulator.peripheral.fdc;

import com.grelobites.romgenerator.util.dsk.SectorInformationBlock;

public class DriveStatus {
    private SectorInformationBlock currentSector;

    public SectorInformationBlock getLastSector() {
        return currentSector;
    }

    public void setCurrentSector(SectorInformationBlock currentSector) {
        this.currentSector = currentSector;
    }
}
