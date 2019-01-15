package com.grelobites.romgenerator.arduino;

import jssc.SerialPort;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class UploadTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadTests.class);
    private static final String SERIAL_PORT = "/dev/ttyACM0";

    @Test
    public void uploadSketchTest() throws Exception {
        SerialPort serialPort = new SerialPort(SERIAL_PORT);
        serialPort.openPort();
        serialPort.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        Stk500Programmer programmer = new Stk500Programmer(serialPort);
        programmer.initialize();
        programmer.sync();
        int majorVersion = programmer.getMajorVersion();
        int minorVersion = programmer.getMinorVersion();
        LOGGER.debug("Major version is {}, minor is {}", majorVersion, minorVersion);


        byte[] signature = programmer.getDeviceSignature();
        LOGGER.debug("Signature is {}", Arrays.toString(signature));

        List<Binary> binaries = HexUtil.toBinaryList(
                UploadTests.class.getResourceAsStream("/hex/JTAGTest.hex"));

        LOGGER.debug("Got {} binaries from hex stream", binaries.size());
        if (binaries.size() > 0) {
            programmer.enterProgramMode();
            for (Binary binary : binaries) {
                programmer.programBinary(binary);
            }
            programmer.leaveProgramMode();
        }
    }

    @Test
    public void programJTagTest() throws Exception {
        SerialPort serialPort = new SerialPort(SERIAL_PORT);
        serialPort.openPort();
        serialPort.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        XsvfUploader uploader = new XsvfUploader(serialPort);
        uploader.upload(UploadTests.class.getResourceAsStream("/xsvf/dan-fix.xsvf"));
    }
}
