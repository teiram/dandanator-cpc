package com.grelobites.romgenerator.com.grelobites.romgenerator.util.wav;

import com.grelobites.romgenerator.com.grelobites.romgenerator.util.emulator.tapeloader.TapeLoaderTests;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.emulator.resources.Cpc464LoaderResources;
import com.grelobites.romgenerator.util.gameloader.loaders.SNAGameImageLoader;
import com.grelobites.romgenerator.util.tape.loaders.TapeLoaderImpl;
import com.grelobites.romgenerator.util.wav.CdtWavOutputStream;
import com.grelobites.romgenerator.util.wav.WavFormat;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CdtWavOutputStreamTests {

    @Test
    public void cdtToWavTest() throws Exception {
        InputStream cdt = CdtWavOutputStreamTests.class.getResourceAsStream("/cdt/lala.cdt");
        OutputStream output = new FileOutputStream(new File("C:\\Users\\mteira\\Desktop\\lala.wav"));
        CdtWavOutputStream converter = new CdtWavOutputStream(WavFormat.DEFAULT_FORMAT, cdt, output);
        converter.flush();
    }

}
