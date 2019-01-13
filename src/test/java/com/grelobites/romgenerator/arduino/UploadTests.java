package com.grelobites.romgenerator.arduino;

import jssc.SerialPort;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class UploadTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadTests.class);
    private static final String SERIAL_PORT = "COM4";

    @Test
    public void uploadSketchTest() throws Exception {
        SerialPort serialPort = new SerialPort(SERIAL_PORT);
        serialPort.openPort();
        serialPort.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        Stk500Programmer programmer = new Stk500Programmer(serialPort);
        programmer.sync();
        byte[] signature = programmer.getDeviceSignature();
        LOGGER.debug("Signature is {}", Arrays.toString(signature));
        programmer.enterProgramMode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                UploadTests.class.getResourceAsStream("/hex/JTAGTest.hex")));
        String line;
        boolean hexEof = false;
        while ((line = br.readLine()) != null && !hexEof) {
            HexRecord record = HexRecord.fromLine(line);
            switch (record.getType()) {
                case DATA:
                    programmer.programHexRecord(record);
                    break;
                case EOF:
                    LOGGER.debug("Reached EOF HEX record");
                    hexEof = true;
                    break;
                default:
                    LOGGER.error("Unsupported HEX record");
            }
        }
        programmer.leaveProgramMode();
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
        uploader.upload(UploadTests.class.getResourceAsStream("/xsvf/test.xsvf"));
    }
}
