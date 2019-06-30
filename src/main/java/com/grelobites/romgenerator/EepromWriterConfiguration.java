package com.grelobites.romgenerator;

import com.grelobites.romgenerator.util.eewriter.BlockServiceType;
import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

public class EepromWriterConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(EepromWriterConfiguration.class);

    private static final String DEFAULT_LOADER_BINARY = "/eewriter/eewriter.bin";
    private static final String ROMSET_LOADER_BINARY = "/eewriter/romset-eewriter.bin";
    private static final String SCREEN_RESOURCE = "/eewriter/screen.scr";

    private static final String SERIALPORT_PROPERTY = "serialPort";
    private static final int DEFAULT_BLOCKSIZE = 0x4000;

    private static final String DEFAULT_HTTP_URL = "http://dandanator.local";

    private StringProperty serialPort;
    private StringProperty customRomSetPath;
    private StringProperty httpUrl;
    private IntegerProperty blockSize;
    private ObjectProperty<BlockServiceType> blockServiceType;

    private static EepromWriterConfiguration INSTANCE;

    private EepromWriterConfiguration() {
        serialPort = new SimpleStringProperty(null);
        customRomSetPath = new SimpleStringProperty(null);
        blockSize = new SimpleIntegerProperty(DEFAULT_BLOCKSIZE);
        blockServiceType = new SimpleObjectProperty<>(BlockServiceType.SERIAL);
        httpUrl = new SimpleStringProperty(DEFAULT_HTTP_URL);
        serialPort.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                SERIALPORT_PROPERTY, newValue));
    }

    public static EepromWriterConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    public InputStream getLoaderStream() throws IOException {
        return EepromWriterConfiguration.class.getResourceAsStream(DEFAULT_LOADER_BINARY);
    }

    public InputStream getRomsetLoaderStream() throws IOException {
        return EepromWriterConfiguration.class.getResourceAsStream(ROMSET_LOADER_BINARY);
    }

    public InputStream getScreenStream() throws IOException {
        return EepromWriterConfiguration.class.getResourceAsStream(SCREEN_RESOURCE);
    }

    public String getSerialPort() {
        return serialPort.get();
    }

    public StringProperty serialPortProperty() {
        return serialPort;
    }

    public void setSerialPort(String serialPort) {
        this.serialPort.set(serialPort);
    }

    public String getCustomRomSetPath() {
        return customRomSetPath.get();
    }

    public StringProperty customRomSetPathProperty() {
        return customRomSetPath;
    }

    public void setCustomRomSetPath(String customRomSetPath) {
        this.customRomSetPath.set(customRomSetPath);
    }

    public int getBlockSize() {
        return blockSize.get();
    }

    public IntegerProperty blockSizeProperty() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize.set(blockSize);
    }

    public BlockServiceType getBlockServiceType() {
        return blockServiceType.get();
    }

    public ObjectProperty<BlockServiceType> blockServiceTypeProperty() {
        return blockServiceType;
    }

    public void setBlockServiceType(BlockServiceType blockServiceType) {
        this.blockServiceType.set(blockServiceType);
    }

    public String getHttpUrl() {
        return httpUrl.get();
    }

    public StringProperty httpUrlProperty() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl.set(httpUrl);
    }

    public static Preferences getApplicationPreferences() {
        return Preferences.userNodeForPackage(EepromWriterConfiguration.class);
    }

    public static void persistConfigurationValue(String key, String value) {
        LOGGER.debug("persistConfigurationValue " + key + ", " + value);
        Preferences p = getApplicationPreferences();
        if (value != null) {
            p.put(key, value);
        } else {
            p.remove(key);
        }
    }

    private static EepromWriterConfiguration setFromPreferences(EepromWriterConfiguration configuration) {
        Preferences p = getApplicationPreferences();
        configuration.serialPort.set(p.get(SERIALPORT_PROPERTY, null));
        return configuration;
    }

    synchronized private static EepromWriterConfiguration newInstance() {
        final EepromWriterConfiguration configuration = new EepromWriterConfiguration();
        return setFromPreferences(configuration);
    }
}
