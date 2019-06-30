package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.view.EepromWriterController;

public class BlockServiceFactory {

    public static BlockService getBlockService(EepromWriterController controller, EepromWriterConfiguration configuration) {
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
