package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.SerialPortConfiguration;
import com.grelobites.romgenerator.util.SerialPortUtils;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.view.EepromWriterController;
import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Future;

public class SerialBlockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialBlockService.class);
    private static final String SERVICE_THREAD_NAME = "SerialBlockService";
    private SerialPort serialPort;
    private Thread serviceThread;
    private Runnable onDataReceived;
    private final EepromWriterController controller;
    private boolean dandanatorReady = false;
    private boolean ignoreSyncRequest = false;
    private SerialPortConfiguration sendSerialPortConfiguration = SerialPortConfiguration.MODE_57600;
    private byte[] romsetByteArray;
    private static final int MARK_SYNC_57600 = 0x55;
    private static final int MARK_SYNC_115200 = 0xF0;
    private static final int MARK_EOC = 0xAA;

    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    private State state = State.STOPPED;

    public SerialBlockService(EepromWriterController controller) {
        this.controller = controller;
    }

    public void setOnDataReceived(Runnable onDataReceived) {
        this.onDataReceived = onDataReceived;
    }

    public void resetRomset() {
        this.romsetByteArray = null;
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

    private Optional<byte[]> getRomsetByteArray() {
        EepromWriterConfiguration configuration = EepromWriterConfiguration.getInstance();
        ApplicationContext applicationContext = controller.getApplicationContext();
        try {
            if (romsetByteArray == null) {
                if (configuration.getCustomRomSetPath() != null) {
                    try (FileInputStream fis = new FileInputStream(configuration.getCustomRomSetPath())) {
                        romsetByteArray = Util.fromInputStream(fis);
                    }
                } else {
                    if (applicationContext.getRomSetHandler().generationAllowedProperty().get()) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        applicationContext.getRomSetHandler().exportRomSet(bos);
                        romsetByteArray = bos.toByteArray();
                    } else {
                        LOGGER.info("Generation of romset byte array currently unavailable");
                    }
                }
            }
            return Optional.ofNullable(romsetByteArray);
        } catch (IOException ioe) {
            LOGGER.error("Trying to get Romset byte array", ioe);
            return Optional.empty();
        }
    }

    private Optional<DataProducer> getRomsetDataProducer(int slot) {
        int blockSize = EepromWriterConfiguration.getInstance().getBlockSize();
        byte[] buffer = new byte[blockSize];
        Optional<byte[]> romsetByteArray = getRomsetByteArray();
        if (romsetByteArray.isPresent()) {
            System.arraycopy(romsetByteArray.get(), slot * blockSize, buffer, 0, blockSize);
            return Optional.of(new SerialDataProducer(serialPort, slot, buffer));
        } else {
            return Optional.empty();
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
                        try {
                            Optional<DataProducer> dataProducer = getRomsetDataProducer(value);
                            if (dataProducer.isPresent()) {
                                Platform.runLater(() ->
                                        controller.bindDataProducer(dataProducer.get()));
                                sendSerialPortConfiguration.apply(serialPort);
                                dataProducer.get().send();
                                //Wait a bit to allow the buffer to be empty
                                Thread.sleep(500);
                                SerialPortConfiguration.MODE_57600.apply(serialPort);
                            } else {
                                LOGGER.error("Unable to send block {}", value);
                            }
                        } catch (Exception e) {
                            LOGGER.error("Sending block", e);
                        }
                        //Allow new sync requests to be ACK'd
                        ignoreSyncRequest = false;
                    } else {
                        LOGGER.warn("Received block request before SYNC (dandanator ready)");
                    }
                } else if (value == MARK_EOC) {
                    LOGGER.debug("Received end of communications message");
                    dandanatorReady = false;
                    Platform.runLater(() ->
                            controller.onCommunicationClosed());
                } else if (value == MARK_SYNC_57600) {
                    LOGGER.debug("Received 57600 SYNC");
                    syncAck();
                    sendSerialPortConfiguration = SerialPortConfiguration.MODE_57600;
                } else if (value == MARK_SYNC_115200) {
                    LOGGER.debug("Received 115200 SYNC");
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
