package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.util.Util;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataProducerSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataProducerSupport.class);
    private static EepromWriterConfiguration configuration = EepromWriterConfiguration.getInstance();

    protected int id;
    protected Runnable onFinalization;
    protected Runnable onDataSent;
    protected DoubleProperty progressProperty;
    protected byte[] data;

    private void init() {
        progressProperty = new SimpleDoubleProperty(0.0);
    }


    public DataProducerSupport(int block, byte[] data) {
        this.id = block;
        init();
        setupBlockData(block, data);
    }

    public DataProducerSupport(byte[] rawData) {
        this.id = 0;
        init();
        data = rawData;
    }

    public int id() {
        return id;
    }

    protected void setupBlockData(int block, byte[] buffer) {
        int blockSize = configuration.getBlockSize();
        data = new byte[blockSize + 3];
        System.arraycopy(buffer, 0, data, 0, blockSize);

        data[blockSize] = Integer.valueOf(block).byteValue();

        Util.writeAsLittleEndian(data, blockSize + 1, Util.getBlockCrc16(data, blockSize + 1));
    }


    public void onFinalization(Runnable onFinalization) {
        this.onFinalization = onFinalization;
    }

    public void onDataChunkSent(Runnable onDataSent) {
        this.onDataSent = onDataSent;
    }

    public DoubleProperty progressProperty() {
        return progressProperty;
    }

}
