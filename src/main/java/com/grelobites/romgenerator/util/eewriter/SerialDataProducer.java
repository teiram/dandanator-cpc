package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.util.Util;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class SerialDataProducer implements DataProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialDataProducer.class);
    private static EepromWriterConfiguration configuration = EepromWriterConfiguration
            .getInstance();
    private static final int SEND_BUFFER_SIZE = 1024;

    private int id;
    private SerialPort serialPort;
    private Runnable onFinalization;
    private Runnable onDataSent;
    private DoubleProperty progressProperty;
    private byte[] data;

    private void init() {
        progressProperty = new SimpleDoubleProperty(0.0);
    }


    public SerialDataProducer(SerialPort serialPort, int block, byte[] data) {
        this.serialPort = serialPort;
        this.id = block;
        init();
        setupBlockData(block, data);
    }

    public SerialDataProducer(SerialPort serialPort, byte[] rawData) {
        this.serialPort = serialPort;
        this.id = 0;
        init();
        data = rawData;
    }

    @Override
    public int id() {
        return id;
    }

    private void setupBlockData(int block, byte[] buffer) {
        int blockSize = configuration.getBlockSize();
        data = new byte[blockSize + 3];
        System.arraycopy(buffer, 0, data, 0, blockSize);

        data[blockSize] = Integer.valueOf(block).byteValue();

        Util.writeAsLittleEndian(data, blockSize + 1, Util.getBlockCrc16(data, blockSize + 1));
    }

    @Override
    public void send() {
        try {
            int sentBytesCount = 0;
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            byte[] sendBuffer = new byte[SEND_BUFFER_SIZE];
            while (sentBytesCount < data.length) {
                int count = bis.read(sendBuffer);
                LOGGER.debug("Sending block of " + count + " bytes");
                if (count < SEND_BUFFER_SIZE) {
                    serialPort.writeBytes(Arrays.copyOfRange(sendBuffer, 0, count));
                } else {
                    serialPort.writeBytes(sendBuffer);
                }
                sentBytesCount += count;
                if (onDataSent != null) {
                    Platform.runLater(onDataSent);
                }
                final double progress = 1.0 * sentBytesCount / data.length;
                Platform.runLater(() -> progressProperty.set(progress));
            }
        } catch (Exception e) {
            LOGGER.error("Exception during send process", e);
            throw new RuntimeException(e);
        } finally {
            Platform.runLater(onFinalization);
        }
    }

    @Override
    public void onFinalization(Runnable onFinalization) {
        this.onFinalization = onFinalization;
    }

    @Override
    public void onDataSent(Runnable onDataSent) {
        this.onDataSent = onDataSent;
    }

    @Override
    public DoubleProperty progressProperty() {
        return progressProperty;
    }

}
