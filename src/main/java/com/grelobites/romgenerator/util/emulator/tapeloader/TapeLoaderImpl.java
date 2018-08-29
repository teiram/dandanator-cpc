package com.grelobites.romgenerator.util.emulator.tapeloader;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.emulator.Clock;
import com.grelobites.romgenerator.util.emulator.LoaderResources;
import com.grelobites.romgenerator.util.emulator.TapeFinishedException;
import com.grelobites.romgenerator.util.emulator.TapeLoader;
import com.grelobites.romgenerator.util.emulator.Z80;
import com.grelobites.romgenerator.util.emulator.Z80State;
import com.grelobites.romgenerator.util.emulator.Z80operations;
import com.grelobites.romgenerator.util.emulator.peripheral.CpcMemory;
import com.grelobites.romgenerator.util.emulator.peripheral.Crtc;
import com.grelobites.romgenerator.util.emulator.peripheral.CrtcType;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArray;
import com.grelobites.romgenerator.util.emulator.peripheral.KeyboardCode;
import com.grelobites.romgenerator.util.emulator.peripheral.Ppi;
import com.grelobites.romgenerator.util.gameloader.loaders.SNAGameImageLoader;
import com.grelobites.romgenerator.util.tape.CDTTapePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TapeLoaderImpl implements TapeLoader, Z80operations {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoaderImpl.class);
    private static final int CPU_HZ = 4000000;
    private static final int VSYNC_HZ = 50;
    private static final int VSYNC_TSTATES = CPU_HZ / VSYNC_HZ;
    private static final int HSYNC_TSTATES = 64 * 4; // 64 microseconds
    private static final int INTERRUPT_HSYNC_COUNT = 52;
    private static final int INTERRUPT_TSTATES = HSYNC_TSTATES * INTERRUPT_HSYNC_COUNT;
    private static final int HSYNC_32_DELAY = HSYNC_TSTATES * 32;

    private final LoaderResources loaderResources;
    private final GateArray gateArray;
    private final Z80 z80;
    private final Clock clock;
    private final CDTTapePlayer tapePlayer;
    private final Crtc crtc;
    private final Ppi ppi;
    private final CpcMemory memory;
    private final HardwareMode hardwareMode;
    private int upperRomNumber = 0;

    private void loadRoms() throws IOException {
        memory.loadLowRom(loaderResources.lowRom());
        memory.loadHighRom(loaderResources.highRom());
    }

    public TapeLoaderImpl(HardwareMode hardwareMode,
                          LoaderResources loaderResources) throws IOException {
        this.loaderResources = loaderResources;
        clock = new Clock();
        this.hardwareMode = hardwareMode;
        z80 = new Z80(clock, this);
        ppi = new Ppi();
        gateArray = GateArray.newBuilder()
                .withCpc464DefaultValues().build();
        crtc = new Crtc(CrtcType.CRTC_TYPE_0);
        memory = new CpcMemory(gateArray);
        tapePlayer = new CDTTapePlayer(clock, ppi, true);
        loadRoms();
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
        for (int i = 0; i < frames; i++) {
            executeFrame();
        }
        ppi.releaseKey(key);
    }

    @Override
    public Game loadTape(InputStream tapeFile) throws IOException {
        tapePlayer.insert(tapeFile);
        loadLoaderSnapshot();
        //Press enter key to continue with load
        pressKeyDuringFrames(KeyboardCode.KEY_ENTER, 20);
        while (!ppi.isMotorOn()) {
            executeFrame();
        }
        LOGGER.info("Motor is on!");
        final SNAGameImageLoader exporter = new SNAGameImageLoader();
        tapePlayer.addBlockChangeListener((c) -> {
            String fileName = String.format("/home/mteira/Escritorio/test%d.sna", c);
            LOGGER.debug("Detected block change. Saving snapshot {}", fileName);
            exporter.save(toSnapshotGame(), new FileOutputStream(new File(fileName)));
        });
        tapePlayer.play();
        try {
            while (!tapePlayer.isEOT()) {
                executeFrame();
            }
        } catch (TapeFinishedException tfe) {
            LOGGER.debug("Tape finished with cpu status " + z80.getZ80State(), tfe);
        }
        LOGGER.info("Ended");
        tapePlayer.stop();
        return toSnapshotGame();
    }

    @Override
    public int fetchOpcode(int address) {
        return peek8(address);
    }

    @Override
    public int peek8(int address) {
        clock.addTstates(3);
        return memory.peek8(address);
    }

    @Override
    public void poke8(int address, int value) {
        clock.addTstates(3);
        memory.poke8(address, value);
    }

    @Override
    public int peek16(int address) {
        clock.addTstates(3);
        return memory.peek16(address);
    }

    @Override
    public void poke16(int address, int word) {
        clock.addTstates(3);
        memory.poke16(address, word);
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
        } else {
            //LOGGER.debug("Unhandled I/O IN Operation on port {}", String.format("%04x", port));
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
            //LOGGER.debug("Ppi Control Port OUT");
            ppi.controlOutput(value & 0xff);
        } else if ((port & 0xFF00) == 0xDF00) {
            //LOGGER.debug("Selection of upper ROM number {}", value);
            upperRomNumber = value & 0xff;
        } else {
            //LOGGER.debug("Unhandled I/O OUT Operation on port {}", String.format("%04x", port));
        }
    }

    protected void executeFrame() {
        long vsync_tstates = clock.getTstates() + VSYNC_TSTATES;
        long lastInterruptTstates = 0;
        int interruptAttempt = 0;
        while (clock.getTstates() < vsync_tstates) {
            long toNextInterrupt = clock.getTstates() + INTERRUPT_TSTATES;
            z80.execute(toNextInterrupt);
            if (++interruptAttempt == 5) {
                ppi.setvSyncActive(true);
            }
            if (!z80.isINTLine() &&
                    (clock.getTstates() - lastInterruptTstates) > HSYNC_32_DELAY) {
                boolean preIFF1 = z80.isIFF1();
                z80.setINTLine(true);
                lastInterruptTstates = clock.getTstates();
                z80.execute();
                boolean asserted = preIFF1 != z80.isIFF1();
                if (asserted) {
                    z80.setINTLine(false);
                }
            }
        }
        z80.setINTLine(false);
        ppi.setvSyncActive(false);
    }

    @Override
    public void contendedStates(int address, int tstates) {
        clock.addTstates(tstates);
        //TODO: Change the way this is implemented, if it is even valid on CPC
    }


    @Override
    public void breakpoint() {
        LOGGER.debug("Breakpoint reached!!");
    }

    @Override
    public void execDone() {

    }
}
