package com.grelobites.romgenerator.dsk;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.dsk.DskConstants;
import com.grelobites.romgenerator.util.filesystem.AmsdosHeader;
import com.grelobites.romgenerator.util.filesystem.Archive;
import com.grelobites.romgenerator.util.filesystem.CpmFileSystem;

import java.io.IOException;

public class RescueDskTests {

    public void generateRescueDsk() throws IOException {
        AmsdosHeader header = AmsdosHeader.builder()
                .withName("RESCUE")
                .withExecAddress(0x4000)
                .withLoadAddress(0x4000)
                .withType(AmsdosHeader.Type.BINARY)
                .build();
        Archive archive = new Archive("RESCUE", "", 0,
                Util.concatArrays(header.toByteArray(),
                        Constants.getRescueEewriter()));
        CpmFileSystem fileSystem = new CpmFileSystem(DskConstants.CPC_DATA_FS_PARAMETERS);
        fileSystem.addArchive(archive);


    }
}
