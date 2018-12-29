package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.sna.SnaCompressedInputStream;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.sna.SnaChunk;
import com.grelobites.romgenerator.util.sna.SnaFactory;
import com.grelobites.romgenerator.util.sna.SnaImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class SNAGameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SNAGameImageLoader.class);

    private static void setGameSlot(Map<Integer,byte[]> slots, byte[] data, int source, int slot) {
        LOGGER.debug("Copying chunk data from {} to {}. Source size {}",
                slot * Constants.SLOT_SIZE, (slot + 1) * Constants.SLOT_SIZE, data.length);
        slots.put(slot, Arrays.copyOfRange(data, source * Constants.SLOT_SIZE,
                (source + 1 ) * Constants.SLOT_SIZE));
    }

    private static boolean isEmptyChunk(SnaChunk chunk) {
        for (byte d : chunk.getData()) {
            if (d != Constants.B_FF) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAllEmptyChunks(Map<String, SnaChunk> chunks) {
        for (SnaChunk chunk: chunks.values()) {
            if (!isEmptyChunk(chunk)) {
                return false;
            }
        }
        LOGGER.debug("SNA has all chunks empty");
        return true;
    }

    @Override
    public Game load(InputStream is) throws IOException {
        try {
            SnaImage snaImage = SnaFactory.fromInputStream(is);
            GameHeader header = GameHeader.fromSnaImage(snaImage);
            int slots = 1024 * snaImage.getMemoryDumpSize() / Constants.SLOT_SIZE;
            SortedMap<Integer, byte[]> gameSlots = new TreeMap<>();
            for (int i = 0; i < slots; i++) {
                setGameSlot(gameSlots, snaImage.getMemoryDump(), i, i);
            }

            if (snaImage.getSnapshotVersion() == 3 && !hasAllEmptyChunks(snaImage.getSnaChunks())) {
                for (Map.Entry<String, SnaChunk> chunkEntry : snaImage.getSnaChunks()
                        .entrySet()) {
                    SnaChunk chunk = chunkEntry.getValue();
                    if (chunk.getName().equals(SnaChunk.CHUNK_MEM0)) {
                        LOGGER.debug("Read compressed chunk {} with size {}", chunk.getName(),
                                chunk.getData().length);
                        for (int i = 0; i < 4; i++) {
                            setGameSlot(gameSlots, chunk.getData(), i, i);
                        }
                    } else if (chunk.getName().equals(SnaChunk.CHUNK_MEM1)) {
                        LOGGER.debug("Read compressed chunk {} with size {}", chunk.getName(),
                                chunk.getData().length);
                        for (int i = 0; i < chunk.getData().length / Constants.SLOT_SIZE; i++) {
                            setGameSlot(gameSlots, chunk.getData(), i, i + 4);
                        }
                    }
                }
            }
            LOGGER.debug("Map of slots with entries {}", gameSlots.keySet());

            SnapshotGame game = new SnapshotGame(gameSlots.size() == 4 ?
                    GameType.RAM64 : GameType.RAM128, new ArrayList(gameSlots.values()));
            game.setGameHeader(header);
            game.setHardwareMode(HardwareMode.fromSnaType(snaImage.getCpcType()));
            return game;
        } catch (Exception e) {
            LOGGER.error("Creating SnapshotGame from SNA", e);
            throw e;
        }
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;

            SnaImage image = SnaFactory.fromSnapshotGame(snapshotGame);
            image.dump(os);
        } else {
            throw new IllegalArgumentException("Non Snapshot games cannot be saved as SNA");
        }
    }


}
