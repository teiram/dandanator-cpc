package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.LoaderConfiguration;
import com.grelobites.romgenerator.view.LoaderController;

public class BlockServiceFactory {

    public static BlockService getBlockService(LoaderController controller, LoaderConfiguration configuration) {
        switch (configuration.getBlockServiceType()) {
            case HTTP:
                return new HttpBlockService(controller, configuration);
            case SERIAL:
                return new SerialBlockService(controller, configuration);
            default:
                throw new IllegalArgumentException("Unsupported block service request");
        }
    }
}
