package com.grelobites.romgenerator.model;

import java.util.Collections;

public class RomGame extends BaseGame implements Game {

    public RomGame(GameType gameType, byte[] data) {
        super(gameType, Collections.singletonList(data));
    }

    @Override
    public boolean isCompressible() {
        return false;
    }

    @Override
    public String toString() {
        return "RomGame{name='" + getName() + "'"
            + ", type=" + getType()
                + "}";
    }
}
