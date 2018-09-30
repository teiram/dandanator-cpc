package com.grelobites.romgenerator.util.tape.loaders;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.emulator.BaseEmulator;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArrayChangeListener;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArrayFunction;
import com.grelobites.romgenerator.util.emulator.resources.LoaderResources;
import com.grelobites.romgenerator.util.tape.TapeFinishedException;
import com.grelobites.romgenerator.util.tape.TapeLoader;
import com.grelobites.romgenerator.util.emulator.peripheral.KeyboardCode;
import com.grelobites.romgenerator.util.tape.CdtTapePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class TapeLoaderImpl extends BaseEmulator implements TapeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoaderImpl.class);
    private static final int MAX_FRAMES_WITHOUT_TAPE_MOVEMENT = 5000;
    private final CdtTapePlayer tapePlayer;
    private SnapshotGame currentSnapshot;
    private int tapeLastSavePosition = 0;
    private int framesWithoutTapeMovement = 0;

    public TapeLoaderImpl(HardwareMode hardwareMode,
                          LoaderResources loaderResources) {
        super(hardwareMode, loaderResources);
        tapePlayer = new CdtTapePlayer(clock, ppi, true);
    }

    @Override
    public Game loadTape(InputStream tapeFile) throws IOException {
        long compensation = 0;
        tapePlayer.insert(tapeFile);
        loadSnapshot(loaderResources.snaLoader());
        //Pressing enter key to continue with loading
        final GateArrayChangeListener paletteGateArrayChangeListener = (f, v) -> {
            if (f == GateArrayFunction.PALETTE_DATA_FN) {
                //Ignore border changes
                if ((gateArray.getSelectedPen() & 0x10) == 0) {
                    if (isTapeNearEndPosition()) {
                        LOGGER.debug("Aborting execution on palette change with tape at end");
                        executionAborted = true;
                        return false;
                    }
                }
            } else if (f == GateArrayFunction.RAM_BANKING_FN) {
                LOGGER.debug("Changing RAM Banking to {}", v);
            }
            return true;
        };
        pressKeyDuringFrames(20, KeyboardCode.KEY_ENTER);
        while (!ppi.isMotorOn()) {
            compensation = executeFrame(compensation);
        }
        ppi.addMotorStateChangeListener((c) -> {
            if (!c) {
                LOGGER.debug("Stopping tape from listener with status {}", tapePlayer.getStatus());
                currentSnapshot = getSnapshotGame();
                tapeLastSavePosition = tapePlayer.getCurrentTapePosition();
                tapePlayer.pause();
                if (tapePlayer.isInLastBlock()) {
                    LOGGER.debug("Aborting emulation with tape stopped in last block");
                    executionAborted = true;
                }
            } else {
                LOGGER.debug("Restarting tape from listener with status {} ", tapePlayer.getStatus());
                tapePlayer.resume();
                framesWithoutTapeMovement = 0;
            }
        });

        LOGGER.info("Motor is on!");
        tapePlayer.play();
        framesWithoutTapeMovement = 0;
        boolean stopOnTapeStalled = false;
        gateArray.addChangeListener(paletteGateArrayChangeListener);
        try {
            while (!tapePlayer.isEOT() && !stopOnTapeStalled && !executionAborted) {
                compensation = executeFrame(compensation);
                if (tapePlayer.getCurrentTapePosition() == tapeLastSavePosition) {
                    framesWithoutTapeMovement++;
                    if (framesWithoutTapeMovement >= MAX_FRAMES_WITHOUT_TAPE_MOVEMENT) {
                        LOGGER.debug("{} frames without tape movement. Stopping",
                                MAX_FRAMES_WITHOUT_TAPE_MOVEMENT);
                        stopOnTapeStalled = true;
                    }
                }
            }
            tapePlayer.stop();
        } catch (TapeFinishedException tfe) {
            LOGGER.debug("Tape finished with cpu status {}, tape: {}",
                    z80.getZ80State(), tapePlayer, tfe);
        }

        gateArray.removeChangeListener(paletteGateArrayChangeListener);
        LOGGER.info("Tape finished at {}, tapeLastSavePosition was {}",
                tapePlayer.getCurrentTapePosition(), tapeLastSavePosition);


        /*
        long deadline = clock.getTstates() + (5 * CPU_HZ); //Five seconds

        while (!memory.isAddressInRam(z80.getRegPC())) {
            z80.execute();
            if (clock.getTstates() > deadline) {
                break;
            }
        }
        */

        if (tapePlayer.getCurrentTapePosition() > tapeLastSavePosition) {
            LOGGER.debug("Saving Snapshot with PC in {}, inRAM: {}",
                    String.format("0x%04x", z80.getRegPC()),
                    memory.isAddressInRam(z80.getRegPC()));
            currentSnapshot = getSnapshotGame();
        }

        return currentSnapshot;
    }

    private boolean isTapeNearEndPosition() {
        return tapePlayer.getTapeLength() - tapePlayer.getCurrentTapePosition() < 2;
    }

    @Override
    public void poke8(int address, int value) {
        super.poke8(address, value);
        if (crtc.isVideoAddress(address) && isTapeNearEndPosition()) {
            LOGGER.debug("Aborting execution on write to VRAM with tape at end");
            executionAborted = true;
        }
    }

    @Override
    public void poke16(int address, int word) {
        super.poke16(address, word);
        if (crtc.isVideoAddress(address) && isTapeNearEndPosition()) {
            LOGGER.debug("Aborting execution on write to VRAM with tape at end");
            executionAborted = true;
        }

    }

    public long executeTstates(long tStates) {
        long limit = clock.getTstates() + tStates;
        while (clock.getTstates() < limit && !executionAborted) {
            z80.execute();
            if (tapePlayer.getCurrentTapePosition() == tapePlayer.getTapeLength()) {
                executionAborted = true;
            }
        }
        return clock.getTstates() - limit;
    }

}
