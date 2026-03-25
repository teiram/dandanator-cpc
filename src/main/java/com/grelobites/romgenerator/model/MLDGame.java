package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
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
import java.util.List;
import java.util.Optional;

public class MLDGame extends BaseGame implements RamGame {
    private static final Logger LOGGER = LoggerFactory.getLogger(MLDGame.class);

    private HardwareMode hardwareMode;
    private Image screenshot;
    private MLDInfo mldInfo;
    private IntegerProperty size;

    private TrainerList trainerList;

    public MLDGame(MLDInfo mldInfo, byte[] data) {
        super(mldInfo.getGameType(), Collections.singletonList(data));
        this.mldInfo = mldInfo;
        this.size = new SimpleIntegerProperty(super.getSize());
        hardwareMode = mldInfo.getHardwareMode();
        if (mldInfo.getMldPokeAddress() != 0xffff) {
            this.trainerList = parseTrainers(mldInfo, data);
        }
    }

    @Override
    public boolean isCompressible() {
        return false;
    }

    public MLDInfo initializeMldInfo() {
        if (mldInfo == null) {
            mldInfo = MLDInfo.fromGameByteArray(data)
                    .orElseThrow(
                            ()-> new IllegalArgumentException("Unable to extract MLD data from file"));
        }
        return mldInfo;
    }

    public MLDInfo getMLDInfo() {
        return mldInfo;
    }

    public Image getScreenshot() {
        if (screenshot == null) {
            try {
                if (mldInfo.getCompressedScreenOffset() != 0) {
                    LOGGER.debug("Getting screenshot from data with length {} on offset {}", data.get(0).length, mldInfo.getCompressedScreenOffset());
                    byte[] screenData = Util.fromInputStream(
                            new Zx7InputStream(
                                new ByteArrayInputStream(
                                    data.get(0),
                                    mldInfo.getCompressedScreenOffset(),
                                    mldInfo.getCompressedScreenSize()
                            )));
                    screenshot = ImageUtil
                            .scrLoader(ImageUtil.newScreenshot(),
                                    MLDInfo.MLD_DEFAULT_SCREENMODE,
                                    screenData,
                                    CrtcDisplayData.DEFAULT_VALUE,
                                    ImageUtil.embeddedPaletteMLD(screenData));


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

    public MLDInfo getMldInfo() {
        return mldInfo;
    }

    public void setMldInfo(MLDInfo mldInfo) {
        this.mldInfo = mldInfo;
    }

    public void relocate(int slot) {
        LOGGER.debug("Relocating MLD game " + this + " with " + getSlotCount()
                + " slots to slot " + slot + ". Current base slot is "
                + mldInfo.getBaseSlot());

        int headerSlot = mldInfo.getHeaderSlot();
        byte[] headerSlotData = getSlot(headerSlot);
        headerSlotData[MLDInfo.MLD_HEADER_OFFSET] = (byte) slot;

        int tableOffset = mldInfo.getTableOffset();
        byte[] slotData = getSlot(0);
        for (int i = 0; i < mldInfo.getTableRows(); i++) {
            int offset = tableOffset + mldInfo.getRowSlotOffset();
            int correctedValue = (slotData[offset] & 0x7F) - mldInfo.getBaseSlot();
            int newValue = (correctedValue + slot) | (slotData[offset] & 0x80);
            LOGGER.debug("Patching position 0x"
                    + Integer.toHexString(offset & 0xffff) + " from value 0x"
                    + Integer.toHexString(slotData[offset] & 0xff)
                    + " to 0x" + Integer.toHexString(newValue & 0xff));
            slotData[offset] = (byte) newValue;
            tableOffset += mldInfo.getTableRowSize();
        }
        mldInfo.setBaseSlot(slot);
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

    private TrainerList parseTrainers(MLDInfo mldInfo, byte[] gameData) {
        TrainerList trainerList = new TrainerList(this);
        ByteBuffer byteBuffer = ByteBuffer.wrap(gameData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.position(mldInfo.getMldPokeAddress());
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
        return "MLDGame{" +
                " name=" + name +
                ", hardwareMode=" + hardwareMode +
                ", mldInfo=" + mldInfo +
                '}';
    }

}
