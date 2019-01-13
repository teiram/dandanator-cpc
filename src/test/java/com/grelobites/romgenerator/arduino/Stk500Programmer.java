package com.grelobites.romgenerator.arduino;

import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Stk500Programmer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Stk500Programmer.class);

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
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte readByte() {
        try {
            byte[] response = serialPort.readBytes(1);
            return response[0];
        } catch (Exception e) {
            throw new RuntimeException("Reading from serial port", e);
        }
    }

    private byte[] readBytes(int count) {
        try {
            return serialPort.readBytes(count);
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

    public void programHexRecord(HexRecord record) {
        if (record.getType() == HexRecordType.DATA) {
            sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                    .withByte(0x55)
                    .withLittleEndianShort(record.getAddress())
                    .withByte(0x20).build());
            sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                    .withByte(0x64)
                    .withLittleEndianShort(record.getData().length)
                    .withChar('F')
                    .withByteArray(record.getData())
                    .withByte(0x20).build());
        }
    }

    public void sync() {
        boolean synced = false;
        int attempts = 3;
        while (!synced) {
            try {
                sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                        .withByte(0x31)
                        .withByte(0x20)
                        .build());
                synced = true;
            } catch (Exception e) {
                if (--attempts == 0) {
                    throw new RuntimeException("Trying to sync", e);
                }
            }
        }
    }

    public void enterProgramMode() {
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(0x50)
                .withByte(0x20)
                .build());
    }

    public void leaveProgramMode() {
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(0x51)
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
