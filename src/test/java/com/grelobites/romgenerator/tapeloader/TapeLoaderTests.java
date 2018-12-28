package com.grelobites.romgenerator.tapeloader;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.emulator.resources.Cpc464LoaderResources;
import com.grelobites.romgenerator.util.tape.loaders.TapeLoaderImpl;
import com.grelobites.romgenerator.util.gameloader.loaders.SNAGameImageLoader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;

public class TapeLoaderTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoaderTests.class);
    @Test
    public void tapeLoadTest() throws Exception {
        TapeLoaderImpl loader = new TapeLoaderImpl(HardwareMode.HW_CPC464,
                Cpc464LoaderResources.getInstance());
        InputStream cdt = TapeLoaderTests.class.getResourceAsStream("/cdt/freddy1.cdt");
        Game game = loader.loadTape(cdt);
        new SNAGameImageLoader().save(game,
                new FileOutputStream(new File("/home/mteira/Escritorio/test.sna")));
    }

    @Test
    public void testHeader() {
        LOGGER.debug("Value for header is {}", Util.dumpAsHexString("ZxTape!".getBytes()));
    }
}
