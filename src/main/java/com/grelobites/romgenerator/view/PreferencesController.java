package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.CpcColor;
import com.grelobites.romgenerator.util.FontViewer;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.imageloader.ImageType;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class PreferencesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesController.class);

    private WritableImage backgroundImage;
    private FontViewer charSetImage;

    @FXML
    private ImageView backgroundImageView;

    @FXML
    private Button changeBackgroundImageButton;

    @FXML
    private Button resetBackgroundImageButton;

    @FXML
    private Pagination charSetPagination;

    private ImageView charSetImageView;

    @FXML
    private Button changeCharSetButton;

    @FXML
    private Button resetCharSetButton;

    @FXML
    private RadioButton tapeMode464;

    @FXML
    private RadioButton tapeMode6128;

    @FXML
    private ToggleGroup tapeLoaderToggleGroup;

    @FXML
    private CheckBox includeExtraRom;

    @FXML
    private CheckBox enforceFollowRomEnable;

    private void initializeImages() throws IOException {
        backgroundImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(), Constants.MENU_SCREEN_MODE,
                new ByteArrayInputStream(Configuration.getInstance().getBackgroundImage()));
        charSetImage = new FontViewer(256, 64);
        recreateCharSetImage();
    }

    private void recreateCharSetImage() throws IOException {
        charSetImage.setCharSet(Configuration.getInstance().getCharSet());
        charSetImage.setPen(CpcColor.BRIGHTWHITE);
        charSetImage.setInk(CpcColor.BLACK);
        charSetImage.clearScreen();
        charSetImage.printLine("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 3, 2);
        charSetImage.printLine("abcdefghijklmnopqrstuvwxyz", 1, 2);
        charSetImage.printLine("1234567890 !\"#$%&/()[]:;,.-_", 5, 2);
    }

    private void recreateBackgroundImage() throws IOException {
        LOGGER.debug("RecreateBackgroundImage");
        ImageUtil.scrLoader(backgroundImage, Constants.MENU_SCREEN_MODE,
                new ByteArrayInputStream(Configuration.getInstance().getBackgroundImage()));
    }

    private void updateBackgroundImage(File backgroundImageFile) throws IOException {
        if (ImageType.imageLoader(backgroundImageFile).isPresent()) {
            Configuration.getInstance().setBackgroundImagePath(backgroundImageFile.getAbsolutePath());
            recreateBackgroundImage();
        } else {
            throw new IllegalArgumentException("No valid background image file provided");
        }
    }

    private void updateCharSetPath(File charSetFile) throws IOException {
        if (isReadableFile(charSetFile) && charSetFile.length() == Constants.CHARSET_SIZE) {
            Configuration.getInstance().setCharSetPath(charSetFile.getAbsolutePath());
            recreateCharSetImage();
        } else {
            throw new IllegalArgumentException("No valid charset file provided");
        }
    }

    private void setTapeTargetMode(String hwMode) {
        switch (HardwareMode.valueOf(hwMode)) {
            case HW_CPC464:
                tapeLoaderToggleGroup.selectToggle(tapeMode464);
                break;
            case HW_CPC6128:
                tapeLoaderToggleGroup.selectToggle(tapeMode6128);
                break;
            default:
                LOGGER.warn("Invalid hardware mode selected as Tape target");
        }
    }

    private void tapeTargetModeSetup() {
        Configuration configuration = Configuration.getInstance();

        tapeMode464.setUserData(HardwareMode.HW_CPC464);
        tapeMode6128.setUserData(HardwareMode.HW_CPC6128);

        setTapeTargetMode(configuration.getTapeLoaderTarget());

        configuration.tapeLoaderTargetProperty().addListener((observable, oldValue, newValue) -> {
            setTapeTargetMode(newValue);
        });

        tapeLoaderToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue)  -> {
            LOGGER.debug("Changed Tape mode toggle to " + newValue);
            configuration.setTapeLoaderTarget(
                    ((HardwareMode) newValue.getUserData()).name());
        });
    }


    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }


    private static void showGenericFileErrorAlert() {
        DialogUtil.buildErrorAlert(LocaleUtil.i18n("fileImportError"),
                LocaleUtil.i18n("fileImportErrorHeader"),
                LocaleUtil.i18n("fileImportErrorContent"))
                .showAndWait();
    }

    private void backgroundImageSetup() {
        backgroundImageView.setImage(backgroundImage);
        changeBackgroundImageButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectNewBackgroundImage"));
            final File backgroundImageFile = chooser.showOpenDialog(changeBackgroundImageButton
                    .getScene().getWindow());
            if (backgroundImageFile != null) {
                try {
                    updateBackgroundImage(backgroundImageFile);
                } catch (Exception e) {
                    LOGGER.error("Updating background image from " + backgroundImageFile, e);
                    showGenericFileErrorAlert();
                }
            }
        });

        resetBackgroundImageButton.setOnAction(event -> {
            try {
                Configuration.getInstance().setBackgroundImagePath(null);
                recreateBackgroundImage();
            } catch (Exception e) {
                LOGGER.error("Resetting background Image", e);
            }
        });
        Configuration.getInstance().backgroundImagePathProperty().addListener(
                (observable, oldValue, newValue) -> {
                    try {
                        recreateBackgroundImage();
                    } catch (IOException ioe) {
                        LOGGER.error("Updating background image", ioe);
                    }
                });
    }

    private void charSetSetup() {
        Configuration configuration = Configuration.getInstance();
        charSetImageView = new ImageView();
        charSetImageView.setImage(charSetImage);
        charSetPagination.getStyleClass().add(Pagination.STYLE_CLASS_BULLET);
        //Disable the pagination when the charSetPath is externally provided
        charSetPagination.disableProperty().bind(configuration.charSetPathExternallyProvidedProperty());
        charSetPagination.setPageCount(configuration.getCharSetFactory().charSetCount());
        if (!configuration.getCharSetPathExternallyProvided()) {
            charSetPagination.setCurrentPageIndex(configuration.getInternalCharSetPathIndex());
        }
        charSetPagination.setPageFactory((index) -> {
            if (index < configuration.getCharSetFactory().charSetCount()) {
                return charSetImageView;
            } else {
                return null;
            }
        });

        charSetPagination.currentPageIndexProperty().addListener(
                (observable, oldValue, newValue) -> {
                    Configuration.getInstance().setCharSetPath(Configuration.INTERNAL_CHARSET_PREFIX + newValue);
                });

        changeCharSetButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectNewCharSetMessage"));
            final File charSetFile = chooser.showOpenDialog(changeCharSetButton.getScene().getWindow());
            if (charSetFile != null) {
                try {
                    updateCharSetPath(charSetFile);
                } catch (Exception e) {
                    LOGGER.error("Updating charset from " + charSetFile, e);
                    showGenericFileErrorAlert();
                }
            }
        });
        resetCharSetButton.setOnAction(event -> {
            try {
                Configuration.getInstance().setCharSetPath(null);
                charSetPagination.setCurrentPageIndex(0);
                recreateCharSetImage();
            } catch (Exception e) {
                LOGGER.error("Resetting charset", e);
            }
        });


        Configuration.getInstance().charSetPathProperty().addListener(
                (c) -> {
                    try {
                        recreateCharSetImage();
                    } catch (IOException ioe) {
                        LOGGER.error("Updating charset image", ioe);
                    }
                });

    }

    @FXML
    private void initialize() throws IOException {
        initializeImages();

        backgroundImageSetup();

        charSetSetup();

        tapeTargetModeSetup();

        includeExtraRom.selectedProperty().bindBidirectional(
                Configuration.getInstance().includeExtraRomProperty());
        enforceFollowRomEnable.selectedProperty().bindBidirectional(
                Configuration.getInstance().enforceFollowRomProperty());
    }
}
