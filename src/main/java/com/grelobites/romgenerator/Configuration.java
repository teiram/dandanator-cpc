package com.grelobites.romgenerator;

import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.util.CharSetFactory;
import com.grelobites.romgenerator.util.RamGameCompressor;
import com.grelobites.romgenerator.util.imageloader.ImageLoader;
import com.grelobites.romgenerator.util.imageloader.ImageType;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.prefs.Preferences;

public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static final String MODE_PROPERTY = "mode";
    private static final String BACKGROUNDIMAGEPATH_PROPERTY = "backgroundImagePath";
    private static final String CHARSETPATH_PROPERTY = "charSetPath";
    private static final String TAPELOADERTARGET_PROPERTY = "tapeLoaderTarget";
    public static final String INTERNAL_CHARSET_PREFIX= "internal://";
    private static final String DEFAULT_MODE = RomSetHandlerType.DDNTR_V1.name();
    private static final String DEFAULT_TAPELOADER_TARGET = HardwareMode.HW_CPC464.name();

    byte[] charSet;
    byte[] backgroundImage;

    private StringProperty mode;
    private StringProperty backgroundImagePath;
    private StringProperty charSetPath;
    private BooleanProperty charSetPathExternallyProvided;
    private CharSetFactory charSetFactory;
    private RamGameCompressor ramGameCompressor;
    private StringProperty tapeLoaderTarget;

    private static Configuration INSTANCE;

    private static boolean isInternalCharSetPath(String value) {
        return value == null || value.startsWith(Configuration.INTERNAL_CHARSET_PREFIX);
    }

    private static boolean isCharSetExternallyProvided(String value) {
        return Constants.ROMSET_PROVIDED.equals(value) || !isInternalCharSetPath(value);
    }

    private Configuration() {
        this.backgroundImagePath = new SimpleStringProperty();
        this.charSetPath = new SimpleStringProperty();
        this.charSetPathExternallyProvided = new SimpleBooleanProperty();
        this.mode = new SimpleStringProperty(DEFAULT_MODE);

        this.charSetPath.addListener((observable, oldValue, newValue) ->
                charSetPathExternallyProvided.set(
                        isCharSetExternallyProvided(newValue)));
        this.charSetFactory = new CharSetFactory();
        this.tapeLoaderTarget = new SimpleStringProperty(DEFAULT_TAPELOADER_TARGET);
        tapeLoaderTarget.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                TAPELOADERTARGET_PROPERTY, newValue));
    }

    public static Configuration getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    public CharSetFactory getCharSetFactory() {
        return charSetFactory;
    }

    private static boolean validConfigurationValue(String value) {
        return value != null && !Constants.ROMSET_PROVIDED.equals(value);
    }


    public String getMode() {
        if (mode.get() == null) {
            return DEFAULT_MODE;
        } else {
            return mode.get();
        }
    }

    public void setMode(String mode) {
        this.mode.set(mode);
    }

    public StringProperty modeProperty() {
        return mode;
    }

    public String getBackgroundImagePath() {
        return backgroundImagePath.get();
    }

    public StringProperty backgroundImagePathProperty() {
        return backgroundImagePath;
    }

    public void setBackgroundImagePath(String backgroundImagePath) {
        //Invalidate the background image in advance, to avoid the listeners to
        //enter before the property is set to null
        if (!Constants.ROMSET_PROVIDED.equals(backgroundImagePath)) {
            backgroundImage = null;
        }

        this.backgroundImagePath.set(backgroundImagePath);
    }

    public byte[] getBackgroundImage() throws IOException {
        if (backgroundImage == null) {
            if (validConfigurationValue(backgroundImagePath.get())) {
                try {
                    File imageFile = new File(backgroundImagePath.get());
                    Optional<ImageLoader> loader = ImageType.imageLoader(imageFile);
                    if (loader.isPresent()) {
                        backgroundImage = loader.get().asByteArray(imageFile);
                    } else {
                        backgroundImage = Constants.getDefaultMenuScreen();
                    }
                } catch (Exception e) {
                    LOGGER.error("Unable to load Background Image from  " + backgroundImagePath.get(), e);
                    backgroundImage = Constants.getDefaultMenuScreen();
                }
            } else {
                backgroundImage = Constants.getDefaultMenuScreen();
            }
        }
        return backgroundImage;
    }

    public void setBackgroundImage(byte[] backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public int getInternalCharSetPathIndex() {
        if (getCharSetPath() != null) {
            return Integer.parseInt(getCharSetPath().substring(INTERNAL_CHARSET_PREFIX.length()));
        } else {
            return 0;
        }
    }

    public byte[] getCharSet() throws IOException {
        if (charSet == null) {
            if (isInternalCharSetPath(getCharSetPath())) {
                return charSetFactory.getCharSetAt(getInternalCharSetPathIndex());
            } else {
                if (validConfigurationValue(getCharSetPath())) {
                    try {
                        charSet = Files.readAllBytes(Paths.get(charSetPath.get()));
                    } catch (Exception e) {
                        LOGGER.error("Unable to load CharSet from " + charSetPath, e);
                        charSet = Constants.getDefaultCharset();
                    }
                } else {
                    charSet = Constants.getDefaultCharset();
                }
            }
        }
        return charSet;
    }

    public void setCharSet(byte[] charSet) {
        this.charSet = charSet;
    }

    public String getCharSetPath() {
        return charSetPath.get();
    }

    public StringProperty charSetPathProperty() {
        return charSetPath;
    }

    public BooleanProperty charSetPathExternallyProvidedProperty() {
        return charSetPathExternallyProvided;
    }

    public boolean getCharSetPathExternallyProvided() {
        return charSetPathExternallyProvided.get();
    }

    public void setCharSetPath(String charSetPath) {
        if (!Constants.ROMSET_PROVIDED.equals(charSetPath)) {
            charSet = null;
        }
        this.charSetPath.set(charSetPath);
    }

    public RamGameCompressor getRamGameCompressor() {
        return ramGameCompressor;
    }

    public void setRamGameCompressor(RamGameCompressor ramGameCompressor) {
        this.ramGameCompressor = ramGameCompressor;
    }

    public String getTapeLoaderTarget() {
        return tapeLoaderTarget.get();
    }

    public StringProperty tapeLoaderTargetProperty() {
        return tapeLoaderTarget;
    }

    public void setTapeLoaderTarget(String tapeLoaderTarget) {
        this.tapeLoaderTarget.set(tapeLoaderTarget);
        persistConfigurationValue(TAPELOADERTARGET_PROPERTY, tapeLoaderTarget);
    }

    public static Preferences getApplicationPreferences() {
        return Preferences.userNodeForPackage(Configuration.class);
    }

    public static void persistConfigurationValue(String key, String value) {
        LOGGER.debug("persistConfigurationValue " + key + ", " + value);
        if (!Constants.ROMSET_PROVIDED.equals(value)) {
            Preferences p = getApplicationPreferences();
            if (value != null) {
                p.put(key, value);
            } else {
                p.remove(key);
            }
        }
    }

    private static Configuration setFromPreferences(Configuration configuration) {
        Preferences p = getApplicationPreferences();
        configuration.tapeLoaderTarget.set(p.get(TAPELOADERTARGET_PROPERTY,
                DEFAULT_TAPELOADER_TARGET));
        return configuration;
    }

    synchronized private static Configuration newInstance() {
        final Configuration configuration = new Configuration();
        return setFromPreferences(configuration);
    }
}
