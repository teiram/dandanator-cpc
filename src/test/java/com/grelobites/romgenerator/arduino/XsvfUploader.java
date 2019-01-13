package com.grelobites.romgenerator.arduino;

import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class XsvfUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(XsvfUploader.class);

    private final SerialPort serialPort;

    public XsvfUploader(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    private void clearSerialPort() {
        try {
            serialPort.setDTR(false);
            serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
            serialPort.setDTR(true);
        } catch (Exception e) {
            LOGGER.error("Clearing serial port", e);
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

    private void sendBytes(byte[] value) {
        try {
            serialPort.writeBytes(value);
        } catch (Exception e) {
            throw new RuntimeException("Writing to serial port", e);
        }
    }

    Command getNextCommand() {
        String command = new String(readBytes(1));
        StringBuilder argument = new StringBuilder();
        int nextValue;
        while ((nextValue = readByte()) != 0x10) {
            argument.append((char) nextValue);
        }
        return new Command(command, argument.toString().trim());
    }

    public void upload(InputStream stream) throws IOException {
        clearSerialPort();
        boolean finished = false;
        while (!finished) {
            Command command = getNextCommand();
            LOGGER.debug("Processing command {}", command);
            switch (command.getCommand()) {
                case "S":
                    int numBytes = Integer.parseInt(command.getArgument());
                    byte[] chunk = new byte[numBytes];
                    int bytesRead = stream.read(chunk, 0, numBytes);
                    Arrays.fill(chunk, bytesRead, numBytes, (byte) 0xff);
                    sendBytes(chunk);
                    break;
                case "R":
                    LOGGER.debug("Got R command.");
                    break;
                case "Q":
                    String[] arguments = command.getArgument().split(",");
                    LOGGER.error("Got error {}, {}", arguments[0], arguments[1]);
                    finished = true;
                    break;
                case "D":
                    LOGGER.info("Device: {}", command.getArgument());
                    break;
                case "!":
                    LOGGER.info("Important: {}", command.getArgument());
                    break;
                default:
                    LOGGER.info("Unrecognized command {}", command);
            }
        }

    }
}
