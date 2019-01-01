package com.grelobites.romgenerator.handlers.dandanatorcpc.v2;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameBlock;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameMapper;
import com.grelobites.romgenerator.handlers.dandanatorcpc.v1.GameHeaderV1Serializer;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GameMapperV2 implements GameMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapperV2.class);

    private static final int COMPRESSED_SLOT_MAXSIZE = Constants.SLOT_SIZE;
    private static final int COMPRESSED_CHUNKSLOT_MAXSIZE = Constants.SLOT_SIZE - DandanatorCpcConstants.GAME_CHUNK_SIZE;
    private static final int INVALID_SLOT_ID = DandanatorCpcConstants.FILLER_BYTE;

    private SlotZeroV2 slotZero;
    private GameHeader gameHeader;
    private String name;
    private boolean isGameCompressed;
    private GameType gameType;
    private HardwareMode hardwareMode;
    private boolean screenHold;
    private List<GameBlock> blocks = new ArrayList<>();
    private TrainerList trainerList = new TrainerList(null);
    private int trainerCount;
    private Game game;

    private GameMapperV2(SlotZeroV2 slotZero) {
        this.slotZero = slotZero;
    }

    private static boolean isSlotCompressed(int slotIndex, int size) {
        return size > 0 && size < COMPRESSED_SLOT_MAXSIZE;
    }

    private static void addGameSlots(PositionAwareInputStream is, GameMapperV2 mapper)
            throws IOException {
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
    }

    private static void addMldGameSlots(PositionAwareInputStream is, GameMapperV2 mapper)
            throws IOException {
        int initSlot = is.read();
        int start = is.getAsLittleEndian();
        int numBlocks = is.getAsLittleEndian();
        is.skip(5 * 7); //Skip the remaining 7 blocks
        for (int i = 0; i < numBlocks; i++) {
            GameBlock block = new GameBlock();
            block.setInitSlot(initSlot++);
            block.setSize(Constants.SLOT_SIZE);
            block.setGameCompressed(false);
            block.setCompressed(false);
            if (block.getInitSlot() < INVALID_SLOT_ID) {
                LOGGER.debug("Read block for game " + mapper.name + ": " + block);
                mapper.getBlocks().add(block);
            }
        }
    }

    public static GameMapperV2 fromRomSet(PositionAwareInputStream is, SlotZeroV2 slotZero) throws IOException {
        LOGGER.debug("About to read game data. Offset is " + is.position());
        GameMapperV2 mapper = new GameMapperV2(slotZero);
        mapper.gameHeader = GameHeaderV1Serializer.deserialize(is);
        LOGGER.debug("Game header deserialized to {}", mapper.gameHeader);
        int gameType = is.read();
        mapper.gameType = GameType.byTypeId(gameType & 0x0f);
        if ((gameType & 0x80) != 0) {
            mapper.hardwareMode = HardwareMode.fromSnaType((gameType & 0x70) >>> 3);
        } else {
            mapper.hardwareMode = HardwareMode.HW_UNKNOWN;
        }
        LOGGER.debug("Setting memory dump size as {}", mapper.gameType.sizeInKBytes());
        mapper.gameHeader.setMemoryDumpSize(mapper.gameType.sizeInKBytes());
        is.skip(DandanatorCpcConstants.GAME_CHUNK_SIZE);
        mapper.isGameCompressed = is.read() != 0;
        mapper.screenHold = is.read() != 0;

        is.skip(2); //Active ROMS
        is.skip(V2Constants.GAME_LAUNCHCODE_SIZE);
        if (GameType.isMLD(mapper.gameType)) {
            addMldGameSlots(is, mapper);
        } else {
            addGameSlots(is, mapper);
        }

        mapper.name = Util.getNullTerminatedString(is, 4, DandanatorCpcConstants.GAMENAME_SIZE);

        LOGGER.debug("Read game {} data. Offset is {}", mapper.name, is.position());
        return mapper;
    }

    public TrainerList getTrainerList() {
        return trainerList;
    }

    public List<GameBlock> getBlocks() {
        return blocks;
    }

    public void setTrainerCount(int trainerCount) {
        this.trainerCount = trainerCount;
    }

    private List<byte[]> getGameSlots() {
        List<byte[]> gameSlots = new ArrayList<>();
        for (int index = 0; index < blocks.size(); index++) {
            GameBlock block = blocks.get(index);
            LOGGER.debug("Adding game slot for game " + name + ": " + block);
            gameSlots.add(block.getData());
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
                case RAM64:
                    SnapshotGame snapshotGame = new SnapshotGame(gameType, getGameSlots());
                    snapshotGame.setCompressed(isGameCompressed);
                    snapshotGame.setHoldScreen(screenHold);
                    snapshotGame.setGameHeader(gameHeader);
                    snapshotGame.setTrainerList(trainerList);
                    snapshotGame.setHardwareMode(hardwareMode);
                    if (isGameCompressed) {
                        snapshotGame.setCompressedData(getGameCompressedData());
                    }
                    game = snapshotGame;
                    break;
                case RAM128_MLD:
                case RAM64_MLD:
                    List<byte[]> gameSlots = getMLDGameSlots();
                    Optional<MLDInfo> mldInfo = MLDInfo.fromGameByteArray(gameSlots);
                    if (mldInfo.isPresent()) {
                        game = new MLDGame(mldInfo.get(), gameSlots);
                    } else {
                        LOGGER.error("Unable to restore MLDGame from ROMSet. No MLDInfo found");
                    }
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
