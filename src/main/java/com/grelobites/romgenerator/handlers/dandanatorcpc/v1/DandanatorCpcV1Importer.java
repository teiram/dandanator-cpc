package com.grelobites.romgenerator.handlers.dandanatorcpc.v1;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConfiguration;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.DandanatorCpcImporter;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.SlotZero;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.Future;

public class DandanatorCpcV1Importer implements DandanatorCpcImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorCpcV1Importer.class);

    @Override
    public void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext) throws IOException {
        try {
            slotZero.parse();

            PositionAwareInputStream is = new PositionAwareInputStream(payload);

            slotZero.populateGameSlots(is);

            //If we reached this far, we have all the data and it's safe to replace the game list
            LOGGER.debug("Clearing game list with recovered games count " + slotZero.getGameMappers().size());
            Collection<Game> games = applicationContext.getGameList();
            games.clear();

            applicationContext.addBackgroundTask(() -> {
                slotZero.getGameMappers().forEach(gameMapper -> {
                    Future<OperationResult> result = applicationContext.getRomSetHandler()
                            .addGame(gameMapper.getGame());
                    try {
                        result.get();
                    } catch (Exception e) {
                        LOGGER.warn("While waiting for background operation result", e);
                    }
                });
                return OperationResult.successResult();
            });

            is.safeSkip(Constants.SLOT_SIZE * DandanatorCpcConstants.GAME_SLOTS - is.position());
            LOGGER.debug("Getting extraRom with offset {}", is.position());
            byte[] extraRom = is.getAsByteArray(Constants.SLOT_SIZE);

            //Update preferences only if everything was OK
            Configuration globalConfiguration = Configuration.getInstance();
            DandanatorCpcConfiguration dandanatorCpcConfiguration = DandanatorCpcConfiguration.getInstance();

            globalConfiguration.setCharSet(slotZero.getCharSet());
            globalConfiguration.setCharSetPath(Constants.ROMSET_PROVIDED);

            globalConfiguration.setBackgroundImage(ImageUtil.fillCpcImage(
                    slotZero.getScreen(),
                    slotZero.getScreenPalette()));
            globalConfiguration.setBackgroundImagePath(Constants.ROMSET_PROVIDED);

            dandanatorCpcConfiguration.setExtraRomPath(Constants.ROMSET_PROVIDED);
            dandanatorCpcConfiguration.setExtraRom(extraRom);

            dandanatorCpcConfiguration.setExtraRomMessage(slotZero.getExtraRomMessage());
            dandanatorCpcConfiguration.setTogglePokesMessage(slotZero.getTogglePokesMessage());
            dandanatorCpcConfiguration.setLaunchGameMessage(slotZero.getLaunchGameMessage());
            dandanatorCpcConfiguration.setSelectPokesMessage(slotZero.getSelectPokesMessage());
        } catch (Exception e) {
            LOGGER.error("Importing RomSet", e);
        }
    }

    @Override
    public void mergeRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext) throws IOException {
        try {
            slotZero.parse();
            slotZero.populateGameSlots(new PositionAwareInputStream(payload));

            applicationContext.addBackgroundTask(() -> {
                slotZero.getGameMappers().forEach(gameMapper -> {
                    Future<OperationResult> result = applicationContext.getRomSetHandler()
                            .addGame(gameMapper.getGame());
                    try {
                        result.get();
                    } catch (Exception e) {
                        LOGGER.warn("While waiting for background operation result", e);
                    }
                });
                return OperationResult.successResult();
            });

         } catch (Exception e) {
            LOGGER.error("Merging RomSet", e);
        }
    }
}
