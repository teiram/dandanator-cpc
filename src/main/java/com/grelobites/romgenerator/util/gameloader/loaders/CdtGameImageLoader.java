package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.tape.TapeLoader;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.tape.TapeLoaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CdtGameImageLoader implements GameImageLoader {
    @Override
    public Game load(InputStream is) throws IOException {
        TapeLoader tapeLoader = TapeLoaderFactory.getTapeLoader(
                HardwareMode.valueOf(Configuration.getInstance().getTapeLoaderTarget()));
        return tapeLoader.loadTape(is);
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        throw new IllegalStateException("Save to CDT not supported");
    }
}
