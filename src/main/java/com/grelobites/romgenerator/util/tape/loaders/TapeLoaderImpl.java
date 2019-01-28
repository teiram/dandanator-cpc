package com.grelobites.romgenerator.util.tape.loaders;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.EmulatorConfiguration;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.emulator.BaseEmulator;
import com.grelobites.romgenerator.util.emulator.EmulationAbortedException;
import com.grelobites.romgenerator.util.emulator.peripheral.*;
import com.grelobites.romgenerator.util.emulator.resources.LoaderResources;
import com.grelobites.romgenerator.util.gameloader.loaders.SNAGameImageLoader;
import com.grelobites.romgenerator.util.tape.TapeFinishedException;
import com.grelobites.romgenerator.util.tape.TapeLoader;
import com.grelobites.romgenerator.util.tape.CdtTapePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class TapeLoaderImpl extends BaseEmulator implements TapeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoaderImpl.class);
    private static final int MAX_FRAMES_WITHOUT_TAPE_MOVEMENT = 5000;
    private static final int CPU_HZ = 4000000;
    private static final int FIRM_ZONE_START = 0xB100;
    private static final int FIRM_ZONE_END = 0xBE00;
    private final CdtTapePlayer tapePlayer;
    private static EmulatorConfiguration configuration = EmulatorConfiguration.getInstance();

    public TapeLoaderImpl(HardwareMode hardwareMode,
                          LoaderResources loaderResources) {
        super(hardwareMode, loaderResources);
        tapePlayer = new CdtTapePlayer(clock, ppi, false);
    }

    private static void saveGameAsSna(SnapshotGame game, int sequence) {
        final String name = String.format("test%d.sna", sequence);
        try (FileOutputStream fos = new FileOutputStream(name)) {
            LOGGER.debug("Saving current snapshot to {}", name);
            new SNAGameImageLoader().save(game, fos);
        } catch (IOException ioe) {
            LOGGER.error("Saving game to file", ioe);
        }
    }


    private boolean isTapeNearEndPosition() {
        return configuration.isTestTapeStopConditions() &&
                tapePlayer.getTapeLength() - tapePlayer.getCurrentTapePosition() <
                        configuration.getTapeRemainingBytes();
    }

    private boolean validLandingZone() {
        //return memory.isAddressInRam(z80.getRegPC()) &&
          //      !(z80.getRegPC() >= FIRM_ZONE_START && z80.getRegPC() < FIRM_ZONE_END);
        return true;
    }

    @Override
    public Game loadTape(InputStream tapeFile) throws IOException {
        long compensation = 0;
        tapePlayer.insert(tapeFile);
        loadSnapshot(loaderResources.snaLoader());

        //Define different listener to detect emulation stop conditions
        final GateArrayChangeListener paletteGateArrayChangeListener = (f, v) -> {
            if (configuration.isTestPaletteChanges() && f == GateArrayFunction.PALETTE_DATA_FN) {
                //Ignore border changes
                if (isTapeNearEndPosition() && (gateArray.getSelectedPen() & 0x10) == 0) {
                    LOGGER.debug("Aborting execution on palette change with tape near end");
                    executionAborted = true;
                    throw new EmulationAbortedException("Palette changed");
                }
            }
            return true;
        };

        final CrtcChangeListener crtcChangeListener = (o) -> {
            if (isTapeNearEndPosition()) {
                LOGGER.debug("Aborting execution on CRTC modification with tape near end");
                executionAborted = true;
                throw new EmulationAbortedException("CRTC access attempt");
            }
            return true;
        };
        final PsgFunctionListener psgFunctionListener = (f) -> {
            if (isTapeNearEndPosition()) {
                if (configuration.isTestPsgAccess() && f == PsgFunction.WRITE) {
                    LOGGER.debug("Aborting emulation on write to PSG with tape near end");
                    executionAborted = true;
                } else if (configuration.isTestKeyboardReads() &&
                        f == PsgFunction.READ &&
                        Ppi.KEYSCAN_PSG_REGISTER == ppi.getSelectedPsgRegister()) {
                    LOGGER.debug("Aborting emulation on read keyboard with tape near end");
                    executionAborted = true;
                }
            }
        };

        final MotorStateChangeListener motorStateChangeListener = (c) -> {
            if (!c) {
                LOGGER.debug("Stopping tape from listener with status {}", tapePlayer);
                tapePlayer.pause();
                if (configuration.isTestOnMotorStopped() && isTapeNearEndPosition()) {
                    LOGGER.debug("Aborting emulation with tape stopped near tape end");
                    executionAborted = true;
                }
            } else {
                LOGGER.debug("Restarting tape from listener with status {} ", tapePlayer);
                tapePlayer.resume();
            }
        };

        //Pressing enter key to continue with loading
        //and wait for the motor to become on
        pressKeyDuringFrames(20, KeyboardCode.KEY_ENTER);
        while (!ppi.isMotorOn()) {
            compensation = executeFrame(compensation);
        }
        LOGGER.info("Motor is on!");


        ppi.addMotorStateChangeListener(motorStateChangeListener);
        crtc.addChangeListener(crtcChangeListener);
        ppi.addPsgFunctionListener(psgFunctionListener);

        z80.setBreakpoint(0xbca1, true);
        z80.setBreakpoint(0xbc83, true);
        tapePlayer.play();

        int framesWithoutTapeMovement = 0;
        int currentTapePosition = tapePlayer.getCurrentTapePosition();
        boolean stopOnTapeStalled = false;
        gateArray.addChangeListener(paletteGateArrayChangeListener);
        try {
            while (!tapePlayer.isEOT() && !stopOnTapeStalled && !executionAborted) {
                compensation = executeFrame(this::isTapeNearEndPosition, compensation);
                if (tapePlayer.getCurrentTapePosition() == currentTapePosition) {
                    framesWithoutTapeMovement++;
                    if (framesWithoutTapeMovement >= MAX_FRAMES_WITHOUT_TAPE_MOVEMENT) {
                        LOGGER.debug("{} frames without tape movement. Stopping",
                                MAX_FRAMES_WITHOUT_TAPE_MOVEMENT);
                        stopOnTapeStalled = true;
                    }
                } else {
                    framesWithoutTapeMovement = 0;
                    currentTapePosition = tapePlayer.getCurrentTapePosition();
                }
            }
            tapePlayer.stop();
        } catch (TapeFinishedException tfe) {
            LOGGER.debug("Tape finished", tfe);
        } catch (EmulationAbortedException eae) {
            LOGGER.debug("Emulation aborted", eae);
        }

        ppi.removeMotorStateChangeListener(motorStateChangeListener);
        ppi.removePsgFunctionListener(psgFunctionListener);
        LOGGER.info("End of emulation with cpu status {}, tape: {}",
                z80.getZ80State(), tapePlayer);

        LOGGER.debug("Searching for proper landing zone");
        long deadline = clock.getTstates() + (2 * CPU_HZ); //Two seconds

        while (!validLandingZone()) {
            z80.execute();
            if (clock.getTstates() > deadline) {
                LOGGER.warn("Unable to find landing zone before deadline with status {}", z80.getZ80State());
                LOGGER.info("Simulating Space keypress");
                //Try to press SPACE to exit dead zone
                pressKeyDuringFrames(20, KeyboardCode.KEY_SPACE);
                deadline = clock.getTstates() + CPU_HZ; //One second
                while (!validLandingZone()) {
                    z80.execute();
                    if (clock.getTstates() > deadline) {
                        LOGGER.warn("Unable to exit banned zone before deadline even after keypress");
                        break;
                    }
                }
                break;
            }
        }
        gateArray.removeChangeListener(paletteGateArrayChangeListener);

        LOGGER.debug("Saving Snapshot with PC in {}, inRAM: {}",
                String.format("0x%04x", z80.getRegPC()),
                memory.isAddressInRam(z80.getRegPC()));
        return getSnapshotGame();
    }

    @Override
    public void poke8(int address, int value) {
        super.poke8(address, value);
        if (isTapeNearEndPosition() && crtc.isVideoAddress(address)) {
            LOGGER.debug("Aborting execution on write to VRAM with tape at end");
            executionAborted = true;
            throw new EmulationAbortedException("Write to VRAM");
        }
    }

    @Override
    public void poke16(int address, int word) {
        super.poke16(address, word);
        if (isTapeNearEndPosition() && crtc.isVideoAddress(address)) {
            LOGGER.debug("Aborting execution on write to VRAM with tape at end");
            executionAborted = true;
            throw new EmulationAbortedException("Write to VRAM");
        }

    }

    @Override
    public void breakpoint() {
        super.breakpoint();
        if (z80.getRegPC() == 0xbca1) {
            LOGGER.debug("CAS READ Invoked with status {}", z80.getZ80State());
        } else if (z80.getRegPC() == 0xbc83) {
            LOGGER.debug("CAS IN DIRECT invoked with status {}", z80.getZ80State());
        }
    }
}
