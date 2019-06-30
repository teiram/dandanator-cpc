package com.grelobites.romgenerator.util.eewriter;

import javafx.application.Platform;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class SerialDataProducer extends DataProducerSupport implements DataProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialDataProducer.class);
    private static final int SEND_BUFFER_SIZE = 1024;

    private SerialPort serialPort;

    public SerialDataProducer(SerialPort serialPort, int block, byte[] data) {
        super(block, data);
        this.serialPort = serialPort;
        setupBlockData(block, data);
    }

    public SerialDataProducer(SerialPort serialPort, byte[] rawData) {
        super(rawData);
        this.serialPort = serialPort;
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

}
