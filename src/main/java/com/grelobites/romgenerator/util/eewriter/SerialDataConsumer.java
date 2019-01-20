package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.view.EepromWriterController;
import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortException;
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
    private SerialPortConfiguration sendSerialPortConfiguration = SerialPortConfiguration.MODE_57600;

    private static final int MARK_SYNC_57600 = 0x55;
    private static final int MARK_SYNC_115200 = 0xF0;
    private static final int MARK_EOC = 0xAA;

    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    private State state = State.STOPPED;

    private enum SerialPortConfiguration {
        MODE_115200(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_2,
                SerialPort.PARITY_NONE),
        MODE_57600(SerialPort.BAUDRATE_57600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_2,
                SerialPort.PARITY_NONE);

        public int baudrate;
        public int dataBits;
        public int stopBits;
        public int parity;

        SerialPortConfiguration(int baudrate, int dataBits, int stopBits, int parity) {
            this.baudrate = baudrate;
            this.dataBits = dataBits;
            this.stopBits = stopBits;
            this.parity = parity;
        }

        public void apply(SerialPort serialPort) throws SerialPortException {
            serialPort.setParams(baudrate, dataBits, stopBits, parity);
        }
    }

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

    private void syncAck() {
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
            LOGGER.debug("Discarded SYNC request");
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
                            try {
                                controller.setCurrentBlock(value);
                                sendSerialPortConfiguration.apply(serialPort);
                                controller.doPlayExternal();
                                SerialPortConfiguration.MODE_57600.apply(serialPort);
                            } catch (Exception e) {
                                LOGGER.error("Sending block", e);
                            }
                        });
                        //Allow new sync requests to be ACK'd
                        ignoreSyncRequest = false;
                    } else {
                        LOGGER.warn("Received block request before SYNC (dandanator ready)");
                    }
                } else if (value == MARK_EOC) {
                    LOGGER.debug("Received end of communications message");
                    dandanatorReady = false;
                    Platform.runLater(() -> {
                        controller.doStopExternal();
                    });
                } else if (value == MARK_SYNC_57600) {
                    syncAck();
                    sendSerialPortConfiguration = SerialPortConfiguration.MODE_57600;
                } else if (value == MARK_SYNC_115200) {
                    syncAck();
                    sendSerialPortConfiguration = SerialPortConfiguration.MODE_115200;
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
            SerialPortConfiguration.MODE_57600.apply(serialPort);
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
