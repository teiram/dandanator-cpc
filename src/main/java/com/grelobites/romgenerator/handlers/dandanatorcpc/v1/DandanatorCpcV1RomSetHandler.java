package com.grelobites.romgenerator.handlers.dandanatorcpc.v1;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConfiguration;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcRamGameCompressor;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcRomSetHandlerSupport;
import com.grelobites.romgenerator.handlers.dandanatorcpc.ExtendedCharSet;
import com.grelobites.romgenerator.handlers.dandanatorcpc.RomSetUtil;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameChunk;
import com.grelobites.romgenerator.handlers.dandanatorcpc.view.DandanatorCpcFrameController;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeaderOffsets;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.CpcColor;
import com.grelobites.romgenerator.util.CpcScreen;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.RamGameCompressor;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.Z80Opcode;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerType;
import com.grelobites.romgenerator.view.util.DirectoryAwareFileChooser;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public class DandanatorCpcV1RomSetHandler extends DandanatorCpcRomSetHandlerSupport implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorCpcV1RomSetHandler.class);

    private static final byte[] EMPTY_CBLOCK = new byte[5];
    private static final int MAX_MENU_PAGES = 3;

    private static RamGameCompressor ramGameCompressor = new DandanatorCpcRamGameCompressor();
    private DoubleProperty currentRomUsage;

    protected DandanatorCpcFrameController dandanatorCpcFrameController;
    protected Pane dandanatorCpcFrame;
    protected MenuItem exportPokesMenuItem;
    protected MenuItem importPokesMenuItem;
    protected MenuItem exportToWavsMenuItem;
    protected MenuItem exportExtraRomMenuItem;
    private BooleanProperty generationAllowedProperty = new SimpleBooleanProperty(false);


    private CpcScreen[] menuImages;
    private AnimationTimer previewUpdateTimer;
    private static final long SCREEN_UPDATE_PERIOD_NANOS = 3 * 1000000000L;

    private InvalidationListener updateImageListener =
            (c) -> updateMenuPreview();

    private InvalidationListener updateRomUsageListener =
            (c) -> updateRomUsage();

    private static void initializeMenuImages(CpcScreen[] menuImages) throws IOException {
        for (int i = 0; i < menuImages.length; i++) {
            menuImages[i] = new CpcScreen(1);
            updateBackgroundImage(menuImages[i]);
        }
    }

    private void updateRomUsage() {
        getApplicationContext().setRomUsage(calculateRomUsage());
        getApplicationContext().setRomUsageDetail(generateRomUsageDetail());
    }

    public DandanatorCpcV1RomSetHandler() throws IOException {
        menuImages = new CpcScreen[MAX_MENU_PAGES];
        initializeMenuImages(menuImages);
        currentRomUsage = new SimpleDoubleProperty();
        previewUpdateTimer = new AnimationTimer() {
            int currentFrame = 0;
            long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate > SCREEN_UPDATE_PERIOD_NANOS) {
                    if (applicationContext != null) {
                        int nextFrame;
                        int gameCount = applicationContext.getGameList().size();
                        if (gameCount > ((currentFrame + 1) * DandanatorCpcConstants.SLOT_COUNT)) {
                            nextFrame = currentFrame + 1;
                        } else {
                            nextFrame = 0;
                        }
                        if (nextFrame >= menuImages.length) {
                            LOGGER.warn("Out of bounds calculated next frame " + nextFrame);
                            nextFrame = 0;
                        }
                        applicationContext.getMenuPreview().setImage(menuImages[nextFrame]);
                        currentFrame = nextFrame;
                        lastUpdate = now;
                    }
                }
            }
        };
    }

    private static byte[] getEepromLoaderCode() throws IOException {
        PlayerConfiguration configuration = PlayerConfiguration.getInstance();
        byte[] eewriter = Util.fromInputStream(configuration.getRomsetLoaderStream());
        return Util.compress(eewriter);
    }

    private static byte[] getEepromLoaderScreen() throws IOException {
        PlayerConfiguration configuration = PlayerConfiguration.getInstance();
        byte[] screen = Util.fromInputStream(configuration.getScreenStream());
        return Util.compress(screen);
    }

    private static byte[] getPaddedGameHeader(Game game) throws IOException {
        byte[] paddedHeader = new byte[V1Constants.GAME_HEADER_SIZE];
        Arrays.fill(paddedHeader, Constants.B_00);
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            GameHeaderV1Serializer.serialize(snapshotGame, os);
            byte[] snaHeader = os.toByteArray();
            System.arraycopy(snaHeader, 0, paddedHeader, 0, snaHeader.length);
        }
        return paddedHeader;
    }

    protected static int dumpGameLaunchCode(OutputStream os, Game game, int index) throws IOException {
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;

            int baseAddress = V1Constants.GAME_STRUCT_OFFSET + V1Constants.GAME_STRUCT_SIZE * index;
            os.write(Z80Opcode.LD_IX_NN(baseAddress + GameHeaderOffsets.IX_OFFSET));
            os.write(Z80Opcode.LD_HL_NN(baseAddress + GameHeaderOffsets.HL_OFFSET));
            boolean interruptDisable = snapshotGame.getGameHeader()
                    .getIff0() == 0;

            os.write(interruptDisable ? Z80Opcode.DI : Z80Opcode.EI);

        } else {
            os.write(new byte[V1Constants.GAME_LAUNCHCODE_SIZE]);
        }
        return V1Constants.GAME_LAUNCHCODE_SIZE;
    }

    private int dumpUncompressedGameCBlocks(OutputStream os, Game game, int offset)
            throws IOException {
        LOGGER.debug("Writing CBlocks for uncompressed game " + game.getName()
                + ", of type " + game.getType()
                + ", at offset " + offset);
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();

        for (int i = 0; i < game.getSlotCount(); i++) {
            if (!game.isSlotZeroed(i)) {
                byte[] block = game.getSlot(i);
                offset -= Constants.SLOT_SIZE;
                LOGGER.debug("Writing CBlock with offset " + offset + " and length " + block.length);
                gameCBlocks.write(offset / Constants.SLOT_SIZE);
                gameCBlocks.write(asLittleEndianWord(Constants.B_00)); //Blocks always at offset 0 (uncompressed)
                gameCBlocks.write(asLittleEndianWord(Constants.SLOT_SIZE));
            } else {
                LOGGER.debug("Writing empty CBlock");
                gameCBlocks.write(EMPTY_CBLOCK);
            }
        }

        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 8, (byte) DandanatorCpcConstants.FILLER_BYTE);
        LOGGER.debug("CBlocks array calculated as " + Util.dumpAsHexString(cBlocksArray));
        os.write(cBlocksArray);
        return offset;
    }

    private int dumpCompressedGameCBlocks(OutputStream os, Game game, int offset)
            throws IOException {
        LOGGER.debug("Writing CBlocks for compressed game " + game.getName()
                + ", of type " + game.getType()
                + ", at offset " + offset);
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            List<byte[]> compressedBlocks = snapshotGame.getCompressedData(ramGameCompressor);
            for (byte[] block : compressedBlocks) {
                if (block != null) {
                    LOGGER.debug("Writing CBlock with offset " + offset + " and length " + block.length);
                    gameCBlocks.write(offset / Constants.SLOT_SIZE);
                    gameCBlocks.write(asLittleEndianWord(offset % Constants.SLOT_SIZE));
                    gameCBlocks.write(asLittleEndianWord(block.length));
                    offset += block.length;
                } else {
                    LOGGER.debug("Writing empty CBlock");
                    gameCBlocks.write(EMPTY_CBLOCK);
                }
            }
        } else {
            throw new IllegalArgumentException("Cannot extract compressed blocks from a non-RAM game");
        }
        //Fill the remaining space with 0xFF
        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 8, (byte) DandanatorCpcConstants.FILLER_BYTE);
        LOGGER.debug("CBlocks array calculated as " + Util.dumpAsHexString(cBlocksArray));
        os.write(cBlocksArray);
        return offset;
    }

    protected static void dumpGameName(OutputStream os, Game game, int index) throws IOException {
        int gameSymbolCode = getGameSymbolCode(game);
        String gameName = String.format("%1d%c%c%s", (index + 1) % DandanatorCpcConstants.SLOT_COUNT,
                gameSymbolCode, gameSymbolCode + 1,
                game.getName());
        os.write(asNullTerminatedByteArray(gameName, DandanatorCpcConstants.GAMENAME_SIZE));
    }

    private static byte[] getGameChunk(Game game) {
        byte[] chunk = new byte[DandanatorCpcConstants.GAME_CHUNK_SIZE];
        if (game instanceof SnapshotGame) {
            System.arraycopy(game.getSlot(DandanatorCpcConstants.GAME_CHUNK_SLOT),
                    Constants.SLOT_SIZE - DandanatorCpcConstants.GAME_CHUNK_SIZE,
                chunk, 0, DandanatorCpcConstants.GAME_CHUNK_SIZE);
        }
        return chunk;
    }

    private int dumpGameHeader(OutputStream os, int index, Game game,
                               GameChunk gameChunk, int offset) throws IOException {
        os.write(getPaddedGameHeader(game));
        os.write(game.getType().typeId());
        os.write(getGameChunk(game));
        os.write(isGameCompressed(game) ? Constants.B_01 : Constants.B_00);
        os.write(game.getType().typeId());
        os.write(isGameScreenHold(game) ? Constants.B_01 : Constants.B_00);
        os.write(0);
        os.write(0); //Upper and lower active roms (TODO)
        dumpGameLaunchCode(os, game, index);
        offset =  isGameCompressed(game) ?
                dumpCompressedGameCBlocks(os, game, offset) :
                dumpUncompressedGameCBlocks(os, game, offset);
        dumpGameName(os, game, index);
        return offset;

    }

    private void dumpGameHeaders(ByteArrayOutputStream os, GameChunk[] gameChunkTable) throws IOException {
        int index = 0;
        //forwardOffset after the slot zero
        int forwardOffset = Constants.SLOT_SIZE;
        //backwardsOffset starts before the test ROM
        int backwardsOffset = Constants.SLOT_SIZE * (DandanatorCpcConstants.GAME_SLOTS + 1);
        for (Game game : getApplicationContext().getGameList()) {
            if (isGameCompressed(game)) {
                forwardOffset = dumpGameHeader(os, index, game, gameChunkTable[index], forwardOffset);
            } else {
                backwardsOffset = dumpGameHeader(os, index, game, gameChunkTable[index], backwardsOffset);
            }
            LOGGER.debug("Dumped gamestruct for " + game.getName() + ". Offset: " + os.size());
            index++;
        }
        Util.fillWithValue(os, (byte) 0, V1Constants.GAME_STRUCT_SIZE * (DandanatorCpcConstants.MAX_GAMES - index));
        LOGGER.debug("Filled to end of gamestruct. Offset: " + os.size());
    }

    private static byte[] getScreenTexts(DandanatorCpcConfiguration configuration) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            dumpScreenTexts(os, configuration);
            return os.toByteArray();
        }
    }

    private static byte[] getPokeStructureData(Collection<Game> games) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            for (Game game : games) {
                os.write(getGamePokeCount(game));
            }
            Util.fillWithValue(os, Constants.B_00, DandanatorCpcConstants.MAX_GAMES - games.size());

            int basePokeAddress = DandanatorCpcConstants.POKE_TARGET_ADDRESS +
                    DandanatorCpcConstants.MAX_GAMES * 3;

            for (Game game : games) {
                os.write(asLittleEndianWord(basePokeAddress));
                basePokeAddress += pokeRequiredSize(game);
            }
            Util.fillWithValue(os, Constants.B_00, (DandanatorCpcConstants.MAX_GAMES - games.size()) * 2);

            for (Game game : games) {
                dumpGamePokeData(os, game);
            }
            LOGGER.debug("Poke Structure before compressing: " + Util.dumpAsHexString(os.toByteArray()));
            return os.toByteArray();

        }
    }

    private static GameChunk getCompressedGameChunk(SnapshotGame game, int cBlockOffset) throws IOException {
        try (ByteArrayOutputStream compressedChunk = new ByteArrayOutputStream()) {
            OutputStream compressingOs = Util.getCompressor().getCompressingOutputStream(compressedChunk);
            compressingOs.write(game.getSlot(DandanatorCpcConstants.GAME_CHUNK_SLOT),
                    Constants.SLOT_SIZE - DandanatorCpcConstants.GAME_CHUNK_SIZE,
                    DandanatorCpcConstants.GAME_CHUNK_SIZE);
            compressingOs.flush();
            byte[] compressedData = compressedChunk.toByteArray();
            if (compressedData.length > (DandanatorCpcConstants.GAME_CHUNK_SIZE - 6)) {
                LOGGER.debug("Compressed chunk for " + game.getName() + " exceeds boundaries");
                return getUncompressedGameChunk(game, cBlockOffset);
            } else {
                GameChunk gameChunk = new GameChunk();
                gameChunk.setAddress(cBlockOffset);
                gameChunk.setData(compressedData);
                LOGGER.debug("Compressed chunk for game " + game.getName() + " calculated offset " +
                    gameChunk.getAddress());
                return gameChunk;
            }
        }
    }

    private static GameChunk getUncompressedGameChunk(Game game, int cBlockOffset) throws IOException {
        GameChunk gameChunk = new GameChunk();
        if (game instanceof SnapshotGame) {
            gameChunk.setData(Arrays.copyOfRange(game.getSlot(DandanatorCpcConstants.GAME_CHUNK_SLOT),
                    Constants.SLOT_SIZE - DandanatorCpcConstants.GAME_CHUNK_SIZE,
                    Constants.SLOT_SIZE));
            gameChunk.setAddress(cBlockOffset);
        } else  {
            gameChunk.setData(new byte[0]);
            gameChunk.setAddress(cBlockOffset);
        }
        LOGGER.debug("Uncompressed chunk for game " + game.getName() + " calculated offset " +
                gameChunk.getAddress());
        return gameChunk;
    }

    private static GameChunk[] calculateGameChunkTable(Collection<Game> games, int cBlockOffset) throws IOException {
        List<GameChunk> chunkList = new ArrayList<>();
        for (Game game : games) {
            if (game instanceof SnapshotGame) {
                SnapshotGame snapshotGame = (SnapshotGame) game;
                GameChunk gameChunk = snapshotGame.getCompressed() ?
                        getCompressedGameChunk(snapshotGame, cBlockOffset) :
                        getUncompressedGameChunk(game, cBlockOffset);
                cBlockOffset += gameChunk.getData().length;
                chunkList.add(gameChunk);
            } else {
                GameChunk gameChunk = getUncompressedGameChunk(game, cBlockOffset);
                cBlockOffset += gameChunk.getData().length;
                chunkList.add(gameChunk);
            }
        }
        return chunkList.toArray(new GameChunk[0]);
    }

    private void dumpCompressedGameData(OutputStream os, Game game) throws IOException {
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            for (byte[] compressedSlot : snapshotGame.getCompressedData(ramGameCompressor)) {
                if (compressedSlot != null) {
                    os.write(compressedSlot);
                    LOGGER.debug("Dumped compressed slot for game " + snapshotGame.getName()
                            + " of size: " + compressedSlot.length);
                } else {
                    LOGGER.debug("Skipped zeroed slot");
                }
            }
        }
    }

    private void dumpUncompressedGameData(OutputStream os, Game game) throws IOException {
        for (int i = game.getSlotCount() - 1; i >= 0; i--) {
            if (!game.isSlotZeroed(i)) {
                os.write(game.getSlot(i));
                LOGGER.debug("Dumped uncompressed slot " + i + " for game " + game.getName());
            } else {
                LOGGER.debug("Skipped zeroed slot");
            }
        }
    }

    private static int getUncompressedSlotCount(List<Game> games) {
        int value = 0;
        for (Game game: games) {
            if (!isGameCompressed(game)) {
                value += game.getSlotCount();
            }
        }
        LOGGER.debug("Number of slots from uncompressed games " + value);
        return value;
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            DandanatorCpcConfiguration dmConfiguration = DandanatorCpcConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            List<Game> games = getApplicationContext().getGameList();
            os.write(dmConfiguration.getDandanatorRom(), 0, DandanatorCpcConstants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            os.write((byte) games.size());
            LOGGER.debug("Dumped game count. Offset: " + os.size());

            int cblocksOffset = V1Constants.GREY_ZONE_OFFSET;
            ByteArrayOutputStream cBlocksTable = new ByteArrayOutputStream();
            byte[] compressedScreen = Util.compress(configuration.getBackgroundImage());
            cBlocksTable.write(asLittleEndianWord(cblocksOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreen.length));
            cblocksOffset += compressedScreen.length;

            byte[] compressedScreenTexts = Util.compress(getScreenTexts(dmConfiguration));
            cBlocksTable.write(asLittleEndianWord(cblocksOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreenTexts.length));
            cblocksOffset += compressedScreenTexts.length;

            byte[] compressedPokeData = Util.compress(getPokeStructureData(games));
            cBlocksTable.write(asLittleEndianWord(cblocksOffset));
            cBlocksTable.write(asLittleEndianWord(compressedPokeData.length));
            cblocksOffset += compressedPokeData.length;

            ExtendedCharSet extendedCharset = new ExtendedCharSet(configuration.getCharSet());
            byte[] compressedCharSet = Util.compress(extendedCharset.getCharSet());
            cBlocksTable.write(asLittleEndianWord(cblocksOffset));
            cBlocksTable.write(asLittleEndianWord(compressedCharSet.length));
            cblocksOffset += compressedCharSet.length;

            GameChunk[] gameChunkTable = calculateGameChunkTable(games, cblocksOffset);
            dumpGameHeaders(os, gameChunkTable);
            LOGGER.debug("Dumped game struct. Offset: " + os.size());

            os.write(compressedScreen);
            os.write(compressedScreenTexts);
            os.write(compressedPokeData);
            os.write(compressedCharSet);

            //loader if enough room
            int freeSpace = V1Constants.VERSION_OFFSET - os.size();
            byte[] eepromLoaderCode = getEepromLoaderCode();
            byte[] eepromLoaderScreen = getEepromLoaderScreen();
            int requiredEepromLoaderSpace = eepromLoaderCode.length + eepromLoaderScreen.length;
            int eepromLocation = 0;
            if (requiredEepromLoaderSpace <= freeSpace) {
                eepromLocation = os.size();
                LOGGER.debug("Dumping EEPROM Loader with size " + requiredEepromLoaderSpace
                        + " at offset " + eepromLocation + ". Free space was " + freeSpace);
                cBlocksTable.write(asLittleEndianWord(os.size()));
                os.write(eepromLoaderScreen);
                cBlocksTable.write(asLittleEndianWord(os.size()));
                os.write(eepromLoaderCode);
            } else {
                LOGGER.debug("Skipping EEPROM Loader. Not enough free space: " +
                        freeSpace + ". Needed: " + requiredEepromLoaderSpace);
                cBlocksTable.write(asLittleEndianWord(0));
                cBlocksTable.write(asLittleEndianWord(0));
            }
            Util.fillWithValue(os, (byte) 0, V1Constants.VERSION_OFFSET - os.size());
            LOGGER.debug("Dumped grey zone. Offset: " + os.size());

            os.write(asNullTerminatedByteArray(getVersionInfo(), V1Constants.VERSION_SIZE));
            LOGGER.debug("Dumped version info. Offset: " + os.size());

            os.write(cBlocksTable.toByteArray());
            LOGGER.debug("Dumped CBlocks table. Offset " + os.size());

            os.write(dmConfiguration.isAutoboot() ? 1 : 0);
            LOGGER.debug("Dumped autoboot configuration. Offset: " + os.size());


            Util.fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size());

            LOGGER.debug("Slot zero completed. Offset: " + os.size());

            for (Game game : games) {
                if (isGameCompressed(game)) {
                    dumpCompressedGameData(os, game);
                    LOGGER.debug("Dumped compressed game. Offset: " + os.size());
                }
            }


            ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
            for (int i = games.size() - 1; i >= 0; i--) {
                Game game = games.get(i);
                if (!isGameCompressed(game)) {
                    dumpUncompressedGameData(uncompressedStream, game);
                }
            }

            //Uncompressed data goes at the end minus the extra ROM size
            //and grows backwards
            int uncompressedOffset = Constants.SLOT_SIZE * (DandanatorCpcConstants.GAME_SLOTS + 1)
                    - uncompressedStream.size();
            int gapSize = uncompressedOffset - os.size();
            LOGGER.debug("Gap to uncompressed zone: " + gapSize);
            Util.fillWithValue(os, Constants.B_FF, gapSize);

            os.write(uncompressedStream.toByteArray());
            LOGGER.debug("Dumped uncompressed game data. Offset: " + os.size());

            os.write(dmConfiguration.getExtraRom());
            LOGGER.debug("Dumped custom rom. Offset: " + os.size());

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }

    private static int getGameSize(Game game) throws IOException {
        if (game.getType() == GameType.ROM) {
            return game.getSlotCount() * Constants.SLOT_SIZE;
        } else if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            //Calculate compression always here, to avoid locking the GUI later
            int compressedSize = snapshotGame.getCompressedSize(ramGameCompressor);
            return snapshotGame.getCompressed() ? compressedSize : snapshotGame.getSize();
        } else {
            return game.getSize();
        }
    }

    protected BooleanBinding getGenerationAllowedBinding(ApplicationContext ctx) {
        return Bindings.size(ctx.getGameList())
                .greaterThan(0)
                .and(Bindings.size(ctx.getGameList()).lessThanOrEqualTo(DandanatorCpcConstants.MAX_GAMES))
                .and(currentRomUsage.lessThan(1.0));
    }

    protected double calculateRomUsage() {
        int size = 0;
        for (Game game : getApplicationContext().getGameList()) {
            try {
                size += getGameSize(game);
            } catch (Exception e) {
                LOGGER.warn("Calculating game size usage", e);
            }
        }
        LOGGER.debug("Used size: " + size + ", total size: "
                + DandanatorCpcConstants.GAME_SLOTS * Constants.SLOT_SIZE);
        currentRomUsage.set(((double) size /
                (DandanatorCpcConstants.GAME_SLOTS * Constants.SLOT_SIZE)));
        return currentRomUsage.get();
    }

    @Override
    public RomSetHandlerType type() {
        return RomSetHandlerType.DDNTR_V1;
    }

    protected String generateRomUsageDetail() {
        return String.format(LocaleUtil.i18n("romUsageV5Detail"),
                getApplicationContext().getGameList().size(),
                DandanatorCpcConstants.MAX_GAMES,
                calculateRomUsage() * 100);
    }

    private void prepareAddedGame(Game game) throws IOException {
        getGameSize(game);
    }

    @Override
    public Future<OperationResult> addGame(Game game) {
        return getApplicationContext().addBackgroundTask(() -> {
                try {
                    //Force compression calculation
                    prepareAddedGame(game);
                    Platform.runLater(() -> getApplicationContext().getGameList().add(game));
                } catch (Exception e) {
                    LOGGER.error("Calculating game size", e);
                }
            return OperationResult.successResult();
        });
    }

    @Override
    public void removeGame(Game game) {
        getApplicationContext().getGameList().remove(game);
    }

    private static void printVersionAndPageInfo(CpcScreen screen, int line, int page, int numPages) {
        String versionInfo = getVersionInfo();
        screen.setInk(CpcColor.BLACK);
        screen.setPen(CpcColor.BRIGHTMAGENTA);
        screen.printLine(versionInfo, line, 0);
        if (numPages > 1) {
            screen.setPen(CpcColor.WHITE);
            String pageInfo = numPages > 1 ?
                    String.format("%d/%d", page, numPages) : "";
            String keyInfo = "SPC - ";
            screen.printLine(keyInfo, line, screen.getColumns() - pageInfo.length() - keyInfo.length());
            screen.setPen(CpcColor.YELLOW);
            screen.printLine(pageInfo, line, screen.getColumns() - pageInfo.length());
        }
    }

    private static int getGameSymbolCode(Game game) {
        return ExtendedCharSet.SYMBOL_SPACE;
    }

    private static void printGameNameLine(CpcScreen screen, Game game, int index, int line) {
        screen.setPen(
                isGameScreenHold(game) ? CpcColor.BRIGHTCYAN : CpcColor.BRIGHTGREEN);
        screen.deleteLine(line);
        screen.printLine(String.format("%1d", (index + 1) % DandanatorCpcConstants.SLOT_COUNT), line, 0);
        screen.setPen(CpcColor.BRIGHTWHITE);
        int gameSymbolCode = getGameSymbolCode(game);
        screen.printLine(String.format("%c", gameSymbolCode), line, 1);
        if (isGameCompressed(game)) {
            screen.setPen(CpcColor.BRIGHTYELLOW);
        }
        screen.printLine(String.format("%c", gameSymbolCode + 1), line, 2);
        screen.setPen(isGameScreenHold(game) ? CpcColor.BRIGHTCYAN : CpcColor.BRIGHTGREEN);
        screen.printLine(
                String.format("%s", game.getName()), line, 3);
    }

    private void updateMenuPage(List<Game> gameList, int pageIndex, int numPages) throws IOException {
        DandanatorCpcConfiguration configuration = DandanatorCpcConfiguration.getInstance();
        CpcScreen page = menuImages[pageIndex];
        updateBackgroundImage(page);
        page.setCharSet(new ExtendedCharSet(Configuration.getInstance().getCharSet()).getCharSet());

        page.setInk(CpcColor.BLACK);
        page.setPen(CpcColor.BRIGHTMAGENTA);
        for (int line = page.getLines() - 1; line >= 8; line--) {
            page.deleteLine(line);
        }

        printVersionAndPageInfo(page, 8, pageIndex + 1, numPages);
        int line = 10;
        int gameIndex = pageIndex * DandanatorCpcConstants.SLOT_COUNT;
        int gameCount = 0;
        while (gameIndex < gameList.size() && gameCount < DandanatorCpcConstants.SLOT_COUNT) {
            Game game = gameList.get(gameIndex);
            printGameNameLine(page, game, gameCount++, line++);
            gameIndex++;
        }

        page.setPen(CpcColor.BRIGHTWHITE);
        page.printLine(String.format("P. %s", configuration.getTogglePokesMessage()), 21, 0);
        page.setPen(CpcColor.BRIGHTRED);
        page.printLine(String.format("R. %s", configuration.getExtraRomMessage()), 23, 0);
    }

    protected MenuItem getExportPokesMenuItem() {
        if (exportPokesMenuItem == null) {
            exportPokesMenuItem = new MenuItem(LocaleUtil.i18n("exportPokesMenuEntry"));

            exportPokesMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+P")
            );
            exportPokesMenuItem.disableProperty().bind(applicationContext
                    .gameSelectedProperty().not());
            exportPokesMenuItem.setOnAction(f -> {
                try {
                    exportCurrentGamePokes();
                } catch (Exception e) {
                    LOGGER.error("Exporting current game pokes", e);
                }
            });
        }
        return exportPokesMenuItem;
    }


    protected MenuItem getImportPokesMenuItem() {
        if (importPokesMenuItem == null) {
            importPokesMenuItem = new MenuItem(LocaleUtil.i18n("importPokesMenuEntry"));

            importPokesMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+L")
            );
            importPokesMenuItem.disableProperty().bind(applicationContext
                    .gameSelectedProperty().not());
            importPokesMenuItem.setOnAction(f -> {
                try {
                    importCurrentGamePokes();
                } catch (Exception e) {
                    LOGGER.error("Importing current game pokes", e);
                }
            });
        }
        return importPokesMenuItem;
    }

    private MenuItem getExportExtraRomMenuItem() {
        if (exportExtraRomMenuItem == null) {
            exportExtraRomMenuItem = new MenuItem(LocaleUtil.i18n("exportExtraRomMenuEntry"));
            exportExtraRomMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+E")
            );

            exportExtraRomMenuItem.setOnAction(f -> {
                try {
                    exportExtraRom();
                } catch (Exception e) {
                    LOGGER.error("Exporting extra Rom", e);
                }
            });
        }
        return exportExtraRomMenuItem;
    }

    private void exportExtraRom() {
        DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
        chooser.setTitle(LocaleUtil.i18n("exportExtraRomMenuEntry"));
        chooser.setInitialFileName("dandanator_extra_rom.rom");
        final File saveFile = chooser.showSaveDialog(applicationContext.getApplicationStage());
        if (saveFile != null) {
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                fos.write(DandanatorCpcConfiguration.getInstance().getExtraRom());
            } catch (IOException e) {
                LOGGER.error("Exporting Extra ROM", e);
            }
        }
    }

    protected MenuItem getExportToWavsMenuItem() {
        if (exportToWavsMenuItem == null) {
            exportToWavsMenuItem = new MenuItem(LocaleUtil.i18n("exportToWavsMenuEntry"));

            exportToWavsMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+W")
            );
            exportToWavsMenuItem.disableProperty().bind(generationAllowedProperty.not());

            exportToWavsMenuItem.setOnAction(f -> exportToWavs());
        }
        return exportToWavsMenuItem;
    }

    public void exportToWavs() {
        DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
        chooser.setTitle(LocaleUtil.i18n("exportToWavsMenuEntry"));
        chooser.setInitialFileName("dandanator_wav_romset.zip");
        final File saveFile = chooser.showSaveDialog(applicationContext.getApplicationStage());
        if (saveFile != null) {
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                exportRomSet(bos);
                RomSetUtil.exportToZippedWavFiles(new ByteArrayInputStream(bos.toByteArray()), fos);
            } catch (IOException e) {
                LOGGER.error("Exporting to Wavs", e);
            }
        }
    }

    @Override
    public void updateMenuPreview() {
        LOGGER.debug("updateMenuPreview");
        try {
            List<Game> gameList = getApplicationContext().getGameList();
            int numPages = 1 + ((gameList.size() - 1) / DandanatorCpcConstants.SLOT_COUNT);
            for (int i = 0; i < numPages; i++) {
                updateMenuPage(gameList, i, numPages);
            }
        } catch (Exception e) {
            LOGGER.error("Updating background screen", e);
        }
    }

    protected DandanatorCpcFrameController getDandanatorCpcFrameController(ApplicationContext applicationContext) {
        if (dandanatorCpcFrameController == null) {
            dandanatorCpcFrameController = new DandanatorCpcFrameController();
        }
        dandanatorCpcFrameController.setApplicationContext(applicationContext);
        return dandanatorCpcFrameController;
    }

    protected Pane getDandanatorCpcFrame(ApplicationContext applicationContext) {
        try {
            if (dandanatorCpcFrame == null) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(DandanatorCpcFrameController.class.getResource("dandanatorcpcframe.fxml"));
                loader.setController(getDandanatorCpcFrameController(applicationContext));
                loader.setResources(LocaleUtil.getBundle());
                dandanatorCpcFrame = loader.load();
            } else {
                dandanatorCpcFrameController.setApplicationContext(applicationContext);
            }
            return dandanatorCpcFrame;
        } catch (Exception e) {
            LOGGER.error("Creating DandanatorCpc frame", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public BooleanProperty generationAllowedProperty() {
        return generationAllowedProperty;
    }

    @Override
    public void bind(ApplicationContext applicationContext) {
        LOGGER.debug("Binding RomSetHandler to ApplicationContext");
        this.applicationContext = applicationContext;
        generationAllowedProperty.bind(getGenerationAllowedBinding(applicationContext));

        applicationContext.getRomSetHandlerInfoPane().getChildren()
                .add(getDandanatorCpcFrame(applicationContext));
        updateMenuPreview();

        DandanatorCpcConfiguration.getInstance().togglePokesMessageProperty()
                .addListener(updateImageListener);
        DandanatorCpcConfiguration.getInstance().extraRomMessageProperty()
                .addListener(updateImageListener);
        Configuration.getInstance().backgroundImagePathProperty()
                .addListener(updateImageListener);
        Configuration.getInstance().charSetPathProperty()
                .addListener(updateImageListener);

        applicationContext.getGameList().addListener(updateImageListener);
        applicationContext.getGameList().addListener(updateRomUsageListener);

        applicationContext.getExtraMenu().getItems().addAll(
                getExportPokesMenuItem(), getImportPokesMenuItem(),
                getExportExtraRomMenuItem(),
                getExportToWavsMenuItem());

        updateRomUsage();
        previewUpdateTimer.start();
        Configuration.getInstance().setRamGameCompressor(ramGameCompressor);

    }

    @Override
    public void unbind() {
        LOGGER.debug("Unbinding RomSetHandler from ApplicationContext");
        DandanatorCpcConfiguration.getInstance().togglePokesMessageProperty()
                .removeListener(updateImageListener);
        DandanatorCpcConfiguration.getInstance().extraRomMessageProperty()
                .removeListener(updateImageListener);
        generationAllowedProperty.unbind();
        generationAllowedProperty.set(false);
        applicationContext.getRomSetHandlerInfoPane().getChildren().clear();

        applicationContext.getExtraMenu().getItems().removeAll(
                getExportPokesMenuItem(),
                getImportPokesMenuItem(),
                getExportExtraRomMenuItem(),
                getExportToWavsMenuItem());
        applicationContext.getGameList().removeListener(updateImageListener);
        applicationContext.getGameList().removeListener(updateRomUsageListener);
        applicationContext = null;
        previewUpdateTimer.stop();
    }
}