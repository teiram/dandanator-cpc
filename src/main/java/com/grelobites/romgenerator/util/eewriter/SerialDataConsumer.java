package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.view.EepromWriterController;
import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialDataConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialDataConsumer.class);
    private static final String SERVICE_THREAD_NAME = "SerialDataConsumer";
    private SerialPort serialPort;
    private Thread serviceThread;
    private Runnable onDataReceived;
    private final EepromWriterController controller;
    private boolean dandanatorReady = false;
    private boolean ignoreSyncRequest = false;

    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    private State state = State.STOPPED;

    public SerialDataConsumer(EepromWriterController controller) {
        this.controller = controller;
    }

    public void setOnDataReceived(Runnable onDataReceived) {
        this.onDataReceived = onDataReceived;
    }

    public void start(String serialPort) {
        LOGGER.debug("Creating serial port on {}", serialPort);
        this.serialPort =  new SerialPort(serialPort);
        this.serviceThread = new Thread(null, this::run, SERVICE_THREAD_NAME);
        this.serviceThread.setDaemon(true);
        this.serviceThread.start();
    }

    public void stop() {
        if (state == State.RUNNING) {
            state = State.STOPPING;
            while (state != State.STOPPED) {
                try {
                    serviceThread.join();
                } catch (InterruptedException e) {
                    LOGGER.debug("Interrupted while waiting for Serial listener to stop", e);
                }
            }
        }
    }

    public void close() {
        if (serialPort != null) {
            if (serialPort.isOpened()) {
                try {
                    serialPort.closePort();
                } catch (Exception e) {
                    LOGGER.error("Closing serial port", e);
                }
            }
        }
    }

    private void handleIncomingData(byte[] data) {
        if (data.length > 0) {
            if (onDataReceived != null) {
                Platform.runLater(onDataReceived);
            }
            if (data.length == 1) {
                int value = data[0] & 0xFF;
                if (value < 64) {
                    if (dandanatorReady) {
                        LOGGER.debug("Block {} Requested by serial port", value);

                        Platform.runLater(() -> {
                            controller.setCurrentBlock(value);
                            controller.doPlayExternal();
                        });
                        //Allow new sync requests to be ACK'd
                        ignoreSyncRequest = false;
                    } else {
                        LOGGER.warn("Received block request before 0x55 (dandanator ready)");
                    }
                } else if (value == 0xAA) {
                    LOGGER.debug("Received end of communications message");
                    dandanatorReady = false;
                    Platform.runLater(() -> {
                        controller.doStopExternal();
                    });
                } else if (value == 0x55) {
                    if (!ignoreSyncRequest) {
                        LOGGER.debug("Dandanator ready to request data");
                        dandanatorReady = true;
                        ignoreSyncRequest = true;
                        try {
                            serialPort.writeByte((byte) 0xFF);
                        } catch (Exception e) {
                            LOGGER.error("Trying to ACK dandanator", e);

                        }
                    } else {
                        LOGGER.debug("Discarded 0x55 request");
                    }
                } else {
                    LOGGER.warn("Unexpected value {} received on serial port", value);
                }
            } else {
                LOGGER.warn("Unexpected byte array of length {} in serial port", data.length);
            }
        }
    }

    public void run() {
        try {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_57600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_2,
                    SerialPort.PARITY_NONE);
        } catch (Exception e) {
            LOGGER.error("Initializing Serial port", e);
            throw new RuntimeException(e);
        }
        state = State.RUNNING;
        dandanatorReady = false;
        ignoreSyncRequest = false;
        while (state == State.RUNNING) {
            try {
                handleIncomingData(serialPort.readBytes(1, 1000));
            } catch (SerialPortTimeoutException spte) {
            } catch (Exception e) {
                LOGGER.error("Trying to read from serial port", e);
                state = State.STOPPING;
            }

        }
        LOGGER.debug("Exiting SerialListener service thread");
        state = State.STOPPED;
    }

    public SerialPort serialPort() {
        return serialPort;
    }
}
