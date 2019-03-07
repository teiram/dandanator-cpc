package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatorcpc.RomSetUtil;
import com.grelobites.romgenerator.handlers.dandanatorcpc.v1.GameHeaderV1Serializer;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.SnapshotGame;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SerialGameUploader implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialGameUploader.class);
    private static final int LAUNCH_CODE_TRAILER_SIZE = 34;
    private static final byte[] LAUNCHER_DATA = new byte[]{(byte) 0x00, (byte) 0xC0, (byte) 0x00};
    private static final int SEND_BUFFER_SIZE = 2048;

    private String serialPortName;
    private SerialPort serialPort;
    private SnapshotGame game;

    public SerialGameUploader(SnapshotGame game, String serialPortName) {
        this.game = game;
        this.serialPortName = serialPortName;
    }

    private void initSerialPort() {
        if (serialPort == null) {
            serialPort = new SerialPort(serialPortName);
        }
        if (!serialPort.isOpened()) {
            try {
                serialPort.openPort();
            } catch (SerialPortException spe) {
                LOGGER.error("Opening serial port", spe);
                throw new RuntimeException("Opening serial port", spe);
            }
        }
    }

    private void closeSerialPort() {
        if (serialPort != null) {
            if (serialPort.isOpened()) {
                try {
                    serialPort.closePort();
                } catch (SerialPortException spe) {
                    LOGGER.warn("Closing serial port", spe);
                }
            }
        }
    }

    /*
     * Fills 34 bytes (31 bytes of code + 3 bytes of DATA)
    */
    private static void dumpGameLaunchCodeTrailer(OutputStream os, SnapshotGame game) throws IOException {
        ByteBuffer launchCode = ByteBuffer.allocate(LAUNCH_CODE_TRAILER_SIZE);
        GameHeader header = game.getGameHeader();
        launchCode.put(Z80Opcode.LD_SP_NN(header.getSp()));
        launchCode.put(Z80Opcode.LD_BC_NN(header.getBcRegister()));
        launchCode.put(Z80Opcode.PUSH_BC);
        launchCode.put(Z80Opcode.POP_AF);
        launchCode.put(Z80Opcode.LD_BC_NN(header.getPc()));
        launchCode.put(Z80Opcode.PUSH_BC);
        launchCode.put(Z80Opcode.LD_BC_NN(header.getBcRegister()));
        launchCode.put(Z80Opcode.LD_DE_NN(header.getDeRegister()));
        launchCode.put(Z80Opcode.LD_HL_NN(header.getHlRegister()));
        launchCode.put(Z80Opcode.LD_IX_NN(header.getIxRegister()));
        launchCode.put(Z80Opcode.LD_IY_NN(header.getIyRegister()));
        launchCode.put(header.getIff0() == 0 ? Z80Opcode.DI : Z80Opcode.EI);
        launchCode.put(Z80Opcode.RET);
        launchCode.put(LAUNCHER_DATA);
        os.write(launchCode.array());
    }

    private void send(byte[] data) {
        LOGGER.debug("Sending block of data with length {}", data.length);
        try {
            SerialPortConfiguration.MODE_115200.apply(serialPort);

            int sentBytesCount = 0;
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            byte[] sendBuffer = new byte[SEND_BUFFER_SIZE];
            while (sentBytesCount < data.length) {
                int count = bis.read(sendBuffer);
                LOGGER.debug("Sending block of " + count + " bytes");
                if (count < SEND_BUFFER_SIZE) {
                    serialPort.writeBytes(Arrays.copyOfRange(sendBuffer, 0, count));
                } else {
                    serialPort.writeBytes(sendBuffer);
                }
                sentBytesCount += count;
            }
        } catch (Exception e) {
            throw new RuntimeException("Game Uploader", e);
        }
    }

    private void dumpGameLaunchCode(OutputStream os, SnapshotGame game) throws IOException {
        os.write(Constants.getUsbLaunchcodeHeader());
        dumpGameLaunchCodeTrailer(os, game);
    }

    private void prepareFirstBlock(OutputStream os) throws IOException {
        GameHeaderV1Serializer.serialize(game, os);     //  90 bytes
        os.write(game.getType().typeId());              //  1 byte
        os.write(RomSetUtil.getGameChunk(game));        //  32 bytes
        os.write(Constants.B_00);                       //  1 byte
        os.write(Constants.B_00);                       //  1 byte
        os.write(0); //Upper and lower active roms.      1 byte
        os.write(game.getCurrentRasterInterrupt());     //  1 byte
        dumpGameLaunchCode(os, game);                   //  466 + 31 + 3 = 500 bytes
                                                        //T:  627 bytes
        os.write(game.getSlot(game.getScreenSlot()));
    }

    public void run() {
        try {
            initSerialPort();
            ByteArrayOutputStream firstBlock = new ByteArrayOutputStream();
            int blocksSent = 0;
            //Send screen + launch code
            prepareFirstBlock(firstBlock);
            send(firstBlock.toByteArray());
            blocksSent++;
            //Start listening for commands
            serialPort.openPort();
            SerialPortConfiguration.MODE_57600.apply(serialPort);
            while (blocksSent < 4) {
                byte[] command = serialPort.readBytes(1, 5000);
                switch (Byte.toUnsignedInt(command[0])) {
                    case 32:
                        send(game.getSlot(0));
                        blocksSent++;
                        break;
                    case 33:
                        send(game.getSlot(1));
                        blocksSent++;
                        break;
                    case 34:
                        send(game.getSlot(2));
                        blocksSent++;
                        break;
                    default:
                        LOGGER.warn("Unexpected command {}", Util.dumpAsHexString(command));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Transferring Game", e);
            throw new RuntimeException("Game transfer", e);
        } finally {
            closeSerialPort();
        }
    }
}
