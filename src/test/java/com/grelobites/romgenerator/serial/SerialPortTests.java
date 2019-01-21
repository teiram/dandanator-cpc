package com.grelobites.romgenerator.serial;

import com.grelobites.romgenerator.util.Util;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SerialPortTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialPortTests.class);

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
            LOGGER.debug("Applying serial port configuration {}", this);
            serialPort.setParams(baudrate, dataBits, stopBits, parity);
            //SerialPortUtils.clear(serialPort);
        }

        @Override
        public String toString() {
            return "SerialPortConfiguration{" +
                    "baudrate=" + baudrate +
                    ", dataBits=" + dataBits +
                    ", stopBits=" + stopBits +
                    ", parity=" + parity +
                    '}';
        }
    }

    @Test
    public void sendWithSpeedChange() throws Exception {
        SerialPort serialPort = new SerialPort("/dev/ttyUSB1");
        serialPort.openPort();
        SerialPortConfiguration.MODE_57600.apply(serialPort);
        serialPort.writeString("A");
        SerialPortConfiguration.MODE_115200.apply(serialPort);
        serialPort.writeString("B");
        serialPort.closePort();

    }

    private static byte[] prepareBlock(byte[] romset, int slot) {
        int blockSize = 0x4000;
        byte[] result = new byte[blockSize + 3];
        System.arraycopy(romset, slot * 0x4000, result, 0, 0x4000);

        result[blockSize] = Integer.valueOf(slot).byteValue();

        Util.writeAsLittleEndian(result, blockSize + 1,
                Util.getBlockCrc16(result, blockSize + 1));
        return result;
    }

    @Test
    public void updateRomSet() throws Exception {
        SerialPort serialPort = new SerialPort("/dev/ttyUSB0");
        serialPort.openPort();
        SerialPortConfiguration.MODE_57600.apply(serialPort);
        boolean communicationFinished = false;
        boolean ignoreSyncRequest = false;
        boolean dandanatorReady = false;
        byte[] romset = Util.fromInputStream(
                SerialPortTests.class.getResourceAsStream("/romset/dandanator_2.0.rom"));
        SerialPortConfiguration sendSerialPortConfiguration = SerialPortConfiguration.MODE_57600;

        while (!communicationFinished) {
            try {
                byte[] data = serialPort.readBytes(1, 10000);
                int value = data[0] & 0xFF;
                if (value < 64) {
                    if (dandanatorReady) {
                        LOGGER.debug("Block {} Requested by serial port", value);
                        try {
                            sendSerialPortConfiguration.apply(serialPort);
                            serialPort.writeBytes(prepareBlock(romset,value));
                            SerialPortConfiguration.MODE_57600.apply(serialPort);
                        } catch (Exception e) {
                            LOGGER.error("Sending block", e);
                        }
                        //Allow new sync requests to be ACK'd
                        ignoreSyncRequest = false;
                    } else {
                        LOGGER.warn("Received block request before SYNC (dandanator ready)");
                    }
                } else if (value == 0xAA) {
                    LOGGER.debug("Received end of communications message");
                    dandanatorReady = false;
                    communicationFinished = true;
                } else if (value == 0x55) {
                    LOGGER.debug("Received 57600 SYNC");
                    if (!ignoreSyncRequest) {
                        LOGGER.debug("Dandanator ready to request data");
                        dandanatorReady = true;
                        ignoreSyncRequest = true;
                        serialPort.writeByte((byte) 0xFF);
                        sendSerialPortConfiguration = SerialPortConfiguration.MODE_57600;
                    }
                } else if (value == 0xF0) {
                    LOGGER.debug("Received 115200 SYNC");
                    if (!ignoreSyncRequest) {
                        LOGGER.debug("Dandanator ready to request data");
                        dandanatorReady = true;
                        ignoreSyncRequest = true;
                        serialPort.writeByte((byte) 0xFF);
                        sendSerialPortConfiguration = SerialPortConfiguration.MODE_115200;
                    }
                } else {
                    LOGGER.warn("Unexpected value {} received on serial port", value);
                }
            } catch (Exception e) {
                LOGGER.error("On reception", e);
            }
        }
    }
}
