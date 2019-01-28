package com.grelobites.romgenerator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

public class EmulatorConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorConfiguration.class);

    private BooleanProperty testTapeStopConditions;
    private IntegerProperty tapeRemainingBytes;
    private BooleanProperty testPaletteChanges;
    private BooleanProperty testCrtcAccess;
    private BooleanProperty testVramWrites;
    private BooleanProperty testOnMotorStopped;
    private BooleanProperty testPsgAccess;
    private BooleanProperty testKeyboardReads;

    private static EmulatorConfiguration INSTANCE;

    private EmulatorConfiguration() {
        this.testTapeStopConditions = new SimpleBooleanProperty(true);
        this.tapeRemainingBytes = new SimpleIntegerProperty(5);
        this.testPaletteChanges = new SimpleBooleanProperty(true);
        this.testCrtcAccess = new SimpleBooleanProperty(true);
        this.testVramWrites = new SimpleBooleanProperty(true);
        this.testOnMotorStopped = new SimpleBooleanProperty(true);
        this.testPsgAccess = new SimpleBooleanProperty(true);
        this.testKeyboardReads = new SimpleBooleanProperty(true);
    }

    public static EmulatorConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    public boolean isTestTapeStopConditions() {
        return testTapeStopConditions.get();
    }

    public BooleanProperty testTapeStopConditionsProperty() {
        return testTapeStopConditions;
    }

    public void setTestTapeStopConditions(boolean testTapeStopConditions) {
        this.testTapeStopConditions.set(testTapeStopConditions);
    }

    public int getTapeRemainingBytes() {
        return tapeRemainingBytes.get();
    }

    public IntegerProperty tapeRemainingBytesProperty() {
        return tapeRemainingBytes;
    }

    public void setTapeRemainingBytes(int tapeRemainingBytes) {
        this.tapeRemainingBytes.set(tapeRemainingBytes);
    }

    public boolean isTestPaletteChanges() {
        return testPaletteChanges.get();
    }

    public BooleanProperty testPaletteChangesProperty() {
        return testPaletteChanges;
    }

    public void setTestPaletteChanges(boolean testPaletteChanges) {
        this.testPaletteChanges.set(testPaletteChanges);
    }

    public boolean isTestCrtcAccess() {
        return testCrtcAccess.get();
    }

    public BooleanProperty testCrtcAccessProperty() {
        return testCrtcAccess;
    }

    public void setTestCrtcAccess(boolean testCrtcAccess) {
        this.testCrtcAccess.set(testCrtcAccess);
    }

    public boolean isTestVramWrites() {
        return testVramWrites.get();
    }

    public BooleanProperty testVramWritesProperty() {
        return testVramWrites;
    }

    public void setTestVramWrites(boolean testVramWrites) {
        this.testVramWrites.set(testVramWrites);
    }

    public boolean isTestOnMotorStopped() {
        return testOnMotorStopped.get();
    }

    public BooleanProperty testOnMotorStoppedProperty() {
        return testOnMotorStopped;
    }

    public void setTestOnMotorStopped(boolean testOnMotorStopped) {
        this.testOnMotorStopped.set(testOnMotorStopped);
    }

    public boolean isTestPsgAccess() {
        return testPsgAccess.get();
    }

    public BooleanProperty testPsgAccessProperty() {
        return testPsgAccess;
    }

    public void setTestPsgAccess(boolean testPsgAccess) {
        this.testPsgAccess.set(testPsgAccess);
    }

    public boolean isTestKeyboardReads() {
        return testKeyboardReads.get();
    }

    public BooleanProperty testKeyboardReadsProperty() {
        return testKeyboardReads;
    }

    public void setTestKeyboardReads(boolean testKeyboardReads) {
        this.testKeyboardReads.set(testKeyboardReads);
    }

    public static Preferences getApplicationPreferences() {
        return Preferences.userNodeForPackage(EmulatorConfiguration.class);
    }

    private static EmulatorConfiguration setFromPreferences(EmulatorConfiguration configuration) {
        Preferences p = getApplicationPreferences();
        /*
        configuration.testTapeStopConditions.set(p.get(TAPELOADERTARGET_PROPERTY,
                DEFAULT_TAPELOADER_TARGET));
                */
        return configuration;
    }

    synchronized private static EmulatorConfiguration newInstance() {
        final EmulatorConfiguration configuration = new EmulatorConfiguration();
        return setFromPreferences(configuration);
    }
}
