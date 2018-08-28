package com.grelobites.romgenerator.util.emulator;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TapGameImageLoader implements GameImageLoader {
    @Override
    public Game load(InputStream is) throws IOException {
        /*
        TapeLoader tapeLoader = TapeLoaderFactory.getTapeLoader(
                HardwareMode.valueOf(Configuration.getInstance().getTapLoaderTarget()));
        return tapeLoader.loadTape(is);
        */
        return null;
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        throw new IllegalStateException("Save to TAP not supported");
    }
}
