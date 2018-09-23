package com.grelobites.romgenerator.util.tape.loaders;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.emulator.Clock;
import com.grelobites.romgenerator.util.Counter;
import com.grelobites.romgenerator.util.emulator.ClockTimeout;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArrayChangeListener;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArrayFunction;
import com.grelobites.romgenerator.util.emulator.resources.LoaderResources;
import com.grelobites.romgenerator.util.tape.TapeFinishedException;
import com.grelobites.romgenerator.util.tape.TapeLoader;
import com.grelobites.romgenerator.util.emulator.Z80;
import com.grelobites.romgenerator.util.emulator.Z80State;
import com.grelobites.romgenerator.util.emulator.Z80operations;
import com.grelobites.romgenerator.util.emulator.peripheral.CpcMemory;
import com.grelobites.romgenerator.util.emulator.peripheral.Crtc;
import com.grelobites.romgenerator.util.emulator.peripheral.CrtcType;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArray;
import com.grelobites.romgenerator.util.emulator.peripheral.KeyboardCode;
import com.grelobites.romgenerator.util.emulator.peripheral.Ppi;
import com.grelobites.romgenerator.util.tape.CdtTapePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class TapeLoaderImpl implements TapeLoader, Z80operations {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoaderImpl.class);
    private static final int CPU_HZ = 4000000;
    private static final int VSYNC_HZ = 50;
    private static final int TSTATES_PER_US = 4;
    private static final int FRAME_TSTATES = 19968 * TSTATES_PER_US;
    private static final int HSYNC_TSTATES = 64 * TSTATES_PER_US; // 64 microseconds
    private static final int LINES_PER_INTERRUPT = 52;
    private static final int INTERRUPT_TSTATES = HSYNC_TSTATES * LINES_PER_INTERRUPT;
    private static final int HSYNC_32_DELAY = HSYNC_TSTATES * 32;
    private static final int MAX_FRAMES_WITHOUT_TAPE_MOVEMENT = 5000;
    private final LoaderResources loaderResources;
    private final GateArray gateArray;
    private final Z80 z80;
    private final Clock clock;
    private final CdtTapePlayer tapePlayer;
    private final Crtc crtc;
    private final Ppi ppi;
    private final CpcMemory memory;
    private final HardwareMode hardwareMode;
    private SnapshotGame currentSnapshot;
    private int tapeLastSavePosition = 0;
    private int upperRomNumber = 0;
    private int framesWithoutTapeMovement = 0;
    private boolean executionAborted = false;

    private void loadRoms() throws IOException {
        memory.loadLowRom(loaderResources.lowRom());
        memory.loadHighRom(loaderResources.highRom());
    }

    public TapeLoaderImpl(HardwareMode hardwareMode,
                          LoaderResources loaderResources) {
        this.loaderResources = loaderResources;
        clock = new Clock();
        this.hardwareMode = hardwareMode;
        z80 = new Z80(clock, this);
        ppi = new Ppi();
        gateArray = GateArray.newBuilder()
                .withCpc464DefaultValues().build();
        crtc = new Crtc(CrtcType.CRTC_TYPE_0);
        memory = new CpcMemory(gateArray);
        tapePlayer = new CdtTapePlayer(clock, ppi, true);
        try {
            loadRoms();
        } catch (IOException ioe) {
            LOGGER.error("Loading ROM resources", ioe);
            throw new IllegalArgumentException("Invalid ROM Resources", ioe);
        }
    }

    protected static Z80.IntMode fromOrdinal(int mode) {
        switch (mode) {
            case 0:
                return Z80.IntMode.IM0;
            case 1:
                return Z80.IntMode.IM1;
            case 2:
                return Z80.IntMode.IM2;
        }
        throw new IllegalArgumentException("Invalid Interrupt mode: " + mode);
    }

    private Z80State getZ80State(GameHeader header) {
        Z80State z80State = new Z80State();
        z80State.setRegAF(header.getAfRegister());
        z80State.setRegBC(header.getBcRegister());
        z80State.setRegDE(header.getDeRegister());
        z80State.setRegHL(header.getHlRegister());
        z80State.setRegIX(header.getIxRegister());
        z80State.setRegIY(header.getIyRegister());
        z80State.setRegI(header.getiRegister());
        z80State.setRegR(header.getrRegister());
        z80State.setRegPC(header.getPc());
        z80State.setRegSP(header.getSp());
        z80State.setIFF1(header.getIff0() != 0);
        z80State.setIM(fromOrdinal(header.getInterruptMode()));
        z80State.setRegAFx(header.getAltAfRegister());
        z80State.setRegBCx(header.getAltBcRegister());
        z80State.setRegDEx(header.getAltDeRegister());
        z80State.setRegHLx(header.getAltHlRegister());
        return z80State;
    }

    private void loadLoaderSnapshot() throws IOException {
        SnapshotGame game = loaderResources.snaLoader();

        GameHeader header = game.getGameHeader();
        z80.setZ80State(getZ80State(game.getGameHeader()));

        gateArray.setSelectedPen(header.getGateArraySelectedPen());
        gateArray.setPalette(header.getGateArrayCurrentPalette());
        gateArray.setScreenModeAndRomConfigurationRegister(
                header.getGateArrayMultiConfiguration());
        gateArray.setRamBankingRegister(
                header.getCurrentRamConfiguration());

        crtc.setSelectedRegister(header.getCrtcSelectedRegisterIndex());
        crtc.setCrtcRegisterData(header.getCrtcRegisterData());

        upperRomNumber = header.getCurrentRomSelection();

        ppi.setPortACurrentValue(header.getPpiPortA());
        ppi.setPortBCurrentValue(header.getPpiPortB());
        ppi.setPortCCurrentValue(header.getPpiPortC());
        ppi.setControlCurrentValue(header.getPpiControlPort());
        ppi.setSelectedPsgRegister(header.getPsgSelectedRegisterIndex());
        ppi.setPsgRegisterData(header.getPsgRegisterData());

        LOGGER.debug("Setting memory from Snapshot with {} slots",
                game.getSlotCount());
        for (int i = 0; i < game.getSlotCount(); i++) {
            memory.loadRamBank(game.getSlot(i), i);
        }
    }

    private GameHeader getGameHeader() {
        GameHeader header = new GameHeader();
        Z80State z80State = z80.getZ80State();

        header.setAfRegister(z80State.getRegAF());
        header.setBcRegister(z80State.getRegBC());
        header.setDeRegister(z80State.getRegDE());
        header.setHlRegister(z80State.getRegHL());
        header.setIxRegister(z80State.getRegIX());
        header.setIyRegister(z80State.getRegIY());
        header.setiRegister(z80State.getRegI());
        header.setrRegister(z80State.getRegR());
        header.setPc(z80State.getRegPC());
        header.setSp(z80State.getRegSP());
        header.setIff0(z80State.isIFF1() ? 1 : 0); //Check
        header.setInterruptMode(z80State.getIM().ordinal());
        header.setAltAfRegister(z80State.getRegAFx());
        header.setAltBcRegister(z80State.getRegBCx());
        header.setAltDeRegister(z80State.getRegDEx());
        header.setAltHlRegister(z80State.getRegHLx());

        header.setGateArraySelectedPen(gateArray.getSelectedPen());
        header.setGateArrayCurrentPalette(gateArray.getPalette());
        header.setGateArrayMultiConfiguration(gateArray
                .getScreenModeAndRomConfigurationRegister()); //Check

        header.setCurrentRamConfiguration(gateArray.getRamBankingRegister()); //Check

        header.setCrtcSelectedRegisterIndex(crtc.getSelectedRegister());
        header.setCrtcRegisterData(crtc.getCrtcRegisterData());

        header.setCurrentRomSelection(upperRomNumber);

        header.setPpiPortA(ppi.getPortACurrentValue());
        header.setPpiPortB(ppi.getPortBCurrentValue());
        header.setPpiPortC(ppi.getPortCCurrentValue());
        header.setPpiControlPort(ppi.getControlCurrentValue());

        header.setPsgSelectedRegisterIndex(ppi.getSelectedPsgRegister());
        header.setPsgRegisterData(ppi.getPsgRegisterData());

        header.setMemoryDumpSize(gateArray.hasRamBanking() ? 64 : 128);
        header.setCpcType(hardwareMode.snaValue());
        LOGGER.debug("Game header calculated as {}", header);
        return header;
    }

    private SnapshotGame toSnapshotGame() {
        SnapshotGame game = new SnapshotGame(memory.getRamSize() == 65536 ?
                GameType.RAM64 : GameType.RAM128,
                memory.toByteArrayList());
        game.setGameHeader(getGameHeader());
        return game;
    }

    private void pressKeyDuringFrames(KeyboardCode key, int frames) {
        ppi.pressKey(key);
        long compensation = 0;
        for (int i = 0; i < frames; i++) {
            compensation = executeFrame(compensation);
        }
        ppi.releaseKey(key);
    }

    @Override
    public Game loadTape(InputStream tapeFile) throws IOException {
        long compensation = 0;
        tapePlayer.insert(tapeFile);
        loadLoaderSnapshot();
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
        pressKeyDuringFrames(KeyboardCode.KEY_ENTER, 20);
        while (!ppi.isMotorOn()) {
            compensation = executeFrame(compensation);
        }
        ppi.addMotorStateChangeListener((c) -> {
            if (!c) {
                LOGGER.debug("Stopping tape from listener with status {}", tapePlayer.getStatus());
                currentSnapshot = toSnapshotGame();
                tapeLastSavePosition = tapePlayer.getTapePos();
                tapePlayer.stop();
            } else {
                LOGGER.debug("Restarting tape from listener with status {} ", tapePlayer.getStatus());
                tapePlayer.play();
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
                if (tapePlayer.getTapePos() == tapeLastSavePosition) {
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
                tapePlayer.getTapePos(), tapeLastSavePosition);


        /*
        long deadline = clock.getTstates() + (5 * CPU_HZ); //Five seconds

        while (!memory.isAddressInRam(z80.getRegPC())) {
            z80.execute();
            if (clock.getTstates() > deadline) {
                break;
            }
        }
        */

        if (tapePlayer.getTapePos() > tapeLastSavePosition) {
            LOGGER.debug("Saving Snapshot with PC in {}, inRAM: {}",
                    String.format("0x%04x", z80.getRegPC()),
                    memory.isAddressInRam(z80.getRegPC()));
            currentSnapshot = toSnapshotGame();
        }

        return currentSnapshot;
    }

    private boolean isTapeNearEndPosition() {
        return tapePlayer.getTapeLength() - tapePlayer.getTapePos() < 2;
    }

    @Override
    public int fetchOpcode(int address) {
        return peek8(address);
    }

    @Override
    public int peek8(int address) {
        clock.addTstates(4);
        return memory.peek8(address);
    }

    @Override
    public void poke8(int address, int value) {
        clock.addTstates(4);
        memory.poke8(address, value);
        if (crtc.isVideoAddress(address) && isTapeNearEndPosition()) {
            LOGGER.debug("Aborting execution on write to VRAM with tape at end");
            executionAborted = true;
        }
    }

    @Override
    public int peek16(int address) {
        clock.addTstates(8);
        return memory.peek16(address);
    }

    @Override
    public void poke16(int address, int word) {
        clock.addTstates(8);
        memory.poke16(address, word);
        if (crtc.isVideoAddress(address) && isTapeNearEndPosition()) {
            LOGGER.debug("Aborting execution on write to VRAM with tape at end");
            executionAborted = true;
        }
    }

    @Override
    public int inPort(int port) {
        clock.addTstates(4); // 4 clocks for read byte from bus (right?)
        if ((port & 0xFF00) == 0xBE00) {
            //LOGGER.debug("CRTC Read Status");
            return crtc.onReadStatusRegisterOperation();
        } else if ((port & 0xFF00) == 0xBF00) {
            //LOGGER.debug("CRTC Read Data");
            return crtc.onReadRegisterOperation();
        } else if ((port & 0xFF00) == 0xF400) {
            //LOGGER.debug("Ppi PortA (PSG)");
            return ppi.portAInput();
        } else if ((port & 0xFF00) == 0xF500) {
            return ppi.portBInput();
        } else if ((port & 0xFF00) == 0xF600) {
            return ppi.portCInput();
        } else {
            LOGGER.debug("Unhandled I/O IN Operation on port {}. Z80 Status: {}",
                    String.format("0x%04x", port),
                    z80.getZ80State(),
                    new Throwable());
        }
        return 0;
    }

    @Override
    public void outPort(int port, int value) {
        clock.addTstates(4); // 4 clocks for writing byte to bus (right?)
        if ((port & 0xFF00) == 0x7F00) {
            //LOGGER.debug("GateArray I/O Port {}, Value {}",
            //      String.format("%04x", port), String.format("%02x", value));
            //gateArray.onPortWriteOperation(port & 0xff);
            gateArray.onPortWriteOperation( value & 0xff);
        } else if ((port & 0xFF00) == 0xBC00) {
            //LOGGER.debug("CRTC register index selection");
            crtc.onSelectRegisterOperation(value & 0xff);
        } else if ((port & 0xFF00) == 0xBD00) {
            //LOGGER.debug("CRTC register data selection");
            crtc.onWriteRegisterOperation(value & 0xff);
        } else if ((port & 0xFF00) == 0xF400) {
            //LOGGER.debug("Ppi PortA OUT");
            ppi.portAOutput(value & 0xff);
        } else if ((port & 0xFF00) == 0xF500) {
            //LOGGER.warn("Ppi Port B OUT");
        } else if ((port & 0xFF00) == 0xF600) {
            //LOGGER.debug("Ppi Port C OUT");
            ppi.portCOutput(value & 0xff);
        } else if ((port & 0xFF00) == 0xF700) {
            //LOGGER.debug("Ppi Control Port OUT: {}, {}", String.format("0x%04x", port), String.format("0x%02x", value & 0xff));
            ppi.controlOutput(value & 0xff);
        } else if ((port & 0xF800) == 0xF800) {
            LOGGER.debug("Peripheral Soft Reset");
        } else if ((port & 0xFF00) == 0xDF00) {
            //LOGGER.debug("Selection of upper ROM number {}", value);
            upperRomNumber = value & 0xff;
        } else {
            LOGGER.debug("Unhandled I/O OUT Operation on port {}, value {}, Z80 Status: {}",
                    String.format("0x%04x", port),
                    String.format("0x%02x", value),
                    z80.getZ80State(),
                    new Throwable());
        }
    }

    protected long executeFrame(long compensation) {
        final long frameStartTstates = clock.getTstates();
        final long tStatesPerLine = crtc.getHorizontalTotal() * TSTATES_PER_US;
        final long tStatesToHSync = (crtc.getHSyncPos() + crtc.getHSyncLength()) * TSTATES_PER_US;
        final long vSyncTstates = frameStartTstates +
                crtc.getVSyncPos() * (crtc.getMaximumRasterAddress() + 1)
                        * tStatesPerLine;
        final long vSyncLines = (crtc.getMaximumRasterAddress() + 1) * crtc.getVSyncLength();
        final Counter gateArrayCounter = new Counter(6);
        final Counter hSyncCounter = new Counter(16);

        final ClockTimeout clockTimeout = new ClockTimeout();

        //LOGGER.debug("Frame[compensation={}, tstatesPerLine={}, vSyncPos={}, tStatesToHSync={}, vSyncTstates={}]",
          //     compensation, tStatesPerLine, crtc.getVSyncPos(), tStatesToHSync, vSyncTstates);
        z80.setInterruptAckListener((t) -> {
            //LOGGER.debug("INT ACK at {}. GateArrayCounter is {}", clock.getTstates() - frameStartTstates, gateArrayCounter.value());
            if (gateArray.isInterruptGenerationDelayed()) {
                gateArrayCounter.reset();
            } else {
                gateArrayCounter.mask(~0x20);
            }
            z80.setINTLine(false);
        });

        clockTimeout.setListener((long t) -> {
            if (gateArrayCounter.increment() == 52) {
                //LOGGER.debug("Enabling INT at {}", clock.getTstates() - frameStartTstates);
                z80.setINTLine(true);
                gateArrayCounter.reset();
            }
            if (ppi.isvSyncActive()) {
                /*
                if (hSyncCounter.increment() == 2) {
                    if ((gateArrayCounter.value() & 0x20) == 0) {
                        LOGGER.debug("VSYNC INT at {}", clock.getTstates() - frameStartTstates);
                        z80.setINTLine(true);
                    }
                    gateArrayCounter.reset();
                } else if (hSyncCounter.value() == vSyncLines) {
                    LOGGER.debug("Disabling VSYNC at {}", clock.getTstates() - frameStartTstates);
                    ppi.setvSyncActive(false);
                }
                */
                if (hSyncCounter.increment() == vSyncLines) {
                    //LOGGER.debug("Disabling VSYNC at {}", clock.getTstates() - frameStartTstates);
                    ppi.setvSyncActive(false);
                }
            } else {
                if (clock.getTstates() >= vSyncTstates && hSyncCounter.value() == 0) {
                    //LOGGER.debug("Enabling VSYNC at {}", clock.getTstates() - frameStartTstates);
                    ppi.setvSyncActive(true);
                }
            }
            clockTimeout.setTimeout(tStatesPerLine); //Next HSYNC comes after a whole line
        });
        clockTimeout.setTimeout(tStatesToHSync);

        clock.addClockTimeout(clockTimeout);
        try {
            compensation = executeTstates(FRAME_TSTATES - compensation);
        } finally {
            ppi.setvSyncActive(false);
            z80.resetInterruptAckListener();
            clock.removeClockTimeout(clockTimeout);
        }
        return compensation;
    }

    public long executeTstates(long tStates) {
        long limit = clock.getTstates() + tStates;
        while (clock.getTstates() < limit && !executionAborted) {
            z80.execute();
            if (tapePlayer.getTapePos() == tapePlayer.getTapeLength()) {
                executionAborted = true;
            }
        }
        return clock.getTstates() - limit;
    }


    @Override
    public void breakpoint() {
        LOGGER.debug("Breakpoint reached!!");
    }

    @Override
    public void execDone() {

    }
}
