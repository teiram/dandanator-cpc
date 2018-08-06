package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.eewriter.SerialPortInterfaces;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;


public class EepromWriterConfigurationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(EepromWriterConfigurationController.class);

    @FXML
    private ComboBox<String> serialPort;

    @FXML
    private Button refreshSerialPorts;

    @FXML
    private Label customRomSetPath;

    @FXML
    private Button changeCustomRomSetPathButton;

    @FXML
    private Button resetCustomRomSetPathButton;

    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private void updateCustomRomSetPath(File romsetFile) {
        if (isReadableFile(romsetFile) && romsetFile.length() == 32 * Constants.SLOT_SIZE) {
            PlayerConfiguration.getInstance().setCustomRomSetPath(romsetFile.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid ROMSet file provided");
        }
    }

    private static void showGenericFileErrorAlert() {
        DialogUtil.buildErrorAlert(LocaleUtil.i18n("fileImportError"),
                LocaleUtil.i18n("fileImportErrorHeader"),
                LocaleUtil.i18n("fileImportErrorContent"))
                .showAndWait();
    }

    private void setupFileBasedParameter(Button changeButton,
                                         String changeMessage,
                                         Label pathLabel,
                                         StringProperty configurationProperty,
                                         Button resetButton,
                                         String defaultMessage,
                                         Consumer<File> consumer) {
        pathLabel.textProperty().bindBidirectional(configurationProperty,
                new StringConverter<String>() {
                    @Override
                    public String toString(String object) {
                        LOGGER.debug("Executing toString on " + object);
                        if (object == null) {
                            return defaultMessage;
                        } else if (object.equals(Constants.ROMSET_PROVIDED)) {
                            return LocaleUtil.i18n("romsetProvidedMessage");
                        } else {
                            return object;
                        }
                    }

                    @Override
                    public String fromString(String string) {
                        LOGGER.debug("Executing fromString on " + string);
                        if (string == null) {
                            return null;
                        } else if (string.isEmpty()) {
                            return null;
                        } else {
                            return string;
                        }
                    }
                });

        changeButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(changeMessage);
            final File file = chooser.showOpenDialog(changeButton.getScene().getWindow());
            if (file != null) {
                try {
                    consumer.accept(file);
                    pathLabel.setText(file.getAbsolutePath());
                } catch (Exception e) {
                    LOGGER.error("In setupFileBasedParameter for " + changeMessage
                                    + " with file " + file, e);
                    showGenericFileErrorAlert();
                }
            }
        });

        resetButton.setOnAction(event ->
            configurationProperty.set(null));

    }

    private static String[] getSerialPortNames() {
        String[] serialPortNames = SerialPortInterfaces.getPortNames();
        LOGGER.debug("Serial Port Names are " + Arrays.asList(serialPortNames));
        return serialPortNames;
    }

    @FXML
    private void initialize() throws IOException {
        EepromWriterConfiguration configuration = EepromWriterConfiguration.getInstance();

        setupFileBasedParameter(changeCustomRomSetPathButton,
                LocaleUtil.i18n("useCustomRomSet"),
                customRomSetPath,
                configuration.customRomSetPathProperty(),
                resetCustomRomSetPathButton,
                LocaleUtil.i18n("none"),
                this::updateCustomRomSetPath);

        serialPort.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    configuration.setSerialPort(newValue);
                });

        refreshSerialPorts.setOnAction(e -> {
            serialPort.getSelectionModel().clearSelection();
            serialPort.getItems().clear();
            serialPort.getItems().addAll(getSerialPortNames());
        });

        ObservableList<String> serialPortNames = FXCollections.observableArrayList(getSerialPortNames());
        serialPort.setItems(serialPortNames);
        if (serialPortNames.contains(configuration.getSerialPort())) {
            serialPort.getSelectionModel().select(configuration.getSerialPort());
        } else {
            serialPort.getSelectionModel().clearSelection();
            configuration.setSerialPort(null);
        }
    }
}
