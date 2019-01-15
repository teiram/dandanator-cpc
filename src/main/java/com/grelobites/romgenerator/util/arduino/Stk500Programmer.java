package com.grelobites.romgenerator.util.arduino;

import com.grelobites.romgenerator.util.Util;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Stk500Programmer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Stk500Programmer.class);

    private static final int PROGRAM_CHUNK_SIZE = 128;
    private static final int SERIAL_TIMEOUT = 500;
    private static final int RESP_STK_OK = 0x10;
    private static final int RESP_STK_INSYNC = 0x14;

    private static class ParametersBuilder {
        List<Byte> parameters = new ArrayList<>();

        public ParametersBuilder withByte(int value) {
            parameters.add(Integer.valueOf(value).byteValue());
            return this;
        }

        public ParametersBuilder withChar(char value) {
            parameters.add((byte) value);
            return this;
        }

        public ParametersBuilder withLittleEndianShort(int value) {
            parameters.add(Integer.valueOf(value & 0xff).byteValue());
            parameters.add(Integer.valueOf((value >> 8) & 0xff).byteValue());
            return this;
        }

        public ParametersBuilder withBigEndianShort(int value) {
            parameters.add(Integer.valueOf((value >> 8) & 0xff).byteValue());
            parameters.add(Integer.valueOf(value & 0xff).byteValue());
            return this;
        }

        public ParametersBuilder withByteArray(byte[] value) {
            for (byte item : value) {
                parameters.add(item);
            }
            return this;
        }

        public byte[] build() {
            byte[] result = new byte[parameters.size()];
            int index = 0;
            for (byte value : parameters) {
                result[index++] = value;
            }
            return result;
        }

        public static ParametersBuilder newInstance() {
            return new ParametersBuilder();
        }

    }
    private SerialPort serialPort;

    private void handleResponse() {
        try {
            byte response = readByte();
            if (response == RESP_STK_INSYNC) {
                LOGGER.debug("Got RESP_STK_INSYNC code");
                response = readByte();
                if (response != RESP_STK_OK) {
                    LOGGER.debug("Got non RESP_STK_OK response {}", response);
                    throw new RuntimeException("STK Operation returned error");
                }
            } else {
                LOGGER.warn("Got out of sync!");
                throw new RuntimeException("Sync lost");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte readByte() {
        try {
            byte[] response = serialPort.readBytes(1, SERIAL_TIMEOUT);
            return response[0];
        } catch (Exception e) {
            throw new RuntimeException("Reading from serial port", e);
        }
    }

    public void initialize() {
        try {
            serialPort.setDTR(false);
            serialPort.setRTS(false);
            Thread.sleep(50);
            serialPort.setDTR(true);
            serialPort.setRTS(true);
            Thread.sleep(50);
            //purgeSerialPort();
        } catch (Exception e) {
            LOGGER.error("Clearing serial port", e);
            throw new RuntimeException(e);
        }
    }

    private void purgeSerialPort() {
        while (true) {
            try {
                serialPort.readBytes(1, 500);
            } catch (Exception e) {
                return;
            }
        }
    }

    private byte[] readBytes(int count) {
        try {
            return serialPort.readBytes(count, SERIAL_TIMEOUT);
        } catch (Exception e) {
            throw new RuntimeException("Reading from serial port", e);
        }
    }

    private void sendCommand(byte[] command) {
        try {
            serialPort.writeBytes(command);
        } catch (Exception e) {
            LOGGER.error("In sendCommand", e);
            throw new RuntimeException(e);
        }
    }

    private void sendCommandAndHandleResponse(byte[] command) {
        sendCommand(command);
        handleResponse();
    }

    public Stk500Programmer(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    public void programBinary(Binary binary) throws IOException {
        byte[] data = binary.toByteArray();
        int chunks = (data.length + PROGRAM_CHUNK_SIZE - 1) / PROGRAM_CHUNK_SIZE;
        int address = binary.getAddress();
        for (int i = 0; i < chunks; i++) {
            byte[] chunkData = Arrays.copyOfRange(data, i * PROGRAM_CHUNK_SIZE,
                    Math.min((i + 1) * PROGRAM_CHUNK_SIZE, data.length));
            programChunk(address, chunkData);
            address += PROGRAM_CHUNK_SIZE >> 1;
        }
    }

    private void programChunk(int address, byte[] data) {
        LOGGER.debug("Programming chunk with address={}, data={}",
                String.format("0x%04x", address), Util.dumpAsHexString(data));
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(0x55)
                .withLittleEndianShort(address)
                .withByte(0x20).build());
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(0x64)
                .withBigEndianShort(data.length)
                .withChar('F')
                .withByteArray(data)
                .withByte(0x20)
                .build());
    }

    private void waitMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            LOGGER.warn("Thread interrupted on wait");
        }
    }
    public void sync() {
        byte[] syncCommand = ParametersBuilder.newInstance()
                .withByte(0x31)
                .withByte(0x20)
                .build();
        sendCommand(syncCommand);
        purgeSerialPort();
        waitMillis(100);
        sendCommand(syncCommand);
        purgeSerialPort();
        waitMillis(100);
        sendCommandAndHandleResponse(syncCommand);
    }

    public void enterProgramMode() {

        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(0x42) //Set device program parameters
                .withByte(0x86) //devicecode
                .withByte(0x00) //revision
                .withByte(0x00) //progtype
                .withByte(0x01) //parmode
                .withByte(0x01) //polling
                .withByte(0x01) //selftimed
                .withByte(0x01) //lockbytes
                .withByte(0x03) //fusebytes
                .withByte(0xFF) //flashpollval1
                .withByte(0xFF) //Flashpollval2
                .withByte(0xFF) //eeprompollval1
                .withByte(0xFF) //eeprompollval2
                .withByte(0x00) //pagesizehigh
                .withByte(0x80) //pagesizelow
                .withByte(0x04) //eepromsizehigh
                .withByte(0x00) //eepromsizelow
                .withByte(0x00) //flashsize4
                .withByte(0x00) //flashsize3
                .withByte(0x80) //flashsize2
                .withByte(0x00) //flashsize1
                .withByte(0x20)
                .build());
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(0x45) //Set device extended parameters
                .withByte(0x05) //Command size
                .withByte(0x04) //eeprompagesize
                .withByte(0xD7) //signalpagel
                .withByte(0xC2) //signalbs2
                .withByte(0x00) //resetdisable
                .withByte(0x20).build());

        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(0x50) //Enter program mode
                .withByte(0x20)
                .build());
    }

    public void leaveProgramMode() {
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(0x51)
                .withByte(0x20)
                .build());
    }

    private int readParameter(byte[] command) {
        sendCommand(command);
        byte response = readByte();
        if (response == RESP_STK_INSYNC) {
            LOGGER.debug("Got RESP_STK_INSYNC code");
            int value = readByte();
            LOGGER.debug("Got value {}", String.format("0x%02x", value));
            response = readByte();
            if (response != RESP_STK_OK) {
                LOGGER.debug("Got non RESP_STK_OK response {}", response);
                throw new RuntimeException("STK Operation returned error");
            }
            return value;
        } else {
            throw new IllegalStateException("Sync lost");
        }
    }

    public int getMajorVersion() {
        return readParameter(ParametersBuilder.newInstance()
                .withByte(0x41)
                .withByte(0x81)
                .withByte(0x20)
                .build());
    }

    public int getMinorVersion() {
        return readParameter(ParametersBuilder.newInstance()
                .withByte(0x41)
                .withByte(0x82)
                .withByte(0x20)
                .build());
    }

    public byte[] getDeviceSignature() {
        sendCommand(ParametersBuilder.newInstance()
                .withByte(0x75)
                .withByte(0x20)
                .build());
        byte response = readByte();
        if (response == RESP_STK_INSYNC) {
            LOGGER.debug("Got RESP_STK_INSYNC code");
            byte[] signature = readBytes(3);
            response = readByte();
            if (response != RESP_STK_OK) {
                LOGGER.debug("Got non RESP_STK_OK response {}", response);
                throw new RuntimeException("STK Operation returned error");
            }
            return signature;
        } else {
            throw new IllegalStateException("Sync lost");
        }
    }

}
