package com.grelobites.romgenerator.util.winape.model;

import com.grelobites.romgenerator.util.winape.PokInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class WinApePokeDatabase {
    private static final String SIGNATURE = "WPOK";
    private static final Logger LOGGER = LoggerFactory.getLogger(WinApePokeDatabase.class);

    private TreeMap<String, WinApeGame> games = new TreeMap<>();

    public static WinApePokeDatabase fromInputStream(InputStream is) throws IOException {
        PokInputStream pis = new PokInputStream(is);
        if (pis.getHeader().equals(SIGNATURE)) {
            WinApePokeDatabase database = new WinApePokeDatabase();
            int numGames = pis.nextNumber();
            LOGGER.debug("Detected {} games in WinApe Poke Database", numGames);
            for (int i = 0; i < numGames; i++) {
                WinApeGame game = WinApeGame.fromPokInputStream(pis);
                database.games.put(game.getName(), game);
            }
            LOGGER.debug("All games extracted from database");
            return database;
        } else {
            throw new IllegalArgumentException("Not a WinApe POK database");
        }
    }

    public WinApeGame search(String key) {
        return games.ceilingEntry(key).getValue();
    }

    public WinApeGame firstGame() {
        return games.firstEntry().getValue();
    }

    public Collection<WinApeGame> games() {
        return games.values();
    }

}
