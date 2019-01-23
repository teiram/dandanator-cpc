package com.grelobites.romgenerator.handlers.dandanatorcpc.v2;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.handlers.dandanatorcpc.*;
import com.grelobites.romgenerator.handlers.dandanatorcpc.v1.GameHeaderV1Serializer;
import com.grelobites.romgenerator.handlers.dandanatorcpc.view.DandanatorCpcFrameController;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.util.*;
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

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public class DandanatorCpcV2RomSetHandler extends DandanatorCpcRomSetHandlerSupport implements RomSetHandler {
    private static class Offsets {
        public int forwardOffset;
        public int backwardsOffset;
        public Offsets(int forwardOffset, int backwardsOffset) {
            this.forwardOffset = forwardOffset;
            this.backwardsOffset = backwardsOffset;
        }
        @Override
        public String toString() {
            return "Offsets{" +
                    "forwardOffset=" + forwardOffset +
                    ", backwardsOffset=" + backwardsOffset +
                    '}';
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorCpcV2RomSetHandler.class);

    private static final byte[] EMPTY_CBLOCK = new byte[5];
    private static final int MAX_MENU_PAGES = 3;

    private static RamGameCompressor ramGameCompressor = new DandanatorCpcRamGameCompressor();
    private DoubleProperty currentRomUsage;

    protected DandanatorCpcFrameController dandanatorCpcFrameController;
    protected Pane dandanatorCpcFrame;
    protected MenuItem exportPokesMenuItem;
    protected MenuItem importPokesMenuItem;
    protected MenuItem exportExtraRomMenuItem;
    protected MenuItem exportRescueEewriterToCdt;
    protected MenuItem exportRescueEewriterToDsk;
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
            menuImages[i] = new CpcScreen(1); //Use mode 1 here
            updateBackgroundImage(menuImages[i]);
        }
    }

    private void updateRomUsage() {
        getApplicationContext().setRomUsage(calculateRomUsage());
        getApplicationContext().setRomUsageDetail(generateRomUsageDetail());
    }

    public DandanatorCpcV2RomSetHandler() throws IOException {
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
        EepromWriterConfiguration configuration = EepromWriterConfiguration.getInstance();
        byte[] eewriter = Util.fromInputStream(configuration.getRomsetLoaderStream());
        return Util.compress(eewriter);
    }

    private static byte[] getEepromLoaderScreen() throws IOException {
        EepromWriterConfiguration configuration = EepromWriterConfiguration.getInstance();
        byte[] screen = Util.fromInputStream(configuration.getScreenStream());
        return RomSetUtil.getCompressedScreen(screen);
    }

    private static byte[] getPaddedGameHeader(Game game) throws IOException {
        byte[] paddedHeader = new byte[V2Constants.GAME_HEADER_SIZE];
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

    protected static void dumpGameLaunchCode(OutputStream os, Game game, int index) throws IOException {
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;

            int baseAddress = V2Constants.GAME_STRUCT_OFFSET + V2Constants.GAME_STRUCT_SIZE * index;
            os.write(Z80Opcode.LD_IX_NN(baseAddress + GameHeaderOffsets.IX_OFFSET));
            os.write(Z80Opcode.LD_HL_NN(baseAddress + GameHeaderOffsets.HL_OFFSET));
            boolean interruptDisable = snapshotGame.getGameHeader()
                    .getIff0() == 0;

            os.write(interruptDisable ? Z80Opcode.DI : Z80Opcode.EI);
            os.write(Z80Opcode.RET);
        } else {
            os.write(new byte[V2Constants.GAME_LAUNCHCODE_SIZE]);
        }
    }

    private void dumpGameCBlocks(OutputStream os, Game game, Offsets offsets)
            throws IOException {
        LOGGER.debug("Writing CBlocks for game " + game.getName()
                + ", of type " + game.getType()
                + ", with offsets " + offsets);
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();
        List<byte[]> blocks;
        if (game instanceof SnapshotGame && ((SnapshotGame) game).getCompressed()) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            blocks = snapshotGame.getCompressedData(ramGameCompressor);
        } else {
            blocks = game.getData();
        }
        //For MLD games we encode the number of slots in the first CBlock. The rest set to FF
        if (game instanceof MLDGame) {
            int requiredSlots = game.getSlotCount(); //Since game.getSize() includes save space
            int startOffset = offsets.forwardOffset - (requiredSlots * Constants.SLOT_SIZE);
            LOGGER.debug("Writing MLD CBlock with offset {}", startOffset);
            gameCBlocks.write(startOffset / Constants.SLOT_SIZE);
            gameCBlocks.write(asLittleEndianWord(Constants.B_00));
            gameCBlocks.write(asLittleEndianWord(requiredSlots));
            offsets.forwardOffset = startOffset;
        } else {
        for (byte[] block: blocks) {
            if (block != null) {
                if (block.length < Constants.SLOT_SIZE) {
                    LOGGER.debug("Writing compressed CBlock with offset {} and length {}", offsets.forwardOffset, block.length);
                    gameCBlocks.write(offsets.forwardOffset / Constants.SLOT_SIZE);
                    gameCBlocks.write(asLittleEndianWord(offsets.forwardOffset % Constants.SLOT_SIZE));
                    gameCBlocks.write(asLittleEndianWord(block.length));
                    offsets.forwardOffset += block.length;
                } else if (block.length == Constants.SLOT_SIZE) {
                    offsets.backwardsOffset -= Constants.SLOT_SIZE;
                    LOGGER.debug("Writing uncompressed CBlock with offset {} and length {}", offsets.backwardsOffset, block.length);
                    gameCBlocks.write(offsets.backwardsOffset / Constants.SLOT_SIZE);
                    gameCBlocks.write(asLittleEndianWord(Constants.B_00)); //Blocks always at offset 0 (uncompressed)
                    gameCBlocks.write(asLittleEndianWord(Constants.SLOT_SIZE));
                } else {
                    throw new IllegalStateException("Attempt to write a block exceeding " + Constants.SLOT_SIZE);
                }
            } else {
                LOGGER.debug("Writing empty CBlock");
                gameCBlocks.write(EMPTY_CBLOCK);
            }
        }
        }

        //Fill the remaining space with 0xFF
        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(),
                5 * 8, (byte) DandanatorCpcConstants.FILLER_BYTE);
        LOGGER.debug("CBlocks array calculated as " + Util.dumpAsHexString(cBlocksArray));
        os.write(cBlocksArray);
    }

    protected static void dumpGameName(OutputStream os, Game game, int index) throws IOException {
        int gameSymbolCode = getGameSymbolCode(game);
        String gameName = String.format("%1d%c%c%c%s", (index + 1) % DandanatorCpcConstants.SLOT_COUNT,
                gameSymbolCode, gameSymbolCode + 1, gameSymbolCode + 2,
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

    private int getGameType(Game game) {
        int value = game.getType().typeId();
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            if (snapshotGame.getHardwareMode() != null) {
                if (snapshotGame.getHardwareMode().supported()) {
                    value |= (1 << 7) | ((snapshotGame.getHardwareMode().snaValue() & 3) << 4);
                }
            }
        }
        LOGGER.debug("Gametype calculated for {} is {}", game, String.format("0x%02x", value));
        return value;
    }

    private static int getCurrentRasterInterrupt(Game game) {
        if (game instanceof SnapshotGame) {
            return ((SnapshotGame) game).getCurrentRasterInterrupt();
        } else {
            return 0;
        }
    }

    private void dumpGameHeader(OutputStream os, int index, Game game,
                               Offsets offsets) throws IOException {
        os.write(getPaddedGameHeader(game));
        os.write(game.getType().typeId());
        os.write(getGameChunk(game));
        os.write(isGameCompressed(game) ? Constants.B_01 : Constants.B_00);
        os.write(isGameScreenHold(game) ? Constants.B_01 : Constants.B_00);
        os.write(0); //Upper and lower active roms. Unused in V2
        os.write(getCurrentRasterInterrupt(game));
        dumpGameLaunchCode(os, game, index);
        dumpGameCBlocks(os, game, offsets);
        dumpGameName(os, game, index);
    }

    private void dumpGameHeaders(ByteArrayOutputStream os) throws IOException {
        int index = 0;
        //forwardOffset after the slot zero
        //backwardsOffset starts before the test ROM
        Offsets offsets = new Offsets(Constants.SLOT_SIZE,
                Constants.SLOT_SIZE * (DandanatorCpcConstants.EEPROM_SLOTS
                        - getReservedSlots(Configuration.getInstance())));
        for (Game game : getApplicationContext().getGameList()) {
            dumpGameHeader(os, index, game, offsets);
            LOGGER.debug("Dumped gamestruct for " + game.getName() + ". Offset: " + os.size());
            index++;
        }
        Util.fillWithValue(os, (byte) 0, V2Constants.GAME_STRUCT_SIZE * (DandanatorCpcConstants.MAX_GAMES - index));
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

    private void dumpCompressedGameData(OutputStream os, Game game) throws IOException {
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            for (byte[] compressedSlot : snapshotGame.getCompressedData(ramGameCompressor)) {
                if (compressedSlot != null) {
                    if (compressedSlot.length < Constants.SLOT_SIZE) {
                        os.write(compressedSlot);
                        LOGGER.debug("Dumped compressed slot for game " + snapshotGame.getName()
                                + " of size: " + compressedSlot.length);
                    } else {
                        LOGGER.debug("Skipped uncompressed slot for game {}", snapshotGame.getName());
                    }
                } else {
                    LOGGER.debug("Skipped zeroed slot");
                }
            }
        }
    }

    private static int gameUncompressedSlotCount(Game game) throws IOException {
        List<byte[]> blocks;
        int count = 0;
        if (game instanceof SnapshotGame && ((SnapshotGame) game).getCompressed()) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            blocks = snapshotGame.getCompressedData(ramGameCompressor);
        } else {
            blocks = game.getData();
        }
        for (byte[] block : blocks) {
            if (block != null && block.length == Constants.SLOT_SIZE) {
                count++;
            }
        }
        return count;
    }

    private static int getUncompressedSlotCount(List<Game> games) throws IOException {
        int value = 0;
        for (Game game: games) {
            value += gameUncompressedSlotCount(game);
        }
        LOGGER.debug("Total Number of uncompressed slots " + value);
        return value;
    }

    private void dumpUncompressedGameData(OutputStream os, Game game) throws IOException {
        if (isGameCompressed(game)) {
            //Dump only compressed slots with size == 16384
            SnapshotGame snapshotGame = (SnapshotGame) game;
            List<byte[]> compressedSlots = snapshotGame.getCompressedData(ramGameCompressor);
            for (int i = compressedSlots.size() - 1; i >= 0; i--) {
                byte[] slotData = compressedSlots.get(i);
                if (slotData != null && slotData.length == Constants.SLOT_SIZE) {
                    LOGGER.debug("Dumped uncompressed slot {} for compressed game {}", i, game.getName());
                    os.write(slotData);
                }
            }
        } else {
            for (int i = game.getSlotCount() - 1; i >= 0; i--) {
                if (!game.isSlotZeroed(i)) {
                    os.write(game.getSlot(i));
                    LOGGER.debug("Dumped uncompressed slot " + i + " for game " + game.getName());
                } else {
                    LOGGER.debug("Skipped zeroed slot");
                }
            }
        }
    }

    private int dumpMLDGameData(OutputStream os, Game game, int lastMldSaveSector,
                                int currentSlot) throws IOException {
        MLDGame mldGame = (MLDGame) game;
        mldGame.reallocate(currentSlot);
        lastMldSaveSector = mldGame.allocateSaveSpace(lastMldSaveSector);

        for (int i = 0; i < game.getSlotCount(); i++) {
            os.write(game.getSlot(i));
        }
        return lastMldSaveSector;
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            DandanatorCpcConfiguration dmConfiguration = DandanatorCpcConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            List<Game> games = getApplicationContext().getGameList();
            os.write(dmConfiguration.getDandanatorRom(), 0, V2Constants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            os.write((byte) games.size());
            LOGGER.debug("Dumped game count. Offset: " + os.size());

            dumpGameHeaders(os);
            LOGGER.debug("Dumped game struct. Offset: {}", os.size());

            os.write(getPokeStructureData(games));
            LOGGER.debug("Dumped poke struct. Offset: {}", os.size());

            int greyAreaOffset = os.size();
            ByteArrayOutputStream cBlocksTable = new ByteArrayOutputStream();
            byte[] compressedScreen = RomSetUtil.getCompressedScreen(configuration.getBackgroundImage());
            cBlocksTable.write(asLittleEndianWord(greyAreaOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreen.length));
            greyAreaOffset += compressedScreen.length;

            byte[] compressedScreenTexts = Util.compress(getScreenTexts(dmConfiguration));
            cBlocksTable.write(asLittleEndianWord(greyAreaOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreenTexts.length));
            greyAreaOffset += compressedScreenTexts.length;

            ExtendedCharSet extendedCharset = new ExtendedCharSet(configuration.getCharSet());
            byte[] compressedCharSet = Util.compress(RomSetUtil.encodeCharset(extendedCharset.getCharSet()));
            cBlocksTable.write(asLittleEndianWord(greyAreaOffset));
            cBlocksTable.write(asLittleEndianWord(compressedCharSet.length));

            os.write(compressedScreen);
            os.write(compressedScreenTexts);
            os.write(compressedCharSet);

            //loader if enough room
            int freeSpace = V2Constants.VERSION_OFFSET - os.size();
            byte[] eepromLoaderCode = getEepromLoaderCode();
            byte[] eepromLoaderScreen = getEepromLoaderScreen();
            int requiredEepromLoaderSpace = eepromLoaderCode.length + eepromLoaderScreen.length;
            int eepromLocation;
            if (requiredEepromLoaderSpace <= freeSpace) {
                eepromLocation = os.size();
                LOGGER.debug("Dumping EEPROM Loader with size {} at offset {}. Free space was {}",
                        requiredEepromLoaderSpace,
                        eepromLocation,
                        freeSpace);
                cBlocksTable.write(asLittleEndianWord(os.size()));
                os.write(eepromLoaderScreen);
                cBlocksTable.write(asLittleEndianWord(os.size()));
                os.write(eepromLoaderCode);
            } else {
                LOGGER.debug("Skipping EEPROM Loader. Not enough free space: {}. Needed {}",
                        freeSpace, requiredEepromLoaderSpace);
                cBlocksTable.write(asLittleEndianWord(0));
                cBlocksTable.write(asLittleEndianWord(0));
            }
            //Empty entry in CBlocks table
            cBlocksTable.write(asLittleEndianWord(0));
            cBlocksTable.write(asLittleEndianWord(0));

            Util.fillWithValue(os, (byte) 0, V2Constants.EXTRA_ROM_PRESENT_OFFSET - os.size());
            LOGGER.debug("Dumped grey zone. Offset: {}", os.size());

            os.write((configuration.isIncludeExtraRom() ? Constants.B_01 : Constants.B_00));
            os.write((configuration.isEnforceFollowRom() ? Constants.B_01: Constants.B_00));
            if (configuration.isEnforceFollowRom()) {
                int baseSlot = 30; //30
                if (configuration.isIncludeExtraRom()) {
                    baseSlot--; //29
                }
                os.write((byte) ((baseSlot - 28) * 8));  //464 ROM Slot (base 28, increment 8)
                baseSlot ++;
                os.write((byte) baseSlot);  //464 BASIC ROM Slot
            } else {
                os.write(Constants.B_FF);
                os.write(Constants.B_FF);
            }

            os.write(asNullTerminatedByteArray(getVersionInfo(), V2Constants.VERSION_SIZE));
            LOGGER.debug("Dumped version info. Offset: {}", os.size());

            os.write(cBlocksTable.toByteArray());
            LOGGER.debug("Dumped CBlocks table {}. Offset {}",
                    Util.dumpAsHexString(cBlocksTable.toByteArray()), os.size());

            os.write(dmConfiguration.isAutoboot() ? 1 : 0);
            LOGGER.debug("Dumped autoboot configuration. Offset: {}", os.size());

            Util.fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size());

            LOGGER.debug("Slot zero completed. Offset: {}", os.size());

            for (Game game : games) {
                if (isGameCompressed(game)) {
                    dumpCompressedGameData(os, game);
                    LOGGER.debug("Dumped compressed game. Offset: " + os.size());
                }
            }

            int currentSlot = DandanatorCpcConstants.GAME_SLOTS + 1
                    - getUncompressedSlotCount(games);

            int lastMldSaveSector = (4 * currentSlot) - 1;

            ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
            for (int i = games.size() - 1; i >= 0; i--) {
                Game game = games.get(i);
                if (game instanceof MLDGame) {
                    lastMldSaveSector = dumpMLDGameData(uncompressedStream, game,
                            lastMldSaveSector, currentSlot);
                } else {
                    dumpUncompressedGameData(uncompressedStream, game);
                }
            }

            //Uncompressed data goes at the end minus the required space for extra ROM and/or
            //machine firmwares and grows backwards
            int uncompressedOffset = Constants.SLOT_SIZE * (DandanatorCpcConstants.EEPROM_SLOTS - getReservedSlots(configuration))
                    - uncompressedStream.size();
            int gapSize = uncompressedOffset - os.size();
            LOGGER.debug("Gap to uncompressed zone: " + gapSize);
            Util.fillWithValue(os, Constants.B_FF, gapSize);

            os.write(uncompressedStream.toByteArray());
            LOGGER.debug("Dumped uncompressed game data. Offset: " + os.size());

            if (configuration.isEnforceFollowRom()) {
                os.write(DandanatorCpcConstants.getCpc464Firmware());
                LOGGER.debug("Dumped 464 firmware. Offset {}", os.size());
                os.write(DandanatorCpcConstants.getCpc464Basic());
                LOGGER.debug("Dumped 464 Basic. Offset {}", os.size());
            }
            if (configuration.isIncludeExtraRom()) {
                os.write(dmConfiguration.getExtraRom());
                LOGGER.debug("Dumped custom rom. Offset: {}", os.size());
            }

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }

    private int getReservedSlots(Configuration configuration) {
        int value = configuration.isIncludeExtraRom() ? 1 : 0;
        value += configuration.isEnforceFollowRom() ? 2 : 0;
        return value;
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
        Configuration configuration = Configuration.getInstance();
        for (Game game : getApplicationContext().getGameList()) {
            try {
                size += getGameSize(game);
            } catch (Exception e) {
                LOGGER.warn("Calculating game size usage", e);
            }
        }
        size += getReservedSlots(configuration) * Constants.SLOT_SIZE;

        LOGGER.debug("Used size: {} , total size: {}", size,
                DandanatorCpcConstants.GAME_SLOTS * Constants.SLOT_SIZE);
        currentRomUsage.set(((double) size /
                (DandanatorCpcConstants.GAME_SLOTS * Constants.SLOT_SIZE)));
        return currentRomUsage.get();
    }

    @Override
    public RomSetHandlerType type() {
        return RomSetHandlerType.DDNTR_V2;
    }

    protected String generateRomUsageDetail() {
        return String.format(LocaleUtil.i18n("romUsageDetail"),
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
        screen.setPaper(CpcColor.BLACK);
        screen.setPen(CpcColor.SEAGREEN);
        screen.printLine(versionInfo, line, 0);

        screen.setPen(CpcColor.WHITE);
        screen.printLine("L. Loader", line, 15);
        if (numPages > 1) {
            String pageInfo = String.format("%d/%d", page, numPages);
            int pos = 30;
            if (page > 1) {
                screen.printIcon(ExtendedCharSet.SYMBOL_LEFT_ARROW_CODE, line, pos);
            }
            pos += 2;
            screen.setPen(CpcColor.SEAGREEN);
            screen.printLine(pageInfo, line, pos);
            if (page < numPages) {
                pos += 4;
                screen.printIcon(ExtendedCharSet.SYMBOL_RIGHT_ARROW_CODE, line, pos);
            }
        }
    }

    private static int getGameSymbolCode(Game game) {
        switch (game.getType()) {
            case ROM:
                return ExtendedCharSet.SYMBOL_ROM_0_CODE;
            case RAM64:
                return ExtendedCharSet.SYMBOL_64K_0_CODE;
            case RAM128:
                return ExtendedCharSet.SYMBOL_128K_0_CODE;
            default:
                return ExtendedCharSet.SYMBOL_64K_0_CODE;
        }
    }

    private static void printGameNameLine(CpcScreen screen, Game game, int index, int line) {
        screen.setPen(new CpcGradient(CpcColor.BRIGHTWHITE, 5,
                CpcColor.WHITE));
        screen.deleteLine(line);
        screen.printLine(String.format("%1d", (index + 1) % DandanatorCpcConstants.SLOT_COUNT),
                line, 0);
        screen.printSymbol(getGameSymbolCode(game), line, 1);
        screen.printLine(
                String.format("%s", game.getName()), line, 4);
    }

    private void updateMenuPage(List<Game> gameList, int pageIndex, int numPages) throws IOException {
        DandanatorCpcConfiguration dConfiguration = DandanatorCpcConfiguration.getInstance();
        Configuration configuration = Configuration.getInstance();
        CpcScreen page = menuImages[pageIndex];
        updateBackgroundImage(page);
        page.setCharSet(new ExtendedCharSet(Configuration.getInstance().getCharSet()).getCharSet());

        page.setPaper(CpcColor.BLACK);
        page.setPen(CpcColor.BRIGHTWHITE);
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

        page.setPen(CpcColor.BRIGHTYELLOW);
        page.printLine(String.format("P. %s", dConfiguration.getTogglePokesMessage()), 21, 0);
        if (configuration.isIncludeExtraRom()) {
            page.setPen(CpcColor.BRIGHTRED);
            page.printLine(String.format("R. %s", dConfiguration.getExtraRomMessage()), 23, 0);
        }
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

    private MenuItem getExportRescueEewriterToCdt() {
        if (exportRescueEewriterToCdt == null) {
            exportRescueEewriterToCdt = new MenuItem(LocaleUtil.i18n("exportRescueEewriterToCdtMenuEntry"));
            exportRescueEewriterToCdt.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+C")
            );

            exportRescueEewriterToCdt.setOnAction(f -> {
                try {
                    exportRescueEewriterToCdt();
                } catch (Exception e) {
                    LOGGER.error("Exporting Rescue EEWriter to CDT", e);
                }
            });
        }
        return exportRescueEewriterToCdt;
    }

    private MenuItem getExportRescueEewriterToDsk() {
        if (exportRescueEewriterToDsk == null) {
            exportRescueEewriterToDsk = new MenuItem(LocaleUtil.i18n("exportRescueEewriterToDskMenuEntry"));
            exportRescueEewriterToDsk.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+K")
            );

            exportRescueEewriterToDsk.setOnAction(f -> {
                try {
                    exportRescueEewriterToDsk();
                } catch (Exception e) {
                    LOGGER.error("Exporting Rescue EEWriter to DSK", e);
                }
            });
        }
        return exportRescueEewriterToDsk;
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

    private void exportRescueEewriterToCdt() {
        DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
        chooser.setTitle(LocaleUtil.i18n("exportRescueEewriterToCdtMenuEntry"));
        chooser.setInitialFileName("dandanator_rescue.cdt");
        final File saveFile = chooser.showSaveDialog(applicationContext.getApplicationStage());
        if (saveFile != null) {
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                RescueUtil.generateRescueCdt(fos);
            } catch (IOException e) {
                LOGGER.error("Generating Rescue CDT", e);
            }
        }
    }

    private void exportRescueEewriterToDsk() {
        DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
        chooser.setTitle(LocaleUtil.i18n("exportRescueEewriterToDskMenuEntry"));
        chooser.setInitialFileName("dandanator_rescue.dsk");
        final File saveFile = chooser.showSaveDialog(applicationContext.getApplicationStage());
        if (saveFile != null) {
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                RescueUtil.generateRescueDisk(fos);
            } catch (IOException e) {
                LOGGER.error("Generating Rescue DSK", e);
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
        Configuration.getInstance().includeExtraRomProperty().addListener(updateRomUsageListener);
        Configuration.getInstance().includeExtraRomProperty().addListener(updateImageListener);
        Configuration.getInstance().enforceFollowRomProperty().addListener(updateRomUsageListener);

        applicationContext.getExtraMenu().getItems().addAll(
                getExportPokesMenuItem(), getImportPokesMenuItem(),
                getExportExtraRomMenuItem(),
                getExportRescueEewriterToCdt(),
                getExportRescueEewriterToDsk());

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
                getExportRescueEewriterToCdt(),
                getExportRescueEewriterToDsk());
        applicationContext.getGameList().removeListener(updateImageListener);
        applicationContext.getGameList().removeListener(updateRomUsageListener);
        applicationContext = null;
        previewUpdateTimer.stop();
    }
}