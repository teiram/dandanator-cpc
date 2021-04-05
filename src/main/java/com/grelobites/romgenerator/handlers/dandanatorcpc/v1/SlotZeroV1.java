package com.grelobites.romgenerator.handlers.dandanatorcpc.v1;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.RomSetUtil;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.DandanatorCpcImporter;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameBlock;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameMapper;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.SlotZero;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.SlotZeroBase;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SlotZeroV1 extends SlotZeroBase implements SlotZero {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroV1.class);
    private byte[] charSet;
    private byte[] screen;
    private byte[] screenPalette;
    private List<GameMapperV1> gameMappers;
    private String extraRomMessage;
    private String togglePokesMessage;
    private String launchGameMessage;
    private String selectPokesMessage;
    List<GameBlock> gameBlocks;

    public SlotZeroV1(byte[] data) {
        super(data);
    }

    @Override
    public boolean validate() {
        try {
            return getMajorVersion() == 1;
        } catch (Exception e) {
            LOGGER.debug("Validation failed", e);
            return false;
        }
    }

    @Override
    public void parse() throws IOException {
        PositionAwareInputStream zis = new PositionAwareInputStream(data());
        zis.safeSkip(V1Constants.BASEROM_SIZE);
        int gameCount = zis.read();
        LOGGER.debug("Read number of games: " + gameCount);
        gameMappers = new ArrayList<>();
        gameBlocks = new ArrayList<>();
        for (int i = 0; i < gameCount; i++) {
            GameMapperV1 mapper = GameMapperV1.fromRomSet(zis, this);
            gameBlocks.addAll(mapper.getBlocks());
            gameMappers.add(mapper);
        }

        zis.safeSkip(V1Constants.GAME_STRUCT_SIZE * (DandanatorCpcConstants.MAX_GAMES - gameCount));

        LOGGER.debug("About to read poke data with offset {}", zis.position());
        for (int i = 0; i < gameCount; i++) {
            GameMapperV1 mapper = gameMappers.get(i);
            mapper.setTrainerCount(zis.read());
            LOGGER.debug("Number of trainers for game {}: {}", i, mapper.getTrainerCount());
        }
        zis.safeSkip(DandanatorCpcConstants.MAX_GAMES - gameCount); //Empty slots
        zis.safeSkip(DandanatorCpcConstants.MAX_GAMES * 2);         //Base game poke addresses
        LOGGER.debug("Reading poke data. Offset {}", zis.position());

        for (int i = 0; i < gameCount; i++) {
            GameMapperV1 mapper = gameMappers.get(i);
            int trainerCount = mapper.getTrainerCount();
            if (trainerCount > 0) {
                LOGGER.debug("Importing {} trainers", trainerCount);
                for (int j = 0; j < trainerCount; j++) {
                    int pokeCount = zis.read();
                    String trainerName = Util.getNullTerminatedString(zis, 3, 24);
                    Optional<Trainer> trainer = mapper.getTrainerList().addTrainerNode(trainerName);
                    if (trainer.isPresent() && pokeCount > 0) {
                        LOGGER.debug("Importing {} pokes on trainer {}", pokeCount, trainerName);
                        for (int k = 0; k < pokeCount; k++) {
                            int address = Util.readAsLittleEndian(zis);
                            int value = zis.read();
                            trainer.map(t -> {
                                t.addPoke(address, value);
                                return true;
                            });
                        }
                    }
                }
            }
        }
        LOGGER.debug("Pokes read. Offset {}", zis.position());
        int compressedScreenOffset = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET);
        int compressedScreenBlocks = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 2);
        LOGGER.debug("Compressed screen located at " + compressedScreenOffset + ", blocks "
                + compressedScreenBlocks);
        int compressedTextDataOffset = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 4);
        int compressedTextDataBlocks = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 6);
        LOGGER.debug("Compressed text data located at " + compressedTextDataOffset + ", blocks "
                + compressedTextDataBlocks);
        int compressedCharsetOffset = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 8);
        int compressedCharsetBlocks = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 10);
        LOGGER.debug("Compressed Charset located at " + compressedCharsetOffset
                + ", blocks " + compressedCharsetBlocks);

        screen = uncompress(zis, compressedScreenOffset, compressedScreenBlocks);
        screenPalette = ImageUtil.embeddedPalette(screen);

        byte[] textData = uncompress(zis, compressedTextDataOffset, compressedTextDataBlocks);
        byte[] encodedCharset = uncompress(zis, compressedCharsetOffset, compressedCharsetBlocks);

        ByteArrayInputStream textDataStream = new ByteArrayInputStream(textData);
        extraRomMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorCpcConstants.GAMENAME_SIZE);
        togglePokesMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorCpcConstants.GAMENAME_SIZE);
        launchGameMessage = Util.getNullTerminatedString(textDataStream, 7, DandanatorCpcConstants.GAMENAME_SIZE);
        selectPokesMessage = Util.getNullTerminatedString(textDataStream, DandanatorCpcConstants.GAMENAME_SIZE);

        charSet = RomSetUtil.decodeCharset(encodedCharset);
    }

    @Override
    public byte[] getCharSet() {
        return charSet;
    }

    @Override
    public byte[] getScreen() {
        return screen;
    }

    @Override
    public byte[] getScreenPalette() {
        return screenPalette;
    }

    @Override
    public void populateGameSlots(PositionAwareInputStream is) throws IOException {
        //Order gameBlocks to read them in order from the stream
        gameBlocks.sort(Comparator.comparingInt(GameBlock::getInitSlot)
                .thenComparingInt(GameBlock::getStart));
        for (GameBlock block : gameBlocks) {
            if (block.getInitSlot() < 0xff) {
                LOGGER.debug("Populating game block " + block);
                if (block.getInitSlot() > 0) {
                    if (block.compressed) {
                        int offset = (block.getInitSlot() - 1) * Constants.SLOT_SIZE + block.getStart();
                        LOGGER.debug("Offsetting to {}", offset - is.position());
                        is.safeSkip(offset - is.position());
                        block.rawdata = Util.fromInputStream(is, block.size);
                        block.data = uncompressByteArray(block.rawdata);
                    } else {
                        block.data = copy(is,
                                (block.getInitSlot() - 1) * Constants.SLOT_SIZE + block.getStart(), block.size);
                        block.rawdata = block.gameCompressed ? block.data : null;
                    }
                } else {
                    block.data = Constants.ZEROED_SLOT;
                    block.rawdata = null;
                }
            }
        }
    }

    @Override
    public List<? extends GameMapper> getGameMappers() {
        return gameMappers;
    }

    @Override
    public DandanatorCpcImporter getImporter() {
        return new DandanatorCpcV1Importer();
    }

    @Override
    public String getExtraRomMessage() {
        return extraRomMessage;
    }

    @Override
    public String getTogglePokesMessage() {
        return togglePokesMessage;
    }

    @Override
    public String getLaunchGameMessage() {
        return launchGameMessage;
    }

    @Override
    public String getSelectPokesMessage() {
        return selectPokesMessage;
    }

}
