package com.grelobites.romgenerator.util;

import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialPortUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialPortUtils.class);
    private static final int DELAY_RESET = 50;

    public static void clear(SerialPort serialPort) {
        try {
            /*
            serialPort.setDTR(false);
            serialPort.setRTS(false);
            Thread.sleep(DELAY_RESET);
            serialPort.setDTR(true);
            serialPort.setRTS(true);
            Thread.sleep(DELAY_RESET);
            */
            serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
        } catch (Exception e) {
            LOGGER.error("Clearing serial port", e);
            throw new RuntimeException(e);
        }
    }

    public static void purgeRxBuffer(SerialPort serialPort) {
        while (true) {
            try {
                serialPort.readBytes(1, 100);
            } catch (Exception e) {
                return;
            }
        }
    }

}
