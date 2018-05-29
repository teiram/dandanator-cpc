package com.grelobites.romgenerator.handlers.dandanatorcpc.v1;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.DandanatorCpcImporter;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameBlock;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameChunk;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.GameMapper;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.SlotZero;
import com.grelobites.romgenerator.handlers.dandanatorcpc.model.SlotZeroBase;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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


        int compressedScreenOffset = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET);
        int compressedScreenBlocks = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 2);
        LOGGER.debug("Compressed screen located at " + compressedScreenOffset + ", blocks "
                + compressedScreenBlocks);
        int compressedTextDataOffset = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 4);
        int compressedTextDataBlocks = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 6);
        LOGGER.debug("Compressed text data located at " + compressedTextDataOffset + ", blocks "
                + compressedTextDataBlocks);
        int compressedPokeStructOffset = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 8);
        int compressedPokeStructBlocks = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 10);
        LOGGER.debug("Compressed poke data located at " + compressedPokeStructOffset + ", blocks "
                + compressedPokeStructBlocks);
        int compressedPicFwAndCharsetOffset = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 12);
        int compressedPicFwAndCharsetBlocks = Util.readAsLittleEndian(data, V1Constants.CBLOCKS_OFFSET + 14);
        LOGGER.debug("Compressed PIC FW and Charset located at " + compressedPicFwAndCharsetOffset
                + ", blocks " + compressedPicFwAndCharsetBlocks);

        screen = uncompress(zis, compressedScreenOffset, compressedScreenBlocks);
        byte[] textData = uncompress(zis, compressedTextDataOffset, compressedTextDataBlocks);
        byte[] pokeData = uncompress(zis, compressedPokeStructOffset, compressedPokeStructBlocks);
        byte[] picFwAndCharset = uncompress(zis, compressedPicFwAndCharsetOffset, compressedPicFwAndCharsetBlocks);

        ByteArrayInputStream textDataStream = new ByteArrayInputStream(textData);
        extraRomMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorCpcConstants.GAMENAME_SIZE);
        togglePokesMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorCpcConstants.GAMENAME_SIZE);
        launchGameMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorCpcConstants.GAMENAME_SIZE);
        selectPokesMessage = Util.getNullTerminatedString(textDataStream, DandanatorCpcConstants.GAMENAME_SIZE);

        charSet = Arrays.copyOfRange(picFwAndCharset, 0, Constants.CHARSET_SIZE);

        //Poke data
        ByteArrayInputStream pokeDataStream = new ByteArrayInputStream(pokeData);
        for (int i = 0; i < gameCount; i++) {
            LOGGER.debug("Reading poke data for game " + i);
            GameMapperV1 mapper = gameMappers.get(i);
            mapper.setTrainerCount(pokeDataStream.read());
        }
        pokeDataStream.skip(DandanatorCpcConstants.MAX_GAMES - gameCount);
        pokeDataStream.skip(DandanatorCpcConstants.MAX_GAMES * 2);

        for (int i = 0; i < gameCount; i++) {
            GameMapperV1 mapper = gameMappers.get(i);
            int trainerCount = mapper.getTrainerCount();
            if (trainerCount > 0) {
                LOGGER.debug("Importing " + trainerCount + " trainers");
                for (int j = 0; j < trainerCount; j++) {
                    int pokeCount = pokeDataStream.read();
                    String trainerName = Util.getNullTerminatedString(pokeDataStream, 3, 24);
                    Optional<Trainer> trainer = mapper.getTrainerList().addTrainerNode(trainerName);
                    if (trainer.isPresent() && pokeCount > 0) {
                        LOGGER.debug("Importing " + pokeCount + " pokes on trainer " + trainerName);
                        for (int k = 0; k < pokeCount; k++) {
                            int address = Util.readAsLittleEndian(pokeDataStream);
                            int value = pokeDataStream.read();
                            trainer.map(t -> {
                                t.addPoke(address, value);
                                return true;
                            });
                        }
                    }
                }
            }
        }

        for (int i = 0; i < gameCount; i++) {
            GameMapperV1 mapper = gameMappers.get(i);
            GameChunk gameChunk = mapper.getGameChunk();
            if (gameChunk.getLength() == DandanatorCpcConstants.GAME_CHUNK_SIZE) {
                gameChunk.setData(copy(zis, gameChunk.getAddress(), gameChunk.getLength()));
            } else if (gameChunk.getLength() > 0) {
                gameChunk.setData(uncompress(zis, gameChunk.getAddress(), gameChunk.getLength()));
            }
        }
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
