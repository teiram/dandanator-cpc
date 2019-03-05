package com.grelobites.romgenerator.handlers.dandanatorcpc.view;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SerialCopyController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialCopyController.class);

    @FXML
    private ProgressBar copyProgress;

    @FXML
    void initialize() throws IOException {
        copyProgress.progressProperty().set(0.0);
    }

}
