package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.view.PlayerController;
import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialListener.class);
    private static PlayerConfiguration configuration = PlayerConfiguration.getInstance();
    private static final String SERVICE_THREAD_NAME = "SerialPortListener";
    private SerialPort serialPort;
    private Thread serviceThread;
    private final PlayerController controller;

    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    private State state = State.STOPPED;

    public SerialListener(PlayerController controller) {
        this.controller = controller;
    }

    public void start() {
        this.serialPort =  new SerialPort(configuration.getSerialPort());
        this.serviceThread = new Thread(null, this::run, SERVICE_THREAD_NAME);
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
        if (data.length == 1) {
            int value = data[0] & 0xFF;
            if (value < 0xAA) {
                LOGGER.debug("Block {} Requested by serial port", value);
                Platform.runLater(() -> {
                    controller.setCurrentBlock(value);
                    controller.doPlayExternal();
                });
            } else if (value == 0xAA) {
                LOGGER.debug("Received end of communications message");
                Platform.runLater(() -> {
                    controller.doStopExternal();
                });
            } else {
                LOGGER.warn("Unexpected value {} received on serial port", value);
            }
        } else {
            LOGGER.warn("Unexpected byte array of length {} in serial port", data.length);
        }
    }

    public void run() {
        try {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_115200,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_2,
                    SerialPort.PARITY_NONE);
        } catch (Exception e) {
            LOGGER.error("Initializing Serial port", e);
            throw new RuntimeException(e);
        }
        state = State.RUNNING;
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
