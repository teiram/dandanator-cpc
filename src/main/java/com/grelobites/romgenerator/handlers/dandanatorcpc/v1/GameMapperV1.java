package com.grelobites.romgenerator.handlers.dandanatorcpc.v1;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameBlock;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameChunk;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameMapper;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RomGame;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.model.TrainerList;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameMapperV1 implements GameMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapperV1.class);

    private static final int COMPRESSED_SLOT_MAXSIZE = Constants.SLOT_SIZE;
    private static final int COMPRESSED_CHUNKSLOT_MAXSIZE = Constants.SLOT_SIZE - DandanatorCpcConstants.GAME_CHUNK_SIZE;
    private static final int INVALID_SLOT_ID = DandanatorCpcConstants.FILLER_BYTE;

    private SlotZeroV1 slotZero;
    private GameHeader gameHeader;
    private String name;
    private boolean isGameCompressed;
    private GameType gameType;
    private boolean screenHold;
    private byte[] launchCode;
    private GameChunk gameChunk;
    private List<GameBlock> blocks = new ArrayList<>();
    private TrainerList trainerList = new TrainerList(null);
    private int trainerCount;
    private Game game;

    private GameMapperV1(SlotZeroV1 slotZero) {
        this.slotZero = slotZero;
    }

    private static boolean isSlotCompressed(int slotIndex, int size) {
        return size > 0 && ((slotIndex != DandanatorCpcConstants.GAME_CHUNK_SLOT) ?
                size < COMPRESSED_SLOT_MAXSIZE :
                size < COMPRESSED_CHUNKSLOT_MAXSIZE);
    }

    public static GameMapperV1 fromRomSet(PositionAwareInputStream is, SlotZeroV1 slotZero) throws IOException {
        LOGGER.debug("About to read game data. Offset is " + is.position());
        GameMapperV1 mapper = new GameMapperV1(slotZero);
        mapper.gameHeader = GameHeaderV1Serializer.deserialize(is);
        mapper.name = Util.getNullTerminatedString(is, 3, DandanatorCpcConstants.GAMENAME_SIZE);

        mapper.isGameCompressed = is.read() != 0;
        mapper.gameType = GameType.byTypeId(is.read());

        mapper.screenHold = is.read() != 0;

        mapper.launchCode = Util.fromInputStream(is, V1Constants.GAME_LAUNCHCODE_SIZE);
        mapper.gameChunk = new GameChunk();
        mapper.gameChunk.setAddress(is.getAsLittleEndian());
        mapper.gameChunk.setLength(is.getAsLittleEndian());
        for (int i = 0; i < 8; i++) {
            GameBlock block = new GameBlock();
            block.setInitSlot(is.read());
            block.setStart(is.getAsLittleEndian());
            block.setSize(is.getAsLittleEndian());
            block.setGameCompressed(mapper.isGameCompressed);
            block.setCompressed(mapper.isGameCompressed && isSlotCompressed(i, block.getSize()));
            if (block.getInitSlot() < INVALID_SLOT_ID) {
                LOGGER.debug("Read block for game " + mapper.name + ": " + block);
                mapper.getBlocks().add(block);
            }
        }
        LOGGER.debug("Read game data. Offset is " + is.position());
        return mapper;
    }

    public TrainerList getTrainerList() {
        return trainerList;
    }

    public List<GameBlock> getBlocks() {
        return blocks;
    }

    public GameChunk getGameChunk() {
        return gameChunk;
    }

    public byte[] getLaunchCode() {
        return launchCode;
    }

    public void setTrainerCount(int trainerCount) {
        this.trainerCount = trainerCount;
    }

    private List<byte[]> getGameSlots() {
        List<byte[]> gameSlots = new ArrayList<>();
        for (int index = 0; index < blocks.size(); index++) {
            GameBlock block = blocks.get(index);
            LOGGER.debug("Adding game slot for game " + name + ": " + block);
            if (index == DandanatorCpcConstants.GAME_CHUNK_SLOT) {
                gameSlots.add(Util.concatArrays(block.getData(), gameChunk.getData()));
            } else {
                gameSlots.add(block.getData());
            }
        }
        return gameSlots;
    }

    private List<byte[]> getMLDGameSlots() {
        List<byte[]> gameSlots = new ArrayList<>();
        for (int index = 0; index < blocks.size(); index++) {
            GameBlock block = blocks.get(index);
            int slots = block.size / Constants.SLOT_SIZE;
            for (int slot = 0; slot < slots; slot++) {
                LOGGER.debug("Adding game slot for game " + name + ": " + block);
                gameSlots.add(Arrays.copyOfRange(block.data, slot * Constants.SLOT_SIZE, (slot + 1) * Constants.SLOT_SIZE));
            }
        }
        return gameSlots;
    }

    private List<byte[]> getGameCompressedData() {
        List<byte[]> compressedData = new ArrayList<>();
        for (int index = 0; index < blocks.size(); index++) {
            GameBlock block = blocks.get(index);
            compressedData.add(block.rawdata);
        }
        return compressedData;
    }

    public int getTrainerCount() {
        return trainerCount;
    }

    @Override
    public GameType getGameType() {
        return gameType;
    }

    @Override
    public Game getGame() {
        if (game == null) {
            switch (gameType) {
                case ROM:
                    game = new RomGame(GameType.ROM, getGameSlots().get(0));
                    break;
                case RAM128:
                    SnapshotGame snapshotGame = new SnapshotGame(gameType, getGameSlots());
                    snapshotGame.setCompressed(isGameCompressed);
                    snapshotGame.setHoldScreen(screenHold);
                    snapshotGame.setGameHeader(gameHeader);
                    snapshotGame.setTrainerList(trainerList);
                    if (isGameCompressed) {
                        snapshotGame.setCompressedData(getGameCompressedData());
                    }
                    game = snapshotGame;
                    break;
                default:
                    LOGGER.error("Unsupported type of game " + gameType.screenName());
                    throw new IllegalArgumentException("Unsupported game type");
            }
            game.setName(name);
        }
        LOGGER.debug("Game generated as " + game);
        return game;
    }

    @Override
    public void populateGameSlots(PositionAwareInputStream is) throws IOException {
        //Actually made in SlotZeroV6 since it keeps track of the GameBlocks and
        //it needs to take them in order
        throw new IllegalStateException("Unsupported slot population method in V5");

    }
}
