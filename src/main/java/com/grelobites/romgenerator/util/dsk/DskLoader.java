package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.emulator.BaseEmulator;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArrayChangeListener;
import com.grelobites.romgenerator.util.emulator.peripheral.GateArrayFunction;
import com.grelobites.romgenerator.util.emulator.peripheral.KeyboardCode;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.resources.LoaderResources;
import com.grelobites.romgenerator.util.filesystem.Archive;
import com.grelobites.romgenerator.util.filesystem.CpmFileSystem;
import com.grelobites.romgenerator.util.tape.CdtTapePlayer;
import com.grelobites.romgenerator.util.tape.TapeFinishedException;
import com.grelobites.romgenerator.util.tape.TapeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DskLoader extends BaseEmulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskLoader.class);
    private static final int DISK_ACCESS_TIMEOUT_TS = 5000;
    private static final int FRAMES_PER_SECOND = 50;

    private final Nec765 nec765;
    private SnapshotGame currentSnapshot;
    private long lastDiskAccessTstates = 0;
    public DskLoader(HardwareMode hardwareMode,
                     LoaderResources loaderResources) {
        super(hardwareMode, loaderResources);
        nec765 = new Nec765();
    }

    @Override
    public void outPort(int port, int value) {
        if ((port & 0xFA7E) == 0xFA7E) {
            lastDiskAccessTstates = clock.getTstates();
            nec765.writeControlRegister(value);
        } else if ((port & 0xFB7F) == 0xFB7F) {
            lastDiskAccessTstates = clock.getTstates();
            nec765.writeDataRegister(value);
        } else {
            super.outPort(port, value);
        }
    }

    @Override
    public int inPort(int port) {
        if ((port & 0xFB7F) == 0xFB7F) {
            lastDiskAccessTstates = clock.getTstates();
            return nec765.readDataRegister();
        } else if ((port & 0xFB7E) == 0xFB7E) {
            lastDiskAccessTstates = clock.getTstates();
            return nec765.readStatusRegister();
        } else {
            return super.inPort(port);
        }
    }

    private static String guessBootstrapCommand(DskContainer container) throws IOException {
        FileSystemParameters parameters = DskUtil.guessFileSystemParameters(container);
        if (parameters.getReservedTracks() > 0) {
            //Probaby system disk that can be loaded with |cpm
            return "|cpm";
        } else {
            //Try to get filenames from filesystem
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            container.dumpRawData(bos);
            CpmFileSystem fileSystem = CpmFileSystem.fromByteArray(bos.toByteArray(), parameters);
            for (Archive archive: fileSystem.getArchiveList()) {
                if (archive.getName().equals("DISC")) {
                    return "run \"disc";
                } else if (archive.getExtension().equalsIgnoreCase("BAS")) {
                    return "run \"" + archive.getName();
                } else if (archive.getExtension().equalsIgnoreCase("BIN")) {
                    return "run \"" + archive.getName();
                }
            }
            //No candidate if we reach this point
            return null;
        }
    }
    public Game loadDsk(InputStream dskFile) throws IOException {
        long compensation = 0;
        //Guess what is inside the disk
        DskContainer container = DskContainer.fromInputStream(dskFile);
        String command = guessBootstrapCommand(container);
        if (command != null) {
            LOGGER.debug("Guesses boootstrap command as {}", command);
            //Wait for the computer to initialize
            for (int i = 0; i < 5 * FRAMES_PER_SECOND; i++) {
                compensation = executeFrame(compensation);
            }
            //Run the guessed command
            enterCommand(command);
            //Attach the disk to the controller
            nec765.attachDskContainer(0, container);
            while (!executionAborted) {
                compensation = executeFrame(compensation);
                if ((clock.getTstates() - lastDiskAccessTstates) > DISK_ACCESS_TIMEOUT_TS) {
                    LOGGER.debug("Execution aborted due to disk access inactivity");
                    executionAborted = true;
                }
            }
        }
        currentSnapshot = getSnapshotGame();
        return currentSnapshot;
    }

}
