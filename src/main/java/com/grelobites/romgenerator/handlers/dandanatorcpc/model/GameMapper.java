package com.grelobites.romgenerator.handlers.dandanatorcpc.model;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.util.PositionAwareInputStream;

import java.io.IOException;

public interface GameMapper {

    Game getGame();
    GameType getGameType();
    void populateGameSlots(PositionAwareInputStream is) throws IOException;
}
