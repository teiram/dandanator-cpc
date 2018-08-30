package com.grelobites.romgenerator.util.gameloader;

import com.grelobites.romgenerator.util.gameloader.loaders.CdtGameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.MldGameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.RomGameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.SNAGameImageLoader;

public enum GameImageType {
    SNA(SNAGameImageLoader.class, "sna"),
    ROM(RomGameImageLoader.class, "rom"),
    MLD(MldGameImageLoader.class, "mld"),
    CDT(CdtGameImageLoader.class, "cdt");

    private Class<? extends GameImageLoader> generator;
    private String[] supportedExtensions;

    GameImageType(Class<? extends GameImageLoader> generator,
                  String... supportedExtensions) {
        this.generator = generator;
        this.supportedExtensions = supportedExtensions;
    }

    public static GameImageType fromExtension(String extension) {
        for (GameImageType type : GameImageType.values()) {
            if (type.supportsExtension(extension)) {
                return type;
            }
        }
        return null;
    }

    public boolean supportsExtension(String extension) {
        for (String candidate : supportedExtensions) {
            if (candidate.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    public Class<? extends GameImageLoader> generator() {
        return generator;
    }
}
