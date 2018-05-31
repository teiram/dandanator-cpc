package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.CompressorFactory;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class DandanatorCpcConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorCpcConfiguration.class);

    private static final String DANDANATORROMPATH_PROPERTY = "dandanatorRomPath";
    private static final String EXTRAROMPATH_PROPERTY = "extraRomPath";
    private static final String TOGGLEPOKESMESSAGE_PROPERTY = "togglePokesMessage";
    private static final String EXTRAROMMESSAGE_PROPERTY = "extraRomMessage";
    private static final String LAUNCHGAMEMESSAGE_PROPERTY = "launchGameMessage";
    private static final String SELECTPOKESMESSAGE_PROPERTY = "selectPokesMessage";
    private static final String DANDANATORPICFIRMWAREPATH_PROPERTY = "dandanatorPicFirmwarePath";

    private StringProperty dandanatorRomPath;
    private StringProperty extraRomPath;
    private StringProperty togglePokesMessage;
    private StringProperty extraRomMessage;
    private StringProperty launchGameMessage;
    private StringProperty selectPokesMessage;
    private BooleanProperty autoboot;


    private byte[] dandanatorRom;
    private byte[] extraRom;

    private static Compressor compressor = CompressorFactory.getDefaultCompressor();

    private static DandanatorCpcConfiguration INSTANCE;

    private static void setPersistenceListenerOnPropertyChange(StringProperty property,
                                                               String configurationKey,
                                                               String defaultValue) {
        property.addListener(
                (observable, oldValue, newValue) -> {
                    if (!Constants.ROMSET_PROVIDED.equals(newValue)) {
                        Configuration.persistConfigurationValue(configurationKey,
                                newValue == null ? null : newValue.equals(defaultValue) ? null : newValue);
                    }
                });
    }

    public static DandanatorCpcConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = newInstance();
        }
        return INSTANCE;
    }


    private DandanatorCpcConfiguration() {
        dandanatorRomPath = new SimpleStringProperty();
        extraRomPath = new SimpleStringProperty();
        togglePokesMessage = new SimpleStringProperty();
        extraRomMessage = new SimpleStringProperty();
        launchGameMessage = new SimpleStringProperty();
        selectPokesMessage = new SimpleStringProperty();
        autoboot = new SimpleBooleanProperty(false);
    }

    private static boolean validConfigurationValue(String value) {
        return value != null && !Constants.ROMSET_PROVIDED.equals(value);
    }

    public String getDandanatorRomPath() throws IOException {
        return dandanatorRomPath.get();
    }

    public void setDandanatorRomPath(String dandanatorRomPath) {
        if (!Constants.ROMSET_PROVIDED.equals(dandanatorRomPath)) {
            dandanatorRom = null;
        }
        this.dandanatorRomPath.set(dandanatorRomPath);
    }

    public StringProperty dandanatorRomPathProperty() {
        return dandanatorRomPath;
    }

    public String getExtraRomPath() {
        return extraRomPath.get();
    }

    public void setExtraRomPath(String extraRomPath) {
        if (!Constants.ROMSET_PROVIDED.equals(extraRomPath)) {
            extraRom = null;
        }
        this.extraRomPath.set(extraRomPath);
    }

    public StringProperty extraRomPathProperty() {
        return extraRomPath;
    }

    public byte[] getDandanatorRom() throws IOException {
        if (dandanatorRom == null) {
            if (validConfigurationValue(dandanatorRomPath.get())) {
                try {
                    dandanatorRom = Files.readAllBytes(Paths.get(dandanatorRomPath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Dandanator ROM from " + dandanatorRomPath, e);
                    dandanatorRom = DandanatorCpcConstants.getDandanatorRom();
                }
            } else {
                dandanatorRom = DandanatorCpcConstants.getDandanatorRom();
            }
        }
        return dandanatorRom;
    }

    public void setDandanatorRom(byte[] dandanatorRom) {
        this.dandanatorRom = dandanatorRom;
    }


    public byte[] getExtraRom() throws IOException {
        if (extraRom == null) {
            if (validConfigurationValue(extraRomPath.get())) {
                try {
                    extraRom = Files.readAllBytes(Paths.get(extraRomPath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Extra ROM from " + extraRomPath, e);
                    extraRom = DandanatorCpcConstants.getExtraRom();
                }
            } else {
                extraRom = DandanatorCpcConstants.getExtraRom();
            }
        }
        return extraRom;
    }

    public void setExtraRom(byte[] extraRom) {
        this.extraRom = extraRom;
    }


    public String getTogglePokesMessage() {
        if (togglePokesMessage.get() == null) {
            return DandanatorCpcConstants.DEFAULT_TOGGLEPOKESKEY_MESSAGE;
        }
        return togglePokesMessage.get();
    }

    public void setTogglePokesMessage(String togglePokesMessage) {
        this.togglePokesMessage.set(togglePokesMessage);
    }

    public StringProperty togglePokesMessageProperty() {
        return togglePokesMessage;
    }

    public String getExtraRomMessage() {
        if (extraRomMessage.get() == null) {
            return DandanatorCpcConstants.DEFAULT_EXTRAROMKEY_MESSAGE;
        }
        return extraRomMessage.get();
    }

    public void setExtraRomMessage(String extraRomMessage) {
        this.extraRomMessage.set(extraRomMessage);
    }

    public StringProperty extraRomMessageProperty() {
        return extraRomMessage;
    }

    public boolean isAutoboot() {
        return autoboot.get();
    }

    public BooleanProperty autobootProperty() {
        return autoboot;
    }

    public void setAutoboot(boolean autoboot) {
        this.autoboot.set(autoboot);
    }

    public String getLaunchGameMessage() {
        if (launchGameMessage.get() == null) {
            return DandanatorCpcConstants.DEFAULT_LAUNCHGAME_MESSAGE;
        }
        return launchGameMessage.get();
    }

    public void setLaunchGameMessage(String launchGameMessage) {
        this.launchGameMessage.set(launchGameMessage);
    }

    public StringProperty launchGameMessageProperty() {
        return launchGameMessage;
    }

    public String getSelectPokesMessage() {
        if (selectPokesMessage.get() == null) {
            return DandanatorCpcConstants.DEFAULT_SELECTPOKE_MESSAGE;
        }
        return selectPokesMessage.get();
    }

    public StringProperty selectPokesMessageProperty() {
        return selectPokesMessage;
    }

    public void setSelectPokesMessage(String selectPokesMessage) {
        this.selectPokesMessage.set(selectPokesMessage);
    }

    public Compressor getCompressor() {
        return compressor;
    }

    @Override
    public String toString() {
        return "DandanatorCpcConfiguration{" +
                "dandanatorRomPath=" + dandanatorRomPath +
                ", extraRomPath=" + extraRomPath +
                ", togglePokesMessage=" + togglePokesMessage +
                ", extraRomMessage=" + extraRomMessage +
                ", launchGameMessage=" + launchGameMessage +
                ", selectPokesMessage=" + selectPokesMessage +
                ", dandanatorRom=" + dandanatorRom +
                ", extraRom=" + extraRom +
                '}';
    }

    private static DandanatorCpcConfiguration setFromPreferences(DandanatorCpcConfiguration configuration) {
        Preferences p = Configuration.getApplicationPreferences();
        //Do not use setters here, since they force the preferences to be reset again
        configuration.dandanatorRomPath.set(
                p.get(DANDANATORROMPATH_PROPERTY, null));
        configuration.extraRomPath.set(
                p.get(EXTRAROMPATH_PROPERTY, null));
        configuration.launchGameMessage.set(
                p.get(LAUNCHGAMEMESSAGE_PROPERTY, null));
        configuration.selectPokesMessage.set(
                p.get(SELECTPOKESMESSAGE_PROPERTY, null));
        configuration.extraRomMessage.set(
                p.get(EXTRAROMMESSAGE_PROPERTY, null));
        configuration.togglePokesMessage.set(
                p.get(TOGGLEPOKESMESSAGE_PROPERTY, null));
        return configuration;
    }

    synchronized private static DandanatorCpcConfiguration newInstance() {
        final DandanatorCpcConfiguration configuration = new DandanatorCpcConfiguration();
        //Avoid persistence so far, since the model is not clear yet
        //return setFromPreferences(configuration);
        return configuration;
    }
}
