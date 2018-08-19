package com.grelobites.romgenerator.util.emulator;

import com.grelobites.romgenerator.model.Game;

import java.io.InputStream;

public interface TapeLoader {
    Game loadTape(InputStream tapeFile);
}
