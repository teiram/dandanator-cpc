package com.grelobites.romgenerator.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Circle;

import java.io.IOException;

public class CpldProgrammerController {

    @FXML
    private Circle arduinoDetectedLed;

    @FXML
    private Circle arduinoValidatedLed;

    @FXML
    private Circle arduinoUpdatedLed;

    @FXML
    private Circle dandanatorDetectedLed;

    @FXML
    private Circle dandanatorUpdatedLed;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button programButton;

    private BooleanProperty programming;

    private DoubleProperty progress;

    public CpldProgrammerController() {
        this.programming = new SimpleBooleanProperty(false);
        this.progress = new SimpleDoubleProperty(0.0);
    }

    @FXML
    void initialize() throws IOException {

        programButton.disableProperty().bind(programming);
        progressBar.progressProperty().bind(progress);


    }

}
