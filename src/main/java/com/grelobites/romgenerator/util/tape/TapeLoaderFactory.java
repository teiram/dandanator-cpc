package com.grelobites.romgenerator.util.tape;

import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.emulator.resources.Cpc464LoaderResources;
import com.grelobites.romgenerator.util.emulator.resources.Cpc6128LoaderResources;
import com.grelobites.romgenerator.util.tape.loaders.TapeLoaderImpl;

public class TapeLoaderFactory {

    public static TapeLoader getTapeLoader(HardwareMode hwMode) {
        switch (hwMode) {
            case HW_CPC464:
                return new TapeLoaderImpl(hwMode, Cpc464LoaderResources.getInstance());
            case HW_CPC6128:
                return new TapeLoaderImpl(hwMode, Cpc6128LoaderResources.getInstance());
            default:
                throw new IllegalArgumentException("Unsupported Hardware by TapeLoader");
        }
    }
}
