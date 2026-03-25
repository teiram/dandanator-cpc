package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.MLAGame;
import com.grelobites.romgenerator.model.MLAInfo;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

public class MlaGameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MlaGameImageLoader.class);

    @Override
    public Game load(InputStream is) throws IOException {
        byte[] gameData = Util.fromInputStream(is);
        LOGGER.debug("Using image of " + gameData.length + " bytes");
        if (gameData.length % Constants.SLOT_SIZE != 0) {
            int slots = (gameData.length + Constants.SLOT_SIZE - 1) / Constants.SLOT_SIZE;
            gameData = Util.paddedByteArray(gameData, 0, slots * Constants.SLOT_SIZE,(byte) 0xFF);
        }
        final byte[] gameImage = gameData;
        return MLAInfo.fromGameByteArray(Collections.singletonList(gameImage))
                .map(f -> new MLAGame(f, gameImage))
                .orElseThrow(() -> new IllegalArgumentException("Unable to extract MLA data from stream"));
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        MLAGame mlaGame = (MLAGame) game;
        //Save the game always reallocated to sector 0
        mlaGame.relocate(0);
        for (int i = 0; i < mlaGame.getSlotCount(); i++) {
            os.write(mlaGame.getSlot(i));
        }
    }

}
