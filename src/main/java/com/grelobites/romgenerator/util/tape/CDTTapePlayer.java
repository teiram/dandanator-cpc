package com.grelobites.romgenerator.util.tape;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.emulator.Clock;
import com.grelobites.romgenerator.util.emulator.ClockTimeoutListener;
import com.grelobites.romgenerator.util.emulator.TapeFinishedException;
import com.grelobites.romgenerator.util.emulator.peripheral.Ppi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class CDTTapePlayer implements ClockTimeoutListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CDTTapePlayer.class);

    private static final int LEADER_LENGHT = 2168;
    private static final int SYNC1_LENGHT = 667;
    private static final int SYNC2_LENGHT = 735;
    private static final int ZERO_LENGHT = 855;
    private static final int ONE_LENGHT = 1710;
    private static final int HEADER_PULSES = 8063;
    private static final int DATA_PULSES = 3223;
    private static final int END_BLOCK_PAUSE = 3500000;


    public enum State {
        STOP, START, LEADER, LEADER_NOCHG, SYNC, NEWBYTE,
        NEWBYTE_NOCHG, NEWBIT, HALF2, LAST_PULSE, PAUSE, TZX_HEADER, PURE_TONE,
        PURE_TONE_NOCHG, PULSE_SEQUENCE, PULSE_SEQUENCE_NOCHG, NEWDR_BYTE,
        NEWDR_BIT, PAUSE_STOP, CSW_RLE, CSW_ZRLE
    }


    private State state;
    private boolean playing;
    private boolean invertedOutput = false;
    private byte[] tapeBuffer;
    private int idxHeader;
    private int tapePos;
    private int blockLen;
    private int bitTime;
    private final Clock clock;
    private final Ppi ppi;
    private int leaderPulses;
    private int leaderLength;
    private int sync1Length;
    private int sync2Length;
    private int zeroLength;
    private int oneLength;
    private int bitsLastByte;
    private int endBlockPause;
    private float cswStatesSample;
    private int nCalls;
    private short[] callSeq;
    int callBlk;
    private ByteArrayInputStream bais;
    private InflaterInputStream iis;
    private int mask;
    private List<Integer> blockOffsets = new ArrayList<>();
    private boolean eot = false;
    private int readBytes = 0;
    int nLoops;
    int loopStart;
    private boolean throwOnEot = false;

    public CDTTapePlayer(Clock clock, Ppi ppi) {
        this.clock = clock;
        this.ppi = ppi;
        state = State.STOP;
        tapePos = 0;
        ppi.setCasseteDataInput(false);
        idxHeader = 0;
        playing = false;
    }

    public CDTTapePlayer(Clock clock, Ppi ppi, boolean throwOnEot) {
        this(clock, ppi);
        this.throwOnEot = throwOnEot;
    }

    private static int readInt(byte buffer[], int start, int len) {
        int res = 0;

        for (int idx = 0; idx < len; idx++) {
            res |= ((buffer[start + idx] << (idx * 8)) & (0xff << idx * 8));
        }
        return res;
    }

    public Clock getClock() {
        return clock;
    }

    public boolean isEOT() {
        return eot;
    }

    private static String readBlockName(byte[] buffer, int offset, int length) {
        return new String(Arrays.copyOfRange(buffer, offset, offset + length));
    }

    private boolean findBlockOffsets() {
        int offset = 0;

        int blocksToBlackList = 0;

        while (offset < tapeBuffer.length) {
            if ((tapeBuffer.length - offset) < 2) {
                return false;
            }
            int len = readInt(tapeBuffer, offset, 2);

            if (offset + len + 2 > tapeBuffer.length) {
                return false;
            }
            String name = readBlockName(tapeBuffer, offset + 4, 10);
            blockOffsets.add(offset);
            LOGGER.debug("Adding tapePlayer block with length " + len + " and name " + name + " at offset " + offset);

            offset += len + 2;
        }
        LOGGER.debug("Number of blocks in tapePlayer " + blockOffsets.size());

        return true;
    }

    public boolean insert(File fileName) {
        try (FileInputStream is = new FileInputStream(fileName)) {
            return insert(is);
        } catch (IOException ioe) {
            LOGGER.error("Inserting tapePlayer", ioe);
        }
        return false;
    }

    public boolean insert(InputStream is) {
        try {
            tapeBuffer = Util.fromInputStream(is);
        } catch (IOException ioe) {
            LOGGER.error("Inserting tapePlayer", ioe);
            return false;
        }

        tapePos = idxHeader = readBytes = 0;
        blockOffsets.clear();
        eot = playing = false;

        state = State.STOP;
        if (!findBlockOffsets()) {
            return false;
        }
        return true;
    }

    public void rewind() {
        state = State.STOP;
        tapePos = idxHeader = readBytes = 0;
        eot = playing = false;
    }

    public void eject() {
        stop();
        tapePos = idxHeader = readBytes = 0;
        eot = false;
        state = State.STOP;
    }

    private boolean playCdt() {
        boolean repeat;
        int timeout;

        do {
            repeat = false;
            switch (state) {
                case STOP:
                    stop();
                    break;
                case START:
                    tapePos = blockOffsets.get(idxHeader);
                    ppi.setCasseteDataInput(invertedOutput);
                    state = State.TZX_HEADER;
                    repeat = true;
                    break;
                case LEADER:
                    ppi.changeCasseteDataInput();
                case LEADER_NOCHG:
                    if (leaderPulses-- > 0) {
                        state = State.LEADER;
                        clock.setTimeout(leaderLength);
                        break;
                    }
                    clock.setTimeout(sync1Length);
                    state = State.SYNC;
                    break;
                case SYNC:
                    ppi.changeCasseteDataInput();
                    clock.setTimeout(sync2Length);
                    state = blockLen > 0 ? State.NEWBYTE : State.PAUSE;
                    break;
                case NEWBYTE_NOCHG:
                    //To Undo on NEWBIT case
                    ppi.changeCasseteDataInput();
                case NEWBYTE:
                    mask = 0x80; // se empieza por el bit 7
                case NEWBIT:
                    ppi.changeCasseteDataInput();
                    if ((tapeBuffer[tapePos] & mask) == 0) {
                        bitTime = zeroLength;
                    } else {
                        bitTime = oneLength;
                    }
                    state = State.HALF2;
                    clock.setTimeout(bitTime);
                    break;
                case HALF2:
                    ppi.changeCasseteDataInput();
                    clock.setTimeout(bitTime);
                    mask >>>= 1;
                    if (blockLen == 1 && bitsLastByte < 8) {
                        if (mask == (0x80 >>> bitsLastByte)) {
                            state = State.LAST_PULSE;
                            tapePos++;
                            break;
                        }
                    }

                    if (mask != 0) {
                        state = State.NEWBIT;
                        break;
                    }

                    tapePos++;
                    if (--blockLen > 0) {
                        state = State.NEWBYTE;
                    } else {
                        state = State.LAST_PULSE;
                    }
                    break;
                case LAST_PULSE:
                    ppi.changeCasseteDataInput();
                    if (endBlockPause == 0) {
                        state = State.TZX_HEADER;
                        repeat = true;
                        break;
                    }
                    state = State.PAUSE;
                    clock.setTimeout(3500); // 1 ms by TZX spec
                    break;
                case PAUSE:
                    ppi.setCasseteDataInput(invertedOutput);
                    state = State.TZX_HEADER;
                    clock.setTimeout(endBlockPause);
                    break;
                case TZX_HEADER:
                    if (idxHeader >= blockOffsets.size()) {
                        state = State.STOP;
                        repeat = true;
                        break;
                    }
                    decodeTzxHeader();
                    repeat = true;
                    break;
                case PURE_TONE:
                    ppi.changeCasseteDataInput();
                case PURE_TONE_NOCHG:
                    if (leaderPulses-- > 0) {
                        clock.setTimeout(leaderLength);
                        state = State.PURE_TONE;
                        break;
                    }
                    state = State.TZX_HEADER;
                    repeat = true;
                    break;
                case PULSE_SEQUENCE:
                    ppi.changeCasseteDataInput();
                case PULSE_SEQUENCE_NOCHG:
                    if (leaderPulses-- > 0) {
                        clock.setTimeout(readInt(tapeBuffer, tapePos, 2));
                        tapePos += 2;
                        state = State.PULSE_SEQUENCE;
                        break;
                    }
                    state = State.TZX_HEADER;
                    repeat = true;
                    break;
                case NEWDR_BYTE:
                    mask = 0x80;
                    state = State.NEWDR_BIT;
                case NEWDR_BIT:
                    boolean earState;
                    if ((tapeBuffer[tapePos] & mask) != 0) {
                        earState = true;
                        ppi.setCasseteDataInput(true);
                    } else {
                        earState = false;
                        ppi.setCasseteDataInput(false);
                    }
                    timeout = 0;

                    while (((tapeBuffer[tapePos] & mask) != 0) == earState) {
                        timeout += zeroLength;

                        mask >>>= 1;
                        if (mask == 0) {
                            mask = 0x80;
                            tapePos++;
                            if (--blockLen == 0) {
                                state = State.LAST_PULSE;
                                break;
                            }
                        } else {
                            if (blockLen == 1 && bitsLastByte < 8) {
                                if (mask == (0x80 >>> bitsLastByte)) {
                                    state = State.LAST_PULSE;
                                    tapePos++;
                                    break;
                                }
                            }
                        }
                    }
                    clock.setTimeout(timeout);
                    break;
                case PAUSE_STOP:
                    if (endBlockPause == 0) {
                        state = State.STOP;
                        repeat = true;
                    } else {
                        ppi.setCasseteDataInput(invertedOutput);
                        state = State.TZX_HEADER;
                        clock.setTimeout(endBlockPause);
                    }
                    break;
                case CSW_RLE:
                    if (blockLen == 0) {
                        state = State.PAUSE;
                        repeat = true;
                    }

                    ppi.changeCasseteDataInput();

                    timeout = tapeBuffer[tapePos++] & 0xff;
                    blockLen--;
                    if (timeout == 0) {
                        timeout = readInt(tapeBuffer, tapePos, 4);
                        tapePos += 4;
                        blockLen -= 4;
                    }

                    timeout *= cswStatesSample;
                    clock.setTimeout(timeout);
                    break;
                case CSW_ZRLE:
                    ppi.changeCasseteDataInput();

                    try {
                        timeout = iis.read();
                        if (timeout < 0) {
                            iis.close();
                            bais.close();
                            repeat = true;
                            state = State.PAUSE;
                            break;
                        }

                        if (timeout == 0) {
                            byte nSamples[] = new byte[4];
                            while (timeout < 4) {
                                int count = iis.read(nSamples, timeout,
                                        nSamples.length - timeout);
                                if (count == -1) {
                                    break;
                                }
                                timeout += count;
                            }

                            if (timeout == 4) {
                                timeout = readInt(nSamples, 0, 4);
                            } else {
                                iis.close();
                                bais.close();
                                repeat = true;
                                state = State.PAUSE;
                                break;
                            }
                        }

                        timeout *= cswStatesSample;
                        clock.setTimeout(timeout);

                    } catch (IOException ioe) {
                        LOGGER.warn("Reading stream", ioe);
                    }
                    break;
            }
        } while (repeat);
        return true;
    }

    private void decodeTzxHeader() {
        boolean repeat = true;

        while (repeat) {
            if (idxHeader >= blockOffsets.size()) {
                return;
            }

            tapePos = blockOffsets.get(idxHeader);

            switch (tapeBuffer[tapePos] & 0xff) {
                case CDTBlock.STANDARD_SPEED: // Standard speed data block
                    leaderLength = LEADER_LENGHT;
                    sync1Length = SYNC1_LENGHT;
                    sync2Length = SYNC2_LENGHT;
                    zeroLength = ZERO_LENGHT;
                    oneLength = ONE_LENGHT;
                    bitsLastByte = 8;
                    endBlockPause = readInt(tapeBuffer, tapePos + 1, 2);
                    blockLen = readInt(tapeBuffer, tapePos + 3, 2);
                    tapePos += 5;
                    leaderPulses =
                            (tapeBuffer[tapePos] & 0xff) < 0x80 ? HEADER_PULSES : DATA_PULSES;
                    state = State.LEADER_NOCHG;
                    idxHeader++;
                    if (idxHeader >= blockOffsets.size() && endBlockPause > 1000) {
                        endBlockPause = 1;
                    }
                    endBlockPause *= (END_BLOCK_PAUSE / 1000);
                    repeat = false;
                    break;
                case CDTBlock.TURBO_SPEED: // Turbo speed data block
                    leaderLength = readInt(tapeBuffer, tapePos + 1, 2);
                    sync1Length = readInt(tapeBuffer, tapePos + 3, 2);
                    sync2Length = readInt(tapeBuffer, tapePos + 5, 2);
                    zeroLength = readInt(tapeBuffer, tapePos + 7, 2);
                    oneLength = readInt(tapeBuffer, tapePos + 9, 2);
                    leaderPulses = readInt(tapeBuffer, tapePos + 11, 2);
                    bitsLastByte = tapeBuffer[tapePos + 13] & 0xff;
                    endBlockPause = readInt(tapeBuffer, tapePos + 14, 2);
                    blockLen = readInt(tapeBuffer, tapePos + 16, 3);
                    tapePos += 19;
                    state = State.LEADER_NOCHG;
                    idxHeader++;
                    if (idxHeader >= blockOffsets.size() && endBlockPause > 1000) {
                        endBlockPause = 1;
                    }
                    endBlockPause *= (END_BLOCK_PAUSE / 1000);
                    repeat = false;
                    break;
                case CDTBlock.PURE_TONE: // Pure Tone Block
                    leaderLength = readInt(tapeBuffer, tapePos + 1, 2);
                    leaderPulses = readInt(tapeBuffer, tapePos + 3, 2);
                    tapePos += 5;
                    state = State.PURE_TONE_NOCHG;
                    idxHeader++;
                    repeat = false;
                    break;
                case CDTBlock.PULSE_SEQUENCE: // Pulse Sequence Block
                    leaderPulses = tapeBuffer[tapePos + 1] & 0xff;
                    tapePos += 2;
                    state = State.PULSE_SEQUENCE_NOCHG;
                    idxHeader++;
                    repeat = false;
                    break;
                case CDTBlock.PURE_DATA_BLOCK: // Pure Data Block
                    zeroLength = readInt(tapeBuffer, tapePos + 1, 2);
                    oneLength = readInt(tapeBuffer, tapePos + 3, 2);
                    bitsLastByte = tapeBuffer[tapePos + 5] & 0xff;
                    endBlockPause = readInt(tapeBuffer, tapePos + 6, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    blockLen = readInt(tapeBuffer, tapePos + 8, 3);
                    tapePos += 11;
                    state = State.NEWBYTE_NOCHG;
                    idxHeader++;
                    repeat = false;
                    break;
                case CDTBlock.DIRECT_RECORDING: // Direct Data Block
                    zeroLength = readInt(tapeBuffer, tapePos + 1, 2);
                    endBlockPause = readInt(tapeBuffer, tapePos + 3, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    bitsLastByte = tapeBuffer[tapePos + 5] & 0xff;
                    blockLen = readInt(tapeBuffer, tapePos + 6, 3);
                    tapePos += 9;
                    state = State.NEWDR_BYTE;
                    idxHeader++;
                    repeat = false;
                    break;
                case CDTBlock.CSW_RECORDING: // CSW Recording Block
                    endBlockPause = readInt(tapeBuffer, tapePos + 5, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    cswStatesSample = 3500000.0f / readInt(tapeBuffer, tapePos + 7, 3);
                    blockLen = readInt(tapeBuffer, tapePos + 1, 4) - 10;
                    if (tapeBuffer[tapePos + 10] == 0x02) {
                        state = State.CSW_ZRLE;
                        bais = new ByteArrayInputStream(tapeBuffer, tapePos + 15, blockLen);
                        iis = new InflaterInputStream(bais);
                    } else {
                        state = State.CSW_RLE;
                    }
                    tapePos += 15;
                    idxHeader++;
                    // al entrar la primera vez deshará el cambio
                    ppi.changeCasseteDataInput();
                    repeat = false;
                    break;
                case CDTBlock.GENERALIZED_DATA: // Generalized Data Block
                    endBlockPause = readInt(tapeBuffer, tapePos + 5, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    /*
                    totp = readInt(tapeBuffer, tapePos + 7, 4);
                    npp = tapeBuffer[tapePos + 11] & 0xff;
                    asp = tapeBuffer[tapePos + 12] & 0xff;
                    totd = readInt(tapeBuffer, tapePos + 13, 4);
                    npd = tapeBuffer[tapePos + 17] & 0xff;
                    asd = tapeBuffer[tapePos + 18] & 0xff;
                    */
                    idxHeader++;
                    LOGGER.warn("Generalized Data Block not supported. Skipping");
                    break;
                case CDTBlock.SILENCE: // Pause (silence) or 'Stop the Tape' command
                    endBlockPause = readInt(tapeBuffer, tapePos + 1, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    tapePos += 3;
                    state = State.PAUSE_STOP;
                    idxHeader++;
                    repeat = false;
                    break;
                case CDTBlock.GROUP_START: // Group Start
                    idxHeader++;
                    break;
                case CDTBlock.GROUP_END: // Group End
                    idxHeader++;
                    break;
                case CDTBlock.JUMP_TO_BLOCK: // Jump to Block
                    short target = (short) readInt(tapeBuffer, tapePos + 1, 2);
                    idxHeader += target;
                    break;
                case CDTBlock.LOOP_START: // Loop Start
                    nLoops = readInt(tapeBuffer, tapePos + 1, 2);
                    loopStart = ++idxHeader;
                    break;
                case CDTBlock.LOOP_END: // Loop End
                    if (--nLoops == 0) {
                        idxHeader++;
                        break;
                    }
                    idxHeader = loopStart;
                    break;
                case CDTBlock.CALL_SEQUENCE: // Call Sequence
                    if (callSeq == null) {
                        nCalls = readInt(tapeBuffer, tapePos + 1, 2);
                        callSeq = new short[nCalls];
                        for (int idx = 0; idx < nCalls; idx++) {
                            callSeq[idx] = (short) (readInt(tapeBuffer, tapePos + idx * 2 + 3, 2));
                        }
                        callBlk = idxHeader;
                        nCalls = 0;
                        idxHeader += callSeq[nCalls++];
                    } else {
                        LOGGER.warn("CALL_SEQUENCE blocks can't be nested. Skipping");
                        idxHeader++;
                    }
                    break;
                case CDTBlock.RETURN_FROM_SEQUENCE: // Return from Sequence
                    if (nCalls < callSeq.length) {
                        idxHeader = callBlk + callSeq[nCalls++];
                    } else {
                        idxHeader = callBlk + 1;
                        callSeq = null;
                    }
                    break;
                case 0x28: // Select Block
                    idxHeader++;
                    break;
                case CDTBlock.STOP_TAPE_48KMODE: // Stop the tape if in 48K mode
                    idxHeader++;
                    break;
                case CDTBlock.SET_SIGNAL_LEVEL: // Set Signal Level
                    ppi.setCasseteDataInput(tapeBuffer[tapePos + 5] != 0);
                    idxHeader++;
                    break;
                case CDTBlock.TEXT_DESCRIPTION: // Text Description
                    idxHeader++;
                    break;
                case CDTBlock.MESSAGE_BLOCK: // Message Block
                    idxHeader++;
                    break;
                case CDTBlock.ARCHIVE_INFO: // Archive Info
                    idxHeader++;
                    break;
                case CDTBlock.HARDWARE_TYPE: // Hardware Type
                    idxHeader++;
                    break;
                case CDTBlock.CUSTOM_INFO_BOCK: // Custom Info Block
                    idxHeader++;
                    break;
                case CDTBlock.GLUE_BLOCK: // TZX Header && "Glue" Block
                    idxHeader++;
                    break;
                default:
                    LOGGER.warn("Unrecognized CDTBlock of type {}", String
                            .format("%02x", tapeBuffer[tapePos]));
                    repeat = false;
                    idxHeader++;
            }
        }
    }


    public boolean play() {
        if (!playing) {
            if (idxHeader >= blockOffsets.size()) {
                LOGGER.warn("Trying to play with blocks exhausted");
                return false;
            }
            state = State.START;
            tapePos = blockOffsets.get(idxHeader);
            clock.addClockTimeoutListener(this);
            clockTimeout();
            playing = true;
        }
        return true;
    }


    public void stop() {
        if (playing) {
            if (state == State.PAUSE_STOP) {
                idxHeader++;
                if (idxHeader >= blockOffsets.size()) {
                    eot = true;
                }
            }
            state = State.STOP;
            clock.removeClockTimeoutListener(this);
            playing = false;
        }
    }

    public int getTapePos() {
        return tapePos;
    }
    public boolean isPlaying() {
        return playing;
    }

    public State getState() {
        return state;
    }

    public int getReadBytes() {
        return readBytes;
    }

    @Override
    public void clockTimeout() {
        playCdt();
    }

    private void onEot() {
        eot = true;
        if (throwOnEot) {
            throw new TapeFinishedException("CDTTapePlayer completed");
        }
    }

    @Override
    public String toString() {
        return "CDTTapePlayer{" +
                "state=" + state +
                ", playing=" + playing +
                ", idxHeader=" + idxHeader +
                ", tapePos=" + tapePos +
                ", blockLen=" + blockLen +
                ", blockOffsets= " + blockOffsets +
                ", tapeBuffer.length=" + (tapeBuffer != null ? tapeBuffer.length : "nil") +
                ", eot=" + eot +
                '}';
    }
}
