package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.LoaderConfiguration;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.eewriter.BlockServiceType;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;


public class LoaderConfigurationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderConfigurationController.class);

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

    @FXML
    private RadioButton serialBlockService;

    @FXML
    private RadioButton httpBlockService;

    @FXML
    private TextField httpUrl;

    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private void updateCustomRomSetPath(File romsetFile) {
        if (isReadableFile(romsetFile) && romsetFile.length() == 32 * Constants.SLOT_SIZE) {
            LoaderConfiguration.getInstance().setCustomRomSetPath(romsetFile.getAbsolutePath());
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

    @FXML
    private void initialize() throws IOException {
        LoaderConfiguration configuration = LoaderConfiguration.getInstance();

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
            serialPort.getItems().addAll(Util.getSerialPortNames());
        });

        ObservableList<String> serialPortNames = FXCollections.observableArrayList(Util.getSerialPortNames());
        serialPort.setItems(serialPortNames);
        if (serialPortNames.contains(configuration.getSerialPort())) {
            serialPort.getSelectionModel().select(configuration.getSerialPort());
        } else {
            serialPort.getSelectionModel().clearSelection();
            configuration.setSerialPort(null);
        }

        serialBlockService.setSelected(configuration.getBlockServiceType() == BlockServiceType.SERIAL);
        serialPort.setDisable(configuration.getBlockServiceType() == BlockServiceType.HTTP);
        refreshSerialPorts.setDisable(configuration.getBlockServiceType() == BlockServiceType.HTTP);
        httpUrl.setDisable(configuration.getBlockServiceType() == BlockServiceType.SERIAL);

        serialBlockService.selectedProperty().addListener((object, oldValue, newValue) -> {
            serialPort.setDisable(!newValue);
            refreshSerialPorts.setDisable(!newValue);
            httpUrl.setDisable(newValue);
            configuration.setBlockServiceType(newValue ? BlockServiceType.SERIAL : BlockServiceType.HTTP);
        });

        httpUrl.textProperty().bindBidirectional(configuration.httpUrlProperty());
    }
}
