package com.grelobites.romgenerator.util.arduino;

import com.grelobites.romgenerator.util.Util;
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
            serialPort.setRTS(false);
            Thread.sleep(50);
            serialPort.setDTR(true);
            serialPort.setRTS(true);
            Thread.sleep(50);
        } catch (Exception e) {
            LOGGER.error("Clearing serial port", e);
            throw new RuntimeException(e);
        }
    }

    private int readByte() {
        try {
            byte[] response = serialPort.readBytes(1);
            return response[0];
        } catch (Exception e) {
            throw new RuntimeException("Reading from serial port", e);
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
        int nextValue;
        while (true) {
            StringBuilder line = new StringBuilder();
            while ((nextValue = readByte()) != 10) { //Read to EOL
                if (nextValue >= 32 && nextValue <= 128) { //Append only ASCII
                    line.append((char) nextValue);
                }
            }
            String rawCommand = line.toString().trim();
            if (!rawCommand.isEmpty()) {
                LOGGER.debug("Got raw command {}", rawCommand);
                return new Command(rawCommand.substring(0, 1), rawCommand.substring(1));
            }
        }
    }

    public void upload(InputStream stream) throws IOException {
        upload(Util.fromInputStream(stream));
    }

    public void upload(byte[] data) {
        upload(data, null);
    }

    public void upload(byte[] data, ProgressListener progressListener) {
        clearSerialPort();
        purgeSerialPort();
        boolean finished = false;
        int errorCode = 0;
        String errorMessage = null;
        int totalBytes = data.length;
        int sentBytes = 0;
        while (!finished) {
            Command command = getNextCommand();
            LOGGER.debug("Processing command {}", command);
            switch (command.getCommand()) {
                case "S":
                    int numBytes = Integer.parseInt(command.getArgument());
                    byte[] chunk = new byte[numBytes];
                    System.arraycopy(data, sentBytes, chunk, 0, Math.min(numBytes, totalBytes - sentBytes));
                    if (totalBytes - sentBytes < numBytes) {
                        Arrays.fill(chunk, totalBytes - sentBytes, numBytes, (byte) 0xff);
                    }
                    sendBytes(chunk);
                    sentBytes += numBytes;
                    if (progressListener != null) {
                        progressListener.onProgressUpdate((1.0 * sentBytes) / totalBytes);
                    }
                    break;
                case "R":
                    LOGGER.debug("Got R command. Programming started");
                    break;
                case "Q":
                    String[] arguments = command.getArgument().split(",");
                    LOGGER.error("Got error {}, {}", arguments[0], arguments[1]);
                    try {
                        errorCode = Integer.parseInt(arguments[0]);
                    } catch (Exception e) {
                        LOGGER.warn("Unable to parse error code from arduino response {}", arguments[0], e);
                        errorCode = -1;
                    }
                    errorMessage = arguments[1];
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
        if (errorCode != 0) {
            throw new RuntimeException("During XSVF Upload. Code: "
                    + errorCode + ", message : " + errorMessage);
        }

    }
}
