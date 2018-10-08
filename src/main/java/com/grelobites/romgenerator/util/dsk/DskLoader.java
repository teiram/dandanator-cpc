package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.emulator.BaseEmulator;
import com.grelobites.romgenerator.util.emulator.peripheral.fdc.Nec765;
import com.grelobites.romgenerator.util.emulator.resources.LoaderResources;
import com.grelobites.romgenerator.util.filesystem.AmsdosHeader;
import com.grelobites.romgenerator.util.filesystem.Archive;
import com.grelobites.romgenerator.util.filesystem.ArchiveFlags;
import com.grelobites.romgenerator.util.filesystem.CpmFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DskLoader extends BaseEmulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskLoader.class);
    private static final int FRAMES_PER_SECOND = 50;
    private static final int DISK_READ_THRESHOLD = 16384;
    private static final int DISK_ACCESS_TIMEOUT_TS = FRAME_TSTATES * FRAMES_PER_SECOND * 4;

    private final Nec765 nec765;
    private long lastDiskAccessTstates = 0;
    public DskLoader(HardwareMode hardwareMode,
                     LoaderResources loaderResources) {
        super(hardwareMode, loaderResources);
        nec765 = new Nec765();
    }

    @Override
    public void outPort(int port, int value) {
        if ((port & 0xFFFF) == 0xFA7E) {
            lastDiskAccessTstates = clock.getTstates();
            nec765.writeControlRegister(value);
        } else if ((port & 0xFFFF) == 0xFB7F) {
            lastDiskAccessTstates = clock.getTstates();
            nec765.writeDataRegister(value);
        } else {
            super.outPort(port, value);
        }
    }

    @Override
    public int inPort(int port) {
        if ((port & 0xFFFF) == 0xFB7F) {
            lastDiskAccessTstates = clock.getTstates();
            return nec765.readDataRegister();
        } else if ((port & 0xFFFF) == 0xFB7E) {
            lastDiskAccessTstates = clock.getTstates();
            return nec765.readStatusRegister();
        } else {
            return super.inPort(port);
        }
    }

    private static List<Archive> getBasicLoaders(CpmFileSystem fileSystem) {
        List<Archive> candidates = fileSystem.getArchiveList().stream()
                .filter(archive -> !archive.getFlags().contains(ArchiveFlags.SYSTEM))
                .filter(archive -> archive.getExtension().equalsIgnoreCase("BAS"))
                .collect(Collectors.toList());
        LOGGER.debug("Candidate basic loaders are {}", candidates);
        return candidates;
    }

    private static List<Archive> getBinLoaders(CpmFileSystem fileSystem) {
        List<Archive> candidates = fileSystem.getArchiveList().stream()
                .filter(archive -> !archive.getFlags().contains(ArchiveFlags.SYSTEM))
                .filter(archive -> archive.getExtension().equalsIgnoreCase("BIN") ||
                archive.getExtension().equals("   "))
                .filter(archive -> AmsdosHeader.fromArchive(archive).isPresent())
                .collect(Collectors.toList());
        //Check if we have an AMSDOS header
        LOGGER.debug("Candidate bin loaders are {}", candidates);
        return candidates;
    }

    private static String getRunCommandForArchive(Archive archive) {
        return "run \"" + archive.getName();
    }

    private static String guessBootstrapCommand(DskContainer container) throws IOException {
        FileSystemParameters parameters = DskUtil.guessFileSystemParameters(container);
        if (parameters.getReservedTracks() > 0) {
            //Probaby system disk that can be loaded with |cpm?
            return "|cpm";
        } else {
            //Try to get filenames from filesystem
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            container.dumpRawData(bos);
            CpmFileSystem fileSystem = CpmFileSystem.fromByteArray(bos.toByteArray(), parameters);
            List<Archive> basicCandidates = getBasicLoaders(fileSystem);
            if (basicCandidates.size() > 1) {
                LOGGER.warn("Got more than one basic loader {}", basicCandidates);
                return getRunCommandForArchive(basicCandidates.stream().sorted(Comparator.comparingInt(c ->
                        c.getName().trim().length())).findFirst().get());
            } else if (basicCandidates.size() > 0) {
                return getRunCommandForArchive(basicCandidates.get(0));
            }
            List<Archive> binCandidates = getBinLoaders(fileSystem);
            if (binCandidates.size() > 1) {
                LOGGER.warn("Got more than one bin loader {}", binCandidates);
                return getRunCommandForArchive(binCandidates.stream().sorted(Comparator.comparingInt(c ->
                        c.getName().trim().length())).findFirst().get());
            } else if (binCandidates.size() > 0) {
                return getRunCommandForArchive(binCandidates.get(0));
            }
        }
        return null;
    }

    public Game loadDsk(InputStream dskFile) throws IOException {
        long compensation = 0;
        //Guess what is inside the disk
        DskContainer container = DskContainer.fromInputStream(dskFile);
        String command = guessBootstrapCommand(container);
        if (command != null) {
            LOGGER.info("Guesses bootstrap command as {}", command);
            //Wait for the computer to initialize
            for (int i = 0; i < 2 * FRAMES_PER_SECOND; i++) {
                compensation = executeFrame(compensation);
            }
            nec765.attachDskContainer(0, container);
            LOGGER.debug("CPC Initialized. Now to run loader");
            //Run the guessed command
            enterCommand(command);
            //Attach the disk to the controller
            while (!executionAborted) {
                compensation = executeFrame(compensation);
                if ((clock.getTstates() - lastDiskAccessTstates) > DISK_ACCESS_TIMEOUT_TS) {
                    LOGGER.info("Execution timeout due to disk access inactivity with FDC statistics {}",
                            nec765.getStatistics());
                    if (nec765.getStatistics().getBytesRead() < DISK_READ_THRESHOLD) {
                        LOGGER.info("Trying to force loading with keyboard input");
                        enterCommand("1");
                    } else {
                        executionAborted = true;
                    }
                }
            }
        }
        return getSnapshotGame();
    }

}
