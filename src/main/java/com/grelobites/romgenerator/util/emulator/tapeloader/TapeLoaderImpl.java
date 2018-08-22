package com.grelobites.romgenerator.util.emulator.tapeloader;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.emulator.Clock;
import com.grelobites.romgenerator.util.emulator.peripheral.Crtc;
import com.grelobites.romgenerator.util.emulator.peripheral.CrtcType;
import com.grelobites.romgenerator.util.emulator.peripheral.Ppi;
import com.grelobites.romgenerator.util.emulator.RomResources;
import com.grelobites.romgenerator.util.emulator.TapePlayer;
import com.grelobites.romgenerator.util.emulator.TapeLoader;
import com.grelobites.romgenerator.util.emulator.Z80;
import com.grelobites.romgenerator.util.emulator.Z80operations;
import com.grelobites.romgenerator.util.emulator.peripheral.CpcMemory;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final RomResources romResources;
    private final GateArray gateArray;
    private final Z80 z80;
    private final Clock clock;
    private final TapePlayer tapePlayer;
    private final Crtc crtc;
    private final Ppi ppi;
    private final CpcMemory memory;

    private void loadRoms() throws IOException {
        memory.loadLowRom(romResources.lowRom());
        memory.loadHighRom(romResources.highRom());
    }

    public TapeLoaderImpl(RomResources romResources) throws IOException {
        this.romResources = romResources;
        clock = new Clock();
        z80 = new Z80(clock, this);
        tapePlayer = new TapePlayer(clock, true);
        gateArray = GateArray.newBuilder()
                .withCpc464DefaultValues().build();
        memory = new CpcMemory(gateArray);
        crtc = new Crtc(CrtcType.CRTC_TYPE_1);
        ppi = new Ppi();
        loadRoms();
    }

    @Override
    public Game loadTape(InputStream tapeFile) {
        return null;
    }

    @Override
    public int fetchOpcode(int address) {
        return peek8(address);
    }

    @Override
    public int peek8(int address) {
        return memory.peek8(address);
    }

    @Override
    public void poke8(int address, int value) {
        memory.poke8(address, value);
    }

    @Override
    public int peek16(int address) {
        return memory.peek16(address);
    }

    @Override
    public void poke16(int address, int word) {
        memory.poke16(address, word);
    }

    @Override
    public int inPort(int port) {
        clock.addTstates(4); // 4 clocks for read byte from bus (right?)
        if ((port & 0x4300) == 0x0200) {
            LOGGER.debug("CRTC Read Status");
            return crtc.onReadStatusRegisterOperation();
        } else if ((port & 0x4300) == 0x0300) {
            LOGGER.debug("CRTC Read Register");
            return crtc.onReadRegisterOperation();
        } else if ((port & 0xFF00) == 0xF400) {
            LOGGER.debug("Ppi PortA (PSG)");
            return ppi.portAInput();
        } else if ((port & 0xFF00) == 0xF500) {
            LOGGER.debug("Ppi PortB Input");
            return ppi.portBInput();
        } else {
            LOGGER.debug("Unhandled I/O IN Operation on port {}",
                    String.format("%04x", port));
        }
        return 0;
    }

    @Override
    public void outPort(int port, int value) {
        clock.addTstates(4); // 4 clocks for writing byte to bus (right?)
        if ((port & 0xC000) == 0x4000) {
            LOGGER.debug("Gate array I/O operation");
            gateArray.onPortWriteOperation(port & 0xff);
        } else if ((port & 0x4300) == 0) {
            LOGGER.debug("CRTC register index selection");
            crtc.onSelectRegisterOperation(port & 0xff);
        } else if ((port & 0x4300) == 0x0100) {
            LOGGER.debug("CRTC register data selection");
            crtc.onWriteRegisterOperation(port & 0xff);
        } else if ((port & 0xFF00) == 0xF400) {
            LOGGER.debug("Ppi PortA OUT");
            ppi.portAOutput(value);
        } else if ((port & 0xFF00) == 0xF500) {
            LOGGER.warn("Ppi Port B OUT");
        } else if ((port & 0xFF00) == 0xF600) {
            LOGGER.debug("Ppi Port C OUT");
            ppi.portCOutput(value);
        } else if ((port & 0xFF00) == 0xF700) {
            LOGGER.debug("Ppi Control Port OUT");
            ppi.controlOutput(value);
        } else {
            LOGGER.debug("Unhandled I/O OUT Operation on port {}",
                    String.format("%04x", port));
        }
    }

    protected void executeFrame() {
        long vsync_tstates = clock.getTstates() + VSYNC_TSTATES;
        long lastInterruptTstates = 0;
        while (clock.getTstates() < vsync_tstates) {
            long toNextInterrupt = clock.getTstates() + INTERRUPT_TSTATES;
            z80.execute(toNextInterrupt);
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
