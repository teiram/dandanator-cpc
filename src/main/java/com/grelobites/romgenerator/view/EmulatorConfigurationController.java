package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.EmulatorConfiguration;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EmulatorConfigurationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorConfigurationController.class);

    @FXML
    private CheckBox testTapeStopConditions;

    @FXML
    private TextField tapeRemainingBytes;

    @FXML
    private CheckBox testPaletteChanges;

    @FXML
    private CheckBox testCrtcAccess;

    @FXML
    private CheckBox testVramWrites;

    @FXML
    private CheckBox testOnMotorStopped;

    @FXML
    private CheckBox testPsgAccess;

    @FXML
    private CheckBox testKeyboardReads;

    @FXML
    private void initialize() throws IOException {
        EmulatorConfiguration configuration = EmulatorConfiguration.getInstance();

        testTapeStopConditions.selectedProperty().bindBidirectional(
                configuration.testTapeStopConditionsProperty());
        tapeRemainingBytes.textProperty().bindBidirectional(
                configuration.tapeRemainingBytesProperty(),
                new StringConverter<Number>() {
                    @Override
                    public String toString(Number object) {
                        return object.toString();
                    }

                    @Override
                    public Number fromString(String string) {
                        return Integer.parseInt(string);
                    }
                }
        );

        testPaletteChanges.selectedProperty().bindBidirectional(
                configuration.testPaletteChangesProperty());
        testCrtcAccess.selectedProperty().bindBidirectional(
                configuration.testCrtcAccessProperty());
        testVramWrites.selectedProperty().bindBidirectional(
                configuration.testVramWritesProperty());
        testOnMotorStopped.selectedProperty().bindBidirectional(
                configuration.testOnMotorStoppedProperty());
        testPsgAccess.selectedProperty().bindBidirectional(
                configuration.testPsgAccessProperty());
        testKeyboardReads.selectedProperty().bindBidirectional(
                configuration.testKeyboardReadsProperty());
    }
}
