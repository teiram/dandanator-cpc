package com.grelobites.romgenerator.util.imageloader;

import com.grelobites.romgenerator.util.imageloader.loaders.MultipaintImageLoader;
import com.grelobites.romgenerator.util.imageloader.loaders.RawImageLoader;
import com.grelobites.romgenerator.util.imageloader.loaders.RgasImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

public enum ImageType {
    RAW(RawImageLoader.class),
    MULTIPAINT(MultipaintImageLoader.class),
    RGAS(RgasImageLoader.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageType.class);

    private Class<? extends ImageLoader> loaderClass;
    private ImageLoader loader;
    ImageType(Class<? extends ImageLoader> loaderClass) {
        this.loaderClass = loaderClass;
    }
    public ImageLoader loader() {
        if (loader == null) {
            try {
                loader = loaderClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return loader;
    }

    public static Optional<ImageLoader> imageLoader(File file) {
        for (ImageType imageType : ImageType.values()) {
            if (imageType.loader().supportsFile(file)) {
                LOGGER.debug("ImageType {} supporting image file {}", imageType, file);
                return Optional.of(imageType.loader());
            }
        }
        LOGGER.info("No ImageType supports file {}", file);
        return Optional.empty();
    }

}
