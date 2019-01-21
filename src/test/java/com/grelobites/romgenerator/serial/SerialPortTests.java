package com.grelobites.romgenerator.serial;

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
}
