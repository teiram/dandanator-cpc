package com.grelobites.romgenerator.handlers.dandanatorcpc.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConfiguration;
import com.grelobites.romgenerator.handlers.dandanatorcpc.DandanatorCpcConstants;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;


public class DandanatorCpcPreferencesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorCpcPreferencesController.class);

    @FXML
    private TextField launchGamesMessage;

    @FXML
    private Button resetLaunchGamesMessage;

    @FXML
    private TextField togglePokesMessage;

    @FXML
    private Button resetTogglePokesMessage;

    @FXML
    private TextField selectPokesMessage;

    @FXML
    private Button resetSelectPokesMessage;

    @FXML
    private TextField extraRomMessage;

    @FXML
    private Button resetExtraRomMessage;

    @FXML
    private Label extraRomPath;

    @FXML
    private Button changeExtraRomButton;

    @FXML
    private Button resetExtraRomButton;

    private static String getRomFileName(String name) {
        int extensionLocation = name.lastIndexOf(".");
        return extensionLocation > 0 ? name.substring(0, extensionLocation) : name;
    }

    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private void updateExtraRom(File extraRomFile) {
        if (isReadableFile(extraRomFile) && extraRomFile.length() == Constants.SLOT_SIZE) {
            DandanatorCpcConfiguration.getInstance().setExtraRomPath(extraRomFile.getAbsolutePath());
            extraRomMessage.setText(getRomFileName(extraRomFile.getName()));
        } else {
            throw new IllegalArgumentException("Invalid ROM File provided");
        }
    }

    private static void showGenericFileErrorAlert() {
        DialogUtil.buildErrorAlert(LocaleUtil.i18n("fileImportError"),
                LocaleUtil.i18n("fileImportErrorHeader"),
                LocaleUtil.i18n("fileImportErrorContent"))
                .showAndWait();
    }

    private static void bindLabelToConfiguration(TextField textField,
                                                 StringProperty stringProperty,
                                                 int maxMessageLength) {
        textField.textProperty().bindBidirectional(stringProperty);
        textField.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null &&
                            newValue.length() > maxMessageLength) {
                        textField.setText(oldValue);
                    }
                });
    }

    private void setupMessageWithResetButton(TextField textField,
                                             StringProperty stringProperty,
                                             int maxMessageLength,
                                             Button resetButton,
                                             String defaultMessage) {
        bindLabelToConfiguration(textField, stringProperty, maxMessageLength);
        if (stringProperty.get() == null) {
            stringProperty.set(defaultMessage);
        }
        resetButton.setOnAction(event -> stringProperty.set(defaultMessage));

    }
    private void setupFileBasedParameter(Button changeButton,
                                         String changeMessage,
                                         Label pathLabel,
                                         StringProperty configurationProperty,
                                         Button resetButton,
                                         Consumer<File> consumer,
                                         Runnable resetAction) {
        pathLabel.textProperty().bindBidirectional(configurationProperty,
                new StringConverter<String>() {
                    @Override
                    public String toString(String object) {
                        LOGGER.debug("Executing toString on " + object);
                        if (object == null) {
                            return LocaleUtil.i18n("builtInMessage");
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

        resetButton.setOnAction(event -> {
            configurationProperty.set(null);
            if (resetAction != null) {
                resetAction.run();
            }
        });

    }
    @FXML
    private void initialize() throws IOException {
        setupMessageWithResetButton(launchGamesMessage, DandanatorCpcConfiguration.getInstance()
                        .launchGameMessageProperty(),
                DandanatorCpcConstants.LAUNCH_GAME_MESSAGE_MAXLENGTH,
                resetLaunchGamesMessage,
                DandanatorCpcConstants.DEFAULT_LAUNCHGAME_MESSAGE);

        setupMessageWithResetButton(togglePokesMessage, DandanatorCpcConfiguration.getInstance()
                        .togglePokesMessageProperty(),
                DandanatorCpcConstants.TOGGLE_POKES_MESSAGE_MAXLENGTH,
                resetTogglePokesMessage,
                DandanatorCpcConstants.DEFAULT_TOGGLEPOKESKEY_MESSAGE);

        setupMessageWithResetButton(selectPokesMessage, DandanatorCpcConfiguration.getInstance()
                        .selectPokesMessageProperty(),
                DandanatorCpcConstants.SELECT_POKE_MESSAGE_MAXLENGTH,
                resetSelectPokesMessage,
                DandanatorCpcConstants.DEFAULT_SELECTPOKE_MESSAGE);
        setupMessageWithResetButton(extraRomMessage, DandanatorCpcConfiguration.getInstance()
                        .extraRomMessageProperty(),
                DandanatorCpcConstants.EXTRA_ROM_MESSAGE_MAXLENGTH,
                resetExtraRomMessage,
                DandanatorCpcConstants.DEFAULT_EXTRAROMKEY_MESSAGE);

        setupFileBasedParameter(changeExtraRomButton,
                LocaleUtil.i18n("selectExtraRomMessage"),
                extraRomPath,
                DandanatorCpcConfiguration.getInstance().extraRomPathProperty(),
                resetExtraRomButton,
                this::updateExtraRom,
                () -> {
                    extraRomMessage.setText(DandanatorCpcConstants.DEFAULT_EXTRAROMKEY_MESSAGE);
                });

    }
}
