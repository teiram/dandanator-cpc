package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.RamGameCompressor;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SnapshotGame extends BaseGame implements RamGame {
	private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotGame.class);
    private static final int[][] MEMORY_CONFIGURATIONS = new int[][] {
            {0, 1, 2, 3},
            {0, 1, 2, 7},
            {4, 5, 6, 7},
            {0, 3, 2, 7},
            {0, 4, 2, 3},
            {0, 5, 2, 3},
            {0, 6, 2, 3},
            {0, 7, 2, 3}
    };

	private static ExecutorService compressingService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                    r -> {
                        Thread t = new Thread(r, "Compressing Service Thread");
                        t.setDaemon(true);
                        return t;
                    });

	private BooleanProperty holdScreen;
	private BooleanProperty compressed;
	private Image screenshot;
    private GameHeader gameHeader;
	private TrainerList trainerList;
    private List<byte[]> compressedData;
    private IntegerProperty compressedSize;
    private HardwareMode hardwareMode;
    private int currentRasterInterrupt;

    private void try64KReduction() {
        if (gameType == GameType.RAM128 && data.size() == 8) {
            for (int i = 4; i < 8; i++) {
                if (data.get(i) != null) {
                    return;
                }
            }
            //All high memory slots are zeroed
            LOGGER.debug("Reducing game to 64K");
            data = data.subList(0, 4);
            gameType = GameType.RAM64;
        }
    }

    public SnapshotGame(GameType gameType, List<byte[]> data) {
        super(gameType, data);
		holdScreen = new SimpleBooleanProperty();
        compressed = new SimpleBooleanProperty(true);
        compressedSize = new SimpleIntegerProperty(0);
        try64KReduction();
    }

    public GameHeader getGameHeader() {
        return gameHeader;
    }

    public void setGameHeader(GameHeader gameHeader) {
        this.gameHeader = gameHeader;
        //Adjust memoryDumpSize since we can have automatically reduced it on creation
        if (gameHeader.getMemoryDumpSize() == 128 && gameType == GameType.RAM64) {
            gameHeader.setMemoryDumpSize(64);
        }
    }

    public boolean getCompressed() {
		return compressed.get();
	}

	public void setCompressed(boolean compressed) {
		this.compressed.set(compressed);
	}

	public BooleanProperty compressedProperty() {
		return compressed;
	}

	public boolean getHoldScreen() {
		return holdScreen.get();
	}
	
	public void setHoldScreen(boolean holdScreen) {
		this.holdScreen.set(holdScreen);
	}
	
	public BooleanProperty holdScreenProperty() {
		return holdScreen;
	}

	public int getScreenSlot() {
        int index = getScreenPage() / Constants.SLOT_SIZE;
        LOGGER.debug("Index in peripheral configuration is {}, current RAM configuration is {}",
                index, gameHeader.getCurrentRamConfiguration());
        //Upper bits in RAM configuration sets the type of RAM extension, ignore them
        /*
                        RAM Expansion	Bits
                                        7	6	5	4	3	2	1	0
                       CPC6128 (note 1)	1	1	-	-	-	s2	s1	s0
        Dk'tronics 256K Silicon Disk	1	1	1	b1	b0	s2	s1	s0
        Key:

        "-" - this bit is ignored. The value of this bit is not important.
        "0" - this bit must be set to "0"
        "1" - this bit must be set to "1"
        "b0,b1,b2" - this bit is used to define the logical 64k block that the ram configuration uses
        "s0,s1,s2" - this bit is used to define the ram configuration
         */
        return MEMORY_CONFIGURATIONS[gameHeader.getCurrentRamConfiguration() & 0x07][index];
    }

    public void setCompressedData(List<byte[]> compressedData) {
        this.compressedData = compressedData;
    }

    private int getScreenMode() {
        return gameHeader.getGateArrayMultiConfiguration() & 0x03;
    }

    private int getScreenOffset() {
        return (((gameHeader.getCrtcRegisterData()
                [CrtcRegisters.DISPLAY_START_ADDR_HI] & 0x3) << 8) |
                (gameHeader.getCrtcRegisterData()
                        [CrtcRegisters.DISPLAY_START_ADDR_LO] & 0xff)) << 4;
    }

    private int getScreenPage() {
        int screenPage = (gameHeader.getCrtcRegisterData()
                [CrtcRegisters.DISPLAY_START_ADDR_HI] & 0x30) << 10;
        LOGGER.debug("Screen page is {}", String.format("%04x", screenPage));
        return screenPage;
    }

    public Image getScreenshot() {
		if (screenshot == null) {
		    int screenSlot = getScreenSlot();
		    int screenOffset = getScreenOffset();
		    LOGGER.debug("Getting screenshot from slot {} with offset {}",
                    screenSlot, screenOffset);
			try {
				screenshot = ImageUtil
						.scrLoader(ImageUtil.newScreenshot(),
								getScreenMode(),
								getSlot(screenSlot),
								CrtcDisplayData.newBuilder()
                                .withDisplayOffset(screenOffset)
                                .withVisibleHeight(gameHeader.getCrtcRegisterData()[CrtcRegisters.VISIBLE_HEIGHT])
                                .withVisibleWidth(gameHeader.getCrtcRegisterData()[CrtcRegisters.VISIBLE_WIDTH]).build(),
                                gameHeader.getGateArrayCurrentPalette());
			} catch (Exception e) {
				LOGGER.error("Loading screenshot", e);
			}
		}
		return screenshot;
	}
	
	public void setScreenshot(Image screenshot) {
		this.screenshot = screenshot;
	}

	public TrainerList getTrainerList() {
        if (trainerList == null) {
            trainerList = new TrainerList(this);
        }
        return trainerList;
    }

	public void setTrainerList(TrainerList trainerList) {
		this.trainerList = trainerList;
        trainerList.setOwner(this);
	}

	public void addTrainer(String pokeName) {
		getTrainerList().addTrainerNode(pokeName);
	}

    public boolean hasPokes() {
        return trainerList != null && trainerList.getChildren().size() > 0;
    }

	@Override
	public boolean isCompressible() {
		return true;
	}

	@Override
    public Observable[] getObservable() {
	    return new Observable[]{name, holdScreen, compressed, compressedSize, autoboot};
    }

    private class CompressingContext {
        public final byte[] data;
        public final  int slot;
        public byte[] compressedData;
        private final CountDownLatch counter;
        public CompressingContext(CountDownLatch counter, byte[] data, int slot) {
            this.data = data;
            this.slot = slot;
            this.counter = counter;
        }
    }

	public List<byte[]> getCompressedData(RamGameCompressor compressor) throws IOException {
	    if (compressedData == null) {
            CountDownLatch counter =  new CountDownLatch(getSlotCount());
            ArrayList<CompressingContext> compressingTasks = new ArrayList<>();
            for (int i = 0; i < getSlotCount(); i++) {
                final CompressingContext context = new CompressingContext(counter, getSlot(i), i);
                compressingTasks.add(context);
                compressingService.submit(() -> {
                    if (!isSlotZeroed(context.slot)) {
                        context.compressedData = compressor.compressSlot(context.slot, context.data);
                    }
                    context.counter.countDown();
                });
            }
            try {
                counter.await();
            } catch (InterruptedException ie) {
                LOGGER.warn("Compressing thread interrupted", ie);
                throw new IOException("Compressing thread interrupted", ie);
            }
            compressedData = new ArrayList<>();
            for (CompressingContext context : compressingTasks) {
                compressedData.add(context.compressedData);
            }
        }
        return compressedData;
    }

    public int getCompressedSize() throws IOException {
        return getCompressedSize(null);
    }

    public int getCompressedSize(RamGameCompressor compressor) throws IOException {
        return getCompressedSize(compressor, false);
    }

    public int getCompressedSize(RamGameCompressor compressor, boolean forced) throws IOException {
        if (compressedSize.get() == 0 || forced) {
            if (compressor != null) {
                int size = 0;
                for (byte[] compressedSlot : getCompressedData(compressor)) {
                    size += compressedSlot != null ? compressedSlot.length : 0;
                }
                compressedSize.set(size);
            } else {
                throw new IllegalStateException("Compressed size not calculated yet");
            }
        }
        return compressedSize.get();
    }

    public IntegerProperty compressedSizeProperty() {
        return compressedSize;
    }

    public int getSlotForMappedRam(int offset) {
        int bankPos = offset / Constants.SLOT_SIZE;
        return MEMORY_CONFIGURATIONS[gameHeader
                .getCurrentRamConfiguration() & 0x07][bankPos];
    }

    public HardwareMode getHardwareMode() {
        return hardwareMode;
    }

    public void setHardwareMode(HardwareMode hardwareMode) {
        LOGGER.debug("Setting hardware mode " + hardwareMode);
        this.hardwareMode = hardwareMode;
    }

    public void updateScreen(byte[] screen) throws IOException {
        int slot = getScreenSlot();
        byte[] slotData = ByteBuffer.allocate(Constants.SLOT_SIZE)
                .put(screen, 0, Constants.CPC_SCREEN_SIZE)
                .put(getSlot(slot), Constants.CPC_SCREEN_SIZE,
                        Constants.SLOT_SIZE - Constants.CPC_SCREEN_SIZE)
                .array();
        data.set(slot, slotData);
        compressedData.set(slot, Configuration.getInstance().getRamGameCompressor()
            .compressSlot(slot, slotData));
        getCompressedSize(Configuration.getInstance().getRamGameCompressor(), true);
        screenshot = null;
    }

    public int getCurrentRasterInterrupt() {
        return currentRasterInterrupt;
    }

    public void setCurrentRasterInterrupt(int currentRasterInterrupt) {
        this.currentRasterInterrupt = currentRasterInterrupt;
    }

    @Override
    public String toString() {
        return "SnapshotGame{" +
                "gameType=" + getType() +
                ", name=" + getName() +
                ", autoboot=" + autoboot +
                ", holdScreen=" + holdScreen.get() +
                ", compressed=" + compressed.get() +
                ", hardwareMode=" + hardwareMode +
                ", gameHeader=" + gameHeader +
                '}';
    }
}
