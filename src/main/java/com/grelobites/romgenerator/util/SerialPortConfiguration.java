package com.grelobites.romgenerator.util;

import com.sun.javafx.PlatformUtil;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum SerialPortConfiguration {
    MODE_115200(114200, //Due to 464 clock jitter
            SerialPort.BAUDRATE_115200,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_2,
            SerialPort.PARITY_NONE),
    MODE_57600(SerialPort.BAUDRATE_57600,
            SerialPort.BAUDRATE_57600,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_2,
            SerialPort.PARITY_NONE);
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialPortConfiguration.class);

    public int baudrate;
    public int baudrateLinux;
    public int dataBits;
    public int stopBits;
    public int parity;

    SerialPortConfiguration(int baudrate, int baudrateLinux, int dataBits, int stopBits, int parity) {
        this.baudrate = baudrate;
        this.baudrateLinux = baudrateLinux;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    public void apply(SerialPort serialPort) throws SerialPortException {
        LOGGER.debug("Applying serial port configuration {}", this);
        boolean linux = PlatformUtil.isLinux();
        serialPort.setParams(linux ? baudrateLinux : baudrate, dataBits, stopBits, parity);
    }
    @Override
    public String toString() {
        return "SerialPortConfiguration{" +
                "baudrate=" + baudrate +
                ", baudrateLinux=" + baudrateLinux +
                ", dataBits=" + dataBits +
                ", stopBits=" + stopBits +
                ", parity=" + parity +
                '}';
    }
}
