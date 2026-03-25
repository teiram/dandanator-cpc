package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.zx7.Zx7InputStream;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Optional;

public class MLAGame extends BaseGame implements RamGame {
    private static final Logger LOGGER = LoggerFactory.getLogger(MLAGame.class);

    private HardwareMode hardwareMode;
    private Image screenshot;
    private MLAInfo mlaInfo;
    private IntegerProperty size;

    private TrainerList trainerList;

    public MLAGame(MLAInfo mlaInfo, byte[] data) {
        super(mlaInfo.getGameType(), Collections.singletonList(data));
        this.mlaInfo = mlaInfo;
        this.size = new SimpleIntegerProperty(super.getSize());
        hardwareMode = mlaInfo.getHardwareMode();
        if (mlaInfo.getMlaPokeAddress() != 0xffff) {
            this.trainerList = parseTrainers(mlaInfo, data);
        }
    }

    @Override
    public boolean isCompressible() {
        return false;
    }

    public MLAInfo initializeMlaInfo() {
        if (mlaInfo == null) {
            mlaInfo = MLAInfo.fromGameByteArray(data)
                    .orElseThrow(
                            ()-> new IllegalArgumentException("Unable to extract MLA data from file"));
        }
        return mlaInfo;
    }

    public MLAInfo getMLAInfo() {
        return mlaInfo;
    }

    public Image getScreenshot() {
        if (screenshot == null) {
            try {
                if (mlaInfo.getCompressedScreenOffset() != 0) {
                    LOGGER.debug("Getting screenshot from data with length {} on offset {}", data.get(0).length, mlaInfo.getCompressedScreenOffset());
                    byte[] screenData = Util.fromInputStream(
                            new Zx7InputStream(
                                new ByteArrayInputStream(
                                    data.get(0),
                                    mlaInfo.getCompressedScreenOffset(),
                                    mlaInfo.getCompressedScreenSize()
                            )));
                    screenshot = ImageUtil
                            .scrLoader(ImageUtil.newScreenshot(),
                                    MLAInfo.MLA_DEFAULT_SCREENMODE,
                                    screenData,
                                    CrtcDisplayData.DEFAULT_VALUE,
                                    ImageUtil.embeddedPaletteMLA(screenData));


                }
            } catch (Exception e) {
                LOGGER.error("Loading screenshot", e);
            }
        }
        return screenshot;
    }

    @Override
    public HardwareMode getHardwareMode() {
        return hardwareMode;
    }

    @Override
    public void setHardwareMode(HardwareMode hardwareMode) {
        this.hardwareMode = hardwareMode;
    }

    @Override
    public void setScreenshot(Image screenshot) {
        this.screenshot = screenshot;
    }

    public MLAInfo getMlaInfo() {
        return mlaInfo;
    }

    public void setMlaInfo(MLAInfo mlaInfo) {
        this.mlaInfo = mlaInfo;
    }

    public void relocate(int slot) {
        LOGGER.debug("Relocating MLA game " + this + " with " + getSlotCount()
                + " slots to slot " + slot + ". Current base slot is "
                + mlaInfo.getBaseSlot());

        int headerSlot = mlaInfo.getHeaderSlot();
        byte[] headerSlotData = getSlot(headerSlot);
        headerSlotData[MLAInfo.MLA_HEADER_OFFSET] = (byte) slot;

        int tableOffset = mlaInfo.getTableOffset();
        byte[] slotData = getSlot(0);
        for (int i = 0; i < mlaInfo.getTableRows(); i++) {
            int offset = tableOffset + mlaInfo.getRowSlotOffset();
            int correctedValue = (slotData[offset] & 0x7F) - mlaInfo.getBaseSlot();
            int newValue = (correctedValue + slot) | (slotData[offset] & 0x80);
            LOGGER.debug("Patching position 0x"
                    + Integer.toHexString(offset & 0xffff) + " from value 0x"
                    + Integer.toHexString(slotData[offset] & 0xff)
                    + " to 0x" + Integer.toHexString(newValue & 0xff));
            slotData[offset] = (byte) newValue;
            tableOffset += mlaInfo.getTableRowSize();
        }
        mlaInfo.setBaseSlot(slot);
    }

    @Override
    public int getSize() {
        return size.get();
    }

    public IntegerProperty sizeProperty() {
        return size;
    }

    public void setSize(int size) {
        this.size.set(size);
    }

    private TrainerList parseTrainers(MLAInfo mlaInfo, byte[] gameData) {
        TrainerList trainerList = new TrainerList(this);
        ByteBuffer byteBuffer = ByteBuffer.wrap(gameData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.position(mlaInfo.getMlaPokeAddress());
        int numTrainers = byteBuffer.get() & 0xff;
        for (int i = 0; i < numTrainers; i++) {
            int numPokes = byteBuffer.get() & 0xff;
            String name = Util.getNullTerminatedString(byteBuffer, 24);
            Optional<Trainer> trainerOpt = trainerList.addTrainerNode(name);
            for (int j = 0; j < numPokes; j++) {
                trainerOpt.ifPresent(t -> t.addPoke(byteBuffer.getShort() & 0xffff,
                        byteBuffer.get() & 0xff));
            }
        }
        return trainerList;
    }

    public TrainerList getTrainerList() {
        return trainerList;
    }

    public void setTrainerList(TrainerList trainerList) {
        this.trainerList = trainerList;
    }

    @Override
    public String toString() {
        return "MLAGame {" +
                "name=" + name +
                ", hardwareMode=" + hardwareMode +
                ", mlaInfo=" + mlaInfo +
                '}';
    }

}
