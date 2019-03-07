package com.grelobites.romgenerator.handlers.dandanatorcpc;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.MainApp;
import com.grelobites.romgenerator.handlers.dandanatorcpc.v1.GameHeaderV1Serializer;
import com.grelobites.romgenerator.handlers.dandanatorcpc.view.SerialCopyController;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.SerialPortConfiguration;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.Z80Opcode;
import com.grelobites.romgenerator.util.compress.zx7.Zx7InputStream;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

public class RomSetUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RomSetUtil.class);
    private static final String LOADER_NAME = "DivIDELoader";
    private static final String PAUSE_FILENAME = "pause.wav";
    private static final String PAUSE_RESOURCE = "/player/" + PAUSE_FILENAME;
    private static final String PLAYLIST_NAME = "loader.m3u";
    private static final int LOAD_ADDRESS = 0x6f00;
    private static final int BLOCK_SIZE = 0x8000;
    private static final int BLOCK_COUNT = 16;
    private static final String BLOCK_NAME_PREFIX = "block";
    private static final String MULTILOADER_SIGNATURE = "MLD";
    private static final int SEND_BUFFER_SIZE = 2048;

    private static Optional<InputStream> getRomScreenResource(ByteBuffer buffer, int slot) {
        buffer.position(Constants.SLOT_SIZE * slot);
        byte[] magic = new byte[3];
        buffer.get(magic);
        if (MULTILOADER_SIGNATURE.equals(new String(magic))) {
            int version = Byte.toUnsignedInt(buffer.get());
            int offset = Short.toUnsignedInt(buffer.getShort());
            int size = Short.toUnsignedInt(buffer.getShort());
            LOGGER.debug("Detected Multiload ROMSet with version " + version);
            LOGGER.debug("Compressed screen at offset " + offset + ", size " + size);
            return Optional.of(new Zx7InputStream(new ByteArrayInputStream(buffer.array(),
                    offset + Constants.SLOT_SIZE * slot, size)));
        } else {
            return Optional.empty();
        }
    }

    public static byte[] getCompressedScreen(byte[] screenWithPalette) throws IOException {
        byte[] packedScreen = Arrays.copyOf(screenWithPalette, DandanatorCpcConstants.PACKED_SCREEN_SIZE);
        System.arraycopy(screenWithPalette, DandanatorCpcConstants.PACKED_SCREEN_SIZE,
                packedScreen, DandanatorCpcConstants.PACKED_SCREEN_SIZE - DandanatorCpcConstants.PALETTE_SIZE,
                DandanatorCpcConstants.PALETTE_SIZE);
        return Util.compress(packedScreen);
    }

    public static byte[] decodeCharset(byte[] encodedCharset) throws IOException {
        byte[] charset = new byte[Constants.CHARSET_SIZE + Constants.ICONS_SIZE];
        for (int i = 0; i < Constants.CHARSET_SIZE; i++) {
            charset[i] = Integer.valueOf((encodedCharset[2 * i] & 0xF0) |
                    (encodedCharset[(2 * i) + 1] & 0x0F)).byteValue();
        }
        System.arraycopy(Constants.getIcons(), 0, charset, Constants.CHARSET_SIZE,
                Constants.ICONS_SIZE);
        return charset;
    }

    public static byte[] encodeCharset(byte [] charset) {
        byte[] encoded = new byte[Constants.CHARSET_SIZE * 2 + Constants.ICONS_SIZE];
        for (int i = 0; i < Constants.CHARSET_SIZE; i++) {
            encoded[2 * i] = Integer.valueOf((((charset[i] & 0xF0))) >> 4 |
                    (charset[i] & 0xF0)).byteValue();
            encoded[(2 * i) + 1] = Integer.valueOf((((charset[i] & 0x0F)) << 4) |
                    (charset[i] & 0x0F)).byteValue();
        }
        System.arraycopy(charset, Constants.CHARSET_SIZE, encoded, Constants.CHARSET_SIZE * 2,
                Constants.ICONS_SIZE);
        return encoded;
    }

    private static boolean isPrintableAscii(String name) {
        for (int i = 0; i < name.length(); i++) {
            int code = name.charAt(i);
            if (code < 32 || code > 127) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidGameName(String name) {
        return name != null &&
                name.length() <= DandanatorCpcConstants.GAMENAME_EFFECTIVE_SIZE &&
                isPrintableAscii(name);
    }

    public static boolean isValidPokeName(String name) {
        return name != null &&
                name.length() <= DandanatorCpcConstants.POKE_EFFECTIVE_NAME_SIZE &&
                isPrintableAscii(name);
    }

    private static byte[] getGameChunk(Game game) {
        byte[] chunk = new byte[DandanatorCpcConstants.GAME_CHUNK_SIZE];
        if (game instanceof SnapshotGame) {
            System.arraycopy(game.getSlot(DandanatorCpcConstants.GAME_CHUNK_SLOT),
                    Constants.SLOT_SIZE - DandanatorCpcConstants.GAME_CHUNK_SIZE,
                    chunk, 0, DandanatorCpcConstants.GAME_CHUNK_SIZE);
        }
        return chunk;
    }

    /*
        POP BC
        LD SP, nnnn ---> o 4000 u 8000, el sitio contrario a donde vaya la pantalla
        LD HL, C000
        LD DE, destino pantalla, 8000 y 4000
        PUSH DE
        PUSH BC
        LD BC,4000
        LDIR
    ----
        PUSH IY
        LD IY,0
        LD B,0
        FDFD
        LD (IY),B
        POP IY
        RET

        * Nuevo *
        * Code3rd:
			LD SP, 0000						; SP Value
			LD BC, 0000						; AF Value
			PUSH BC
			POP AF
			LD BC, 0000						; PC Value
			PUSH BC
			LD BC, 0000						; BC Value
			LD DE, 0000						; DE Value
			LD HL, 0000						; HL Value
			LD IX, 0000						; IX Value
			LD IY, 0000						; IY Value
			DI 								; DI or EI according to IFF0
			RET
EndCode3rd:
         LD SP, 0000      ; SP Value
   LD BC, 0000      ; AF Value
   PUSH BC
   POP AF
   LD BC, 0000      ; PC Value
   PUSH BC
   LD BC, 0000      ; BC Value
   LD DE, 0000      ; DE Value
   LD HL, 0000      ; HL Value
   LD IX, 0000      ; IX Value
   LD IY, 0000      ; IY Value
   DI         ; DI or EI according to IFF0
   RET
        *********
     */
    protected static void dumpGameLaunchCode(OutputStream os, SnapshotGame game) throws IOException {
         ByteBuffer launchCode = ByteBuffer.allocate(33);
         /*
        if (game.getScreenSlot() != 3) {
            launchCode.put(Z80Opcode.POP_BC);
            launchCode.put(Z80Opcode.LD_SP_NN(game.getScreenSlot() == 1 ? 0x8000 : 0x4000));
            launchCode.put(Z80Opcode.LD_HL_NN(0xC000));
            launchCode.put(Z80Opcode.LD_DE_NN(game.getScreenSlot() * Constants.SLOT_SIZE));
            launchCode.put(Z80Opcode.PUSH_DE);
            launchCode.put(Z80Opcode.PUSH_BC);
            launchCode.put(Z80Opcode.LD_BC_NN(0x4000));
            launchCode.put(Z80Opcode.LDIR);
        }
        launchCode.put(Z80Opcode.PUSH_IY);
        launchCode.put(Z80Opcode.LD_IY_NN(0));
        launchCode.put(Z80Opcode.LD_B_N(0));
        launchCode.put(Z80Opcode.DANDANATOR_PREFIX);
        launchCode.put(Z80Opcode.LD_IY_B);
        launchCode.put(Z80Opcode.POP_IY);
        launchCode.put(Z80Opcode.RET);
        */
         GameHeader header = game.getGameHeader();
         launchCode.put(Z80Opcode.LD_SP_NN(header.getSp()));
         launchCode.put(Z80Opcode.LD_BC_NN(header.getBcRegister()));
         launchCode.put(Z80Opcode.PUSH_BC);
         //launchCode.put(Z80Opcode.POP_AF);


        os.write(launchCode.array());
    }

    private static void sendBySerialPort(byte[] data) {
        SerialPort serialPort = new SerialPort(
                EepromWriterConfiguration.getInstance().getSerialPort()
        );
        try {
            serialPort.openPort();
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
            LOGGER.error("Setting up serial port", e);
        } finally {
            try {
                serialPort.closePort();
            } catch (Exception e) {
                LOGGER.error("Closing serial port", e);
            }
        }
    }


    public static void sendSelectedGameBySerialPort(ApplicationContext context) throws IOException {
        SnapshotGame game = (SnapshotGame) context.getSelectedGame();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GameHeaderV1Serializer.serialize(game, bos);    // 90 bytes
        bos.write(game.getType().typeId());             // 1 byte
        bos.write(getGameChunk(game));                  // 32 byte
        bos.write(Constants.B_00);                      // 1 byte
        bos.write(Constants.B_00);                      // 1 byte
        bos.write(0); //Upper and lower active roms.    1 byte
        bos.write(game.getCurrentRasterInterrupt());    // 1 byte
        dumpGameLaunchCode(bos, game);                  // 33 bytes. 160 bytes
        bos.write(game.getSlot(game.getScreenSlot()));
        /*
        try (FileOutputStream fos = new FileOutputStream("output/data.bin")) {
            fos.write(bos.toByteArray());
        }
        */
        sendBySerialPort(bos.toByteArray());
    }

}
