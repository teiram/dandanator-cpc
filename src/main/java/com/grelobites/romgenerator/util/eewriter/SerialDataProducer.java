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
    private static final String SERVICE_THREAD_NAME = "SerialDataProducerService";
    private static EepromWriterConfiguration configuration = EepromWriterConfiguration
            .getInstance();
    private static final int SEND_BUFFER_SIZE = 1024;

    private Thread serviceThread;
    private SerialPort serialPort;
    private Runnable onFinalization;
    private Runnable onDataSent;
    private DoubleProperty progressProperty;
    private byte[] data;
    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    private State state = State.STOPPED;

    private void init() {
        progressProperty = new SimpleDoubleProperty(0.0);
        serviceThread = new Thread(null, this::serialSendData, SERVICE_THREAD_NAME);
    }


    public SerialDataProducer(SerialPort serialPort, int block, byte[] data) {
        this.serialPort = serialPort;
        init();
        setupBlockData(block, data);
    }

    private void setupBlockData(int block, byte[] buffer) {
        int blockSize = configuration.getBlockSize();
        data = new byte[blockSize + 3];
        System.arraycopy(buffer, 0, data, 0, blockSize);

        data[blockSize] = Integer.valueOf(block).byteValue();

        Util.writeAsLittleEndian(data, blockSize + 1, Util.getBlockCrc16(data, blockSize + 1));
    }

    private void serialSendData() {
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
                if (state != State.RUNNING) {
                    LOGGER.debug("No more in running state");
                    break;
                }
            }

            if (state == State.RUNNING && onFinalization != null) {
                Platform.runLater(onFinalization);
            }
            state = State.STOPPED;
            LOGGER.debug("State is now STOPPED");
        } catch (Exception e) {
            LOGGER.error("Exception during send process", e);
            state = State.STOPPED;
        }
    }

    @Override
    public void send() {
        state = State.RUNNING;
        serviceThread.start();
    }

    @Override
    public void stop() {
        if (state == State.RUNNING) {
            state = State.STOPPING;
            LOGGER.debug("State changed to STOPPING");

            while (state != State.STOPPED) {
                try {
                    serviceThread.join();
                } catch (InterruptedException e) {
                    LOGGER.debug("Serial thread was interrupted", e);
                }
            }
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
