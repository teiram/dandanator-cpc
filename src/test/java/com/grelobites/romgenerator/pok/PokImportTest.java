package com.grelobites.romgenerator.pok;

import com.grelobites.romgenerator.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PokImportTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokImportTest.class);
    private static String[] valueNames = new String[]{
            "Decimal",
            "Hex",
            "BCD",
            "Numeric (Zero Based)",
            "Numeric (ASCII)",
            "Long",
            "String"
    };

    private static class GameData {
        public String name;
        public byte[] idData;
        public int idAddr;
        public int numTrainers;

        @Override
        public String toString() {
            return "GameData{" +
                    "name='" + name + '\'' +
                    ", idData=" + Arrays.toString(idData) +
                    ", idAddr=" + idAddr +
                    ", numTrainers=" + numTrainers +
                    '}';
        }
    }

    private static class Trainer {
        public String description;
        public String comment;
        public int valueType;
        public boolean reversed;
        public int ramBank;
        public int numPokes;

        @Override
        public String toString() {
            return "Trainer{" +
                    "description='" + description + '\'' +
                    ", comment='" + comment + '\'' +
                    ", valueType=" + valueNames[valueType] +
                    ", reversed=" + reversed +
                    ", ramBank=" + String.format("0x%02x", ramBank) +
                    ", numPokes=" + numPokes +
                    '}';
        }
    }

    private static class Poke {
        public int size;
        public int address;
        public int[] values;

        @Override
        public String toString() {
            return "Poke{" +
                    "size=" + size +
                    ", address=" + address +
                    ", values=" + Arrays.toString(values) +
                    '}';
        }
    }

    private static String getPokHeader(InputStream is) throws IOException {
        byte[] h = new byte[4];
        is.read(h);
        return new String(h, StandardCharsets.US_ASCII);
    }

    private static GameData getNextGameData(InputStream is) throws IOException {
        GameData gameData = new GameData();
        gameData.name = getNextString(is);
        int idSize = Util.readAsLittleEndian(is);
        gameData.idAddr = Util.readAsLittleEndian(is);
        gameData.idData = new byte[idSize];
        is.read(gameData.idData);
        gameData.numTrainers = getNextNumber(is);
        return gameData;
    }

    private static Trainer getNextTrainer(InputStream is) throws IOException {
        Trainer trainer = new Trainer();
        trainer.description = getNextString(is);
        trainer.comment = getNextString(is);
        trainer.valueType = is.read();
        trainer.reversed = is.read() != 0;
        trainer.ramBank = is.read();
        trainer.numPokes = getNextNumber(is);
        return trainer;
    }

    private static Poke getNextPoke(InputStream is) throws IOException {
        Poke poke = new Poke();
        poke.size = Util.readAsLittleEndian(is);
        poke.address = Util.readAsLittleEndian(is);
        poke.values = new int[poke.size];
        for (int i = 0; i < poke.size; i++) {
            poke.values[i] = Util.readAsLittleEndian(is);
        }
        return poke;
    }
    private static String getNextString(InputStream is) throws IOException {
        int size = getNextNumber(is);
        byte s[] = new byte[size];
        is.read(s);
        return new String(s, StandardCharsets.US_ASCII);
    }

    private static int getNextNumber(InputStream is) throws IOException {
        long nextValue;
        long result = 0;
        int offset = 0;
        boolean firstBlock = true;
        boolean negative = false;
        while ((nextValue = is.read()) != -1) {
            boolean hasMore = (nextValue & 0x80) != 0;
            if (firstBlock) {
                result = nextValue & 0x3F;
                negative = (nextValue & 0x40) != 0;
                firstBlock = false;
                offset = 5;
            } else {
                result |= (nextValue & 0x7F) << offset;
                offset += 6;
            }
            if (!hasMore) break;
        }
        return Long.valueOf(negative ? -result : result).intValue();
    }

    @Test
    public void testPokPokesLoad() throws IOException {
        InputStream is = PokImportTest.class.getResourceAsStream("/winape.pok");
        assertNotNull(is);

        String header = getPokHeader(is);
        assertEquals("WPOK", header);
        long gameCount = getNextNumber(is);
        int[] valueTypes = new int[5];
        int[] pokeSizes = new int[20];
        int[] pokeCounts = new int[20];

        int trainerCount = 0;
        int pokeCount = 0;

        for (int i = 0; i < gameCount; i++) {
            GameData gameData = getNextGameData(is);
            LOGGER.debug("Next game is {}", gameData);
            for (int j = 0; j < gameData.numTrainers; j++) {
                Trainer trainer = getNextTrainer(is);
                pokeCounts[trainer.numPokes - 1]++;
                LOGGER.debug("Next trainer is {}", trainer);
                valueTypes[trainer.valueType]++;
                trainerCount++;
                assertEquals(trainer.ramBank, 0xc0);
                for (int k = 0; k < trainer.numPokes; k++) {
                    Poke poke = getNextPoke(is);
                    LOGGER.debug("Next poke is {}", poke);
                    pokeCount++;
                    pokeSizes[poke.size - 1]++;
                }
            }
        }

        LOGGER.debug("{} games, {} trainers, {} pokes",
                gameCount, trainerCount, pokeCount);
        LOGGER.debug("Value types distribution {}", Arrays.toString(valueTypes));
        LOGGER.debug("Pokes per trainer distribution {}", Arrays.toString(pokeCounts));
        LOGGER.debug("Poke size distribution {}", Arrays.toString(pokeSizes));
    }
}
