package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.dsk.DskConstants;
import com.grelobites.romgenerator.util.dsk.DskContainer;
import com.grelobites.romgenerator.util.dsk.DskLoader;
import com.grelobites.romgenerator.util.emulator.resources.Cpc6128LoaderResources;
import com.grelobites.romgenerator.util.filesystem.Archive;
import com.grelobites.romgenerator.util.filesystem.CpmFileSystem;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class BasGameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasGameImageLoader.class);
    private static final String LOADER_NAME = "LOADER";
    private static final String LOADER_EXTENSION = "BAS";

    @Override
    public Game load(InputStream is) throws IOException {
        DskLoader loader = new DskLoader(HardwareMode.HW_CPC6128,
                Cpc6128LoaderResources.getInstance());
        DskContainer dskContainer = DskContainer.emptyDisk(DskConstants.CPC_DATA_FS_PARAMETERS);
        CpmFileSystem fileSystem = new CpmFileSystem(DskConstants.CPC_DATA_FS_PARAMETERS);
        fileSystem.addArchive(new Archive(LOADER_NAME, LOADER_EXTENSION, 0,
                Util.fromInputStream(is)));
        byte[] fileSystemBytes = fileSystem.asByteArray();
        dskContainer.populateWithRawData(DskConstants.CPC_DATA_FS_PARAMETERS,
                new ByteArrayInputStream(fileSystemBytes));
        SnapshotGame game = (SnapshotGame) loader.loadBas(dskContainer, LOADER_NAME);
        game.setHoldScreen(true);
        return game;
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        throw new IllegalStateException("Save to BAS not supported");
    }
}
