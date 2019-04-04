package com.grelobites.romgenerator.util;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum SerialPortConfiguration {
    MODE_115200(114000, //Due to 464 clock jitter
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_2,
            SerialPort.PARITY_NONE),
    MODE_57600(SerialPort.BAUDRATE_57600,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_2,
            SerialPort.PARITY_NONE);
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialPortConfiguration.class);

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
