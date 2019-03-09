package com.grelobites.romgenerator.util.dsk;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.emulator.BaseEmulator;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DskLoader extends BaseEmulator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskLoader.class);
    private static final int FRAMES_PER_SECOND = 50;
    private static final int DISK_READ_THRESHOLD = 16384;
    private static final int DISK_ACCESS_TIMEOUT_TS = FRAME_TSTATES * FRAMES_PER_SECOND * 10;
    private static final String CPM_BOOTSTRAP_COMMAND = "|cpm";
    private static final String RUN_COMMAND_TEMPLATE = "run \"%s";

    public DskLoader(HardwareMode hardwareMode,
                     LoaderResources loaderResources) {
        super(hardwareMode, loaderResources);
    }

    private static List<Archive> getBasicLoaders(CpmFileSystem fileSystem) {
        //Non system-flagged files with BAS extension
        //System-flagged files are hidden to the CAT AMSDOS command
        //and therefore they are not intended to be directly executed by the user
        List<Archive> candidates = fileSystem.getArchiveList().stream()
                .filter(archive -> !archive.getFlags().contains(ArchiveFlags.SYSTEM))
                .filter(archive -> archive.getExtension().equalsIgnoreCase("BAS"))
                .collect(Collectors.toList());
        LOGGER.debug("Candidate basic loaders are {}", candidates);
        return candidates;
    }

    private static List<Archive> getBinLoaders(CpmFileSystem fileSystem) {
        //Non System-flagged files with BIN or empty extension
        //and with a valid AMSDOS header (meaning that it may declare
        //a load and execution address)
        List<Archive> candidates = fileSystem.getArchiveList().stream()
                .filter(archive -> !archive.getFlags().contains(ArchiveFlags.SYSTEM))
                .filter(archive -> archive.getExtension().equalsIgnoreCase("BIN") ||
                archive.getExtension().equals("   "))
                .filter(archive -> AmsdosHeader.fromArchive(archive).isPresent())
                .collect(Collectors.toList());
        LOGGER.debug("Candidate bin loaders are {}", candidates);
        return candidates;
    }

    private static String getRunCommandForName(String name) {
        return String.format(RUN_COMMAND_TEMPLATE, name.trim());
    }

    private static String getRunCommandForArchive(Archive archive) {
        return getRunCommandForName(archive.getName());
    }

    private static String guessBootstrapCommand(DskContainer container) throws IOException {
        FileSystemParameters parameters = DskUtil.guessFileSystemParameters(container);
        if (parameters.getReservedTracks() > 0) {
            //Probably a system disk that can be loaded with |cpm?
            return CPM_BOOTSTRAP_COMMAND;
        } else {
            //Try to get filenames from filesystem
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            container.dumpRawData(bos);
            CpmFileSystem fileSystem = CpmFileSystem.fromByteArray(bos.toByteArray(), parameters);
            List<Archive> basicCandidates = getBasicLoaders(fileSystem);
            if (basicCandidates.size() > 1) {
                LOGGER.warn("Got more than one basic loader {}", basicCandidates);
                //TODO: Probably we should try to use all the basic candidates
                // And decide which captured snapshot looks better (more data loaded from disk)
                return getRunCommandForArchive(basicCandidates.stream().sorted(Comparator.comparingInt(c ->
                        c.getName().trim().length())).findFirst().get());
            } else if (basicCandidates.size() > 0) {
                return getRunCommandForArchive(basicCandidates.get(0));
            }
            List<Archive> binCandidates = getBinLoaders(fileSystem);
            if (binCandidates.size() > 1) {
                LOGGER.warn("Got more than one bin loader {}", binCandidates);
                //In this case we use the shortest name, since normally we have things like:
                // ADVENTUR.BIN ADVENTUR01.BIN, ADVENTUR02.BIN,...
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
            LOGGER.info("Guesses bootstrap command as: {}", command);
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
                        LOGGER.info("Trying to force load resume with keyboard input");
                        enterCommand("1");
                    } else {
                        executionAborted = true;
                    }
                }
            }
        }
        return getSnapshotGame();
    }

    public Game loadBas(DskContainer container, String name) throws IOException {
        long compensation = 0;
        //Guess what is inside the disk
        String command = getRunCommandForName(name);
        if (command != null) {
            LOGGER.info("Guesses bootstrap command as: {}", command);
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
                        LOGGER.info("Trying to force load resume with keyboard input");
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
