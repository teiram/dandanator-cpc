package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.LoaderConfiguration;
import com.grelobites.romgenerator.util.SerialPortConfiguration;
import com.grelobites.romgenerator.view.LoaderController;
import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SerialBlockService extends BlockServiceSupport implements BlockService  {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialBlockService.class);
    private SerialPort serialPort;
    private boolean dandanatorReady = false;
    private boolean ignoreSyncRequest = false;
    private SerialPortConfiguration sendSerialPortConfiguration = SerialPortConfiguration.MODE_57600;
    private static final int MARK_SYNC_57600 = 0x55;
    private static final int MARK_SYNC_115200 = 0xF0;
    private static final int MARK_EOC = 0xAA;

    public SerialBlockService(LoaderController controller, LoaderConfiguration configuration) {
        super(controller);
        LOGGER.debug("Creating serial port on {}", serialPort);
        this.serialPort = new SerialPort(configuration.getSerialPort());
    }

    @Override
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

    @Override
    public DataProducer getDataProducer(byte[] data) {
        return new SerialDataProducer(serialPort, data);
    }

    private Optional<DataProducer> getBlockDataProducer(int slot) {
        int blockSize = LoaderConfiguration.getInstance().getBlockSize();
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
                            Optional<DataProducer> dataProducer = getBlockDataProducer(value);
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
                    Platform.runLater(controller::onCommunicationClosed);
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
        try {
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
        } finally {
            close();
        }
        LOGGER.debug("Exiting SerialListener service thread");
        state = State.STOPPED;
    }

}
