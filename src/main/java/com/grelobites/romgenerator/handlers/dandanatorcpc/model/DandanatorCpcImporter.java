package com.grelobites.romgenerator.handlers.dandanatorcpc.model;

import com.grelobites.romgenerator.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;

public interface DandanatorCpcImporter {
    void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext)
        throws IOException;

    void mergeRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext)
        throws IOException;
}
