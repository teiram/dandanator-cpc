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
import java.util.Optional;
import java.util.zip.InflaterInputStream;

public class CdtTapePlayer implements ClockTimeoutListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdtTapePlayer.class);

    private static final int LEADER_LENGHT = adjustDuration(2168);
    private static final int SYNC1_LENGHT = adjustDuration(667);
    private static final int SYNC2_LENGHT = adjustDuration(735);
    private static final int ZERO_LENGHT = adjustDuration(855);
    private static final int ONE_LENGHT = adjustDuration(1710);
    private static final int HEADER_PULSES = adjustDuration(8063);
    private static final int DATA_PULSES = adjustDuration(3223);
    private static final int END_BLOCK_PAUSE = adjustDuration(3500000);

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

    private List<BlockChangeListener> blockChangeListeners = new ArrayList<>();

    //Adjust duration in 3.5Mhz clock pulses to 4Mhz clock pulses
    private static int adjustDuration(int duration) {
        return (duration * 40) / 35;
    }

    public CdtTapePlayer(Clock clock, Ppi ppi) {
        this.clock = clock;
        this.ppi = ppi;
        state = State.STOP;
        tapePos = 0;
        ppi.setCasseteDataInput(false);
        idxHeader = 0;
        playing = false;
    }

    public void addBlockChangeListener(BlockChangeListener listener) {
        blockChangeListeners.add(listener);
    }

    public void removeBlockChangeListener(BlockChangeListener listener) {
        blockChangeListeners.remove(listener);
    }

    private void notifyBlockChangeListeners(int currentBlock) {
        blockChangeListeners.forEach(c -> {
            try {
                c.onBlockChange(currentBlock);
            } catch (IOException e) {
                LOGGER.warn("Block Change Listener failed with exception", e);
            }
        });
    }

    public CdtTapePlayer(Clock clock, Ppi ppi, boolean throwOnEot) {
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

    public State state() {
        return state;
    }

    public Clock getClock() {
        return clock;
    }

    public boolean isEOT() {
        return eot;
    }

    public String getStatus() {
        return String.format("Playing=%s, position=%d, block=%d/%d",
                playing, tapePos, idxHeader + 1, blockOffsets.size());
    }
    private static String readBlockName(byte[] buffer, int offset, int length) {
        return new String(Arrays.copyOfRange(buffer, offset, offset + length));
    }

    private static void breakWithError(int offset) {
        throw new IllegalArgumentException("Invalid block/offset detected at " + offset);
    }

    private void findBlockOffsets() {
        int offset = 0;
        int len;

        while (offset < tapeBuffer.length) {
            blockOffsets.add(offset);
            switch (tapeBuffer[offset] & 0xff) {
                case CdtBlock.STANDARD_SPEED:
                    if (tapeBuffer.length - offset < 5) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 3, 2);
                    offset += len + 5;
                    break;
                case CdtBlock.TURBO_SPEED:
                    if (tapeBuffer.length - offset < 19) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 16, 3);
                    offset += len + 19;
                    break;
                case CdtBlock.PURE_TONE:
                    offset += 5;
                    break;
                case CdtBlock.PULSE_SEQUENCE:
                    if (tapeBuffer.length - offset < 2) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 1] & 0xff;
                    offset += len * 2 + 2;
                    break;
                case CdtBlock.PURE_DATA_BLOCK:
                    if (tapeBuffer.length - offset < 11) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 8, 3);
                    offset += len + 11;
                    break;
                case CdtBlock.DIRECT_RECORDING:
                    if (tapeBuffer.length - offset < 9) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 6, 3);
                    offset += len + 9;
                    break;
                case CdtBlock.CSW_RECORDING:
                case CdtBlock.GENERALIZED_DATA:
                    if (tapeBuffer.length - offset < 5) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 1, 4);
                    offset += len + 5;
                    break;
                case CdtBlock.SILENCE:
                case CdtBlock.JUMP_TO_BLOCK:
                case CdtBlock.LOOP_START:
                    offset += 3;
                    break;
                case CdtBlock.GROUP_START:
                    if (tapeBuffer.length - offset < 2) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 1] & 0xff;
                    offset += len + 2;
                    break;
                case CdtBlock.GROUP_END:
                case CdtBlock.LOOP_END:
                case CdtBlock.RETURN_FROM_SEQUENCE:
                    offset++;
                    break;
                case CdtBlock.CALL_SEQUENCE:
                    if (tapeBuffer.length - offset < 3) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 1, 2);
                    offset += len * 2 + 3;
                    break;
                case CdtBlock.SELECT_BLOCK:
                case CdtBlock.ARCHIVE_INFO:
                    if (tapeBuffer.length - offset < 3) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 1, 2);
                    offset += len + 3;
                    break;
                case CdtBlock.STOP_TAPE_48KMODE:
                    offset += 5;
                    break;
                case CdtBlock.SET_SIGNAL_LEVEL:
                    offset += 6;
                    break;
                case CdtBlock.TEXT_DESCRIPTION:
                    if (tapeBuffer.length - offset < 2) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 1] & 0xff;
                    offset += len + 2;
                    break;
                case CdtBlock.MESSAGE_BLOCK:
                    if (tapeBuffer.length - offset < 3) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 2] & 0xff;
                    offset += len + 3;
                    break;
                case CdtBlock.HARDWARE_TYPE:
                    if (tapeBuffer.length - offset < 2) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 1] & 0xff;
                    offset += len * 3 + 2;
                    break;
                case CdtBlock.CUSTOM_INFO_BLOCK:
                    if (tapeBuffer.length - offset < 21) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 17, 4);
                    offset += len + 21;
                    break;
                case CdtBlock.GLUE_BLOCK:
                    offset += 10;
                    break;
                default:
                    LOGGER.error("Unexpected block type {}", String.format("%02x", tapeBuffer[offset]));
                    breakWithError(offset);
            }

            if (offset > tapeBuffer.length) {
                throw new IllegalArgumentException("Tape stream exhausted");
            }
        }
        LOGGER.debug("Found {} blocks in tape", blockOffsets.size());
    }

    public void insert(File fileName) {
        try (FileInputStream is = new FileInputStream(fileName)) {
            insert(is);
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Trying to read file stream", ioe);
        }
    }

    public void insert(InputStream is) {
        try {
            Optional<CdtHeader> header = CdtHeader.fromInputStream(is);
            if (header.isPresent()) {
                tapeBuffer = Util.fromInputStream(is);
            } else {
                throw new IllegalArgumentException("No header found in tape stream");
            }
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Trying to read tape stream", ioe);
        }

        tapePos = idxHeader = readBytes = 0;
        blockOffsets.clear();
        eot = playing = false;

        state = State.STOP;
        findBlockOffsets();
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

    private void playCdt() {
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
                    clock.setTimeout(4000); // 1 ms by TZX spec
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
                        eot = true;
                    }
                    break;
            }
        } while (repeat);
    }

    private void decodeTzxHeader() {
        boolean repeat = true;
        int currentBlock = idxHeader;

        while (repeat) {
            if (idxHeader >= blockOffsets.size()) {
                return;
            }

            tapePos = blockOffsets.get(idxHeader);

            switch (tapeBuffer[tapePos] & 0xff) {
                case CdtBlock.STANDARD_SPEED:
                    LOGGER.debug("Standard Speed block");
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
                case CdtBlock.TURBO_SPEED:
                    LOGGER.debug("Turbo Speed block");
                    leaderLength =  adjustDuration(readInt(tapeBuffer, tapePos + 1, 2));
                    sync1Length =   adjustDuration(readInt(tapeBuffer, tapePos + 3, 2));
                    sync2Length =   adjustDuration(readInt(tapeBuffer, tapePos + 5, 2));
                    zeroLength =    adjustDuration(readInt(tapeBuffer, tapePos + 7, 2));
                    oneLength =     adjustDuration(readInt(tapeBuffer, tapePos + 9, 2));
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
                case CdtBlock.PURE_TONE:
                    LOGGER.debug("Pure Tone block");
                    leaderLength =  adjustDuration(readInt(tapeBuffer, tapePos + 1, 2));
                    leaderPulses =  adjustDuration(readInt(tapeBuffer, tapePos + 3, 2));
                    tapePos += 5;
                    state = State.PURE_TONE_NOCHG;
                    idxHeader++;
                    repeat = false;
                    break;
                case CdtBlock.PULSE_SEQUENCE:
                    LOGGER.debug("Pulse Sequence block");
                    leaderPulses = tapeBuffer[tapePos + 1] & 0xff;
                    tapePos += 2;
                    state = State.PULSE_SEQUENCE_NOCHG;
                    idxHeader++;
                    repeat = false;
                    break;
                case CdtBlock.PURE_DATA_BLOCK:
                    LOGGER.debug("Pure Data block");
                    zeroLength =    adjustDuration(readInt(tapeBuffer, tapePos + 1, 2));
                    oneLength =     adjustDuration(readInt(tapeBuffer, tapePos + 3, 2));
                    bitsLastByte = tapeBuffer[tapePos + 5] & 0xff;
                    endBlockPause = readInt(tapeBuffer, tapePos + 6, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    blockLen = readInt(tapeBuffer, tapePos + 8, 3);
                    tapePos += 11;
                    state = State.NEWBYTE_NOCHG;
                    idxHeader++;
                    repeat = false;
                    break;
                case CdtBlock.DIRECT_RECORDING: // Direct Data Block
                    LOGGER.debug("Direct Recording block");
                    zeroLength = adjustDuration(readInt(tapeBuffer, tapePos + 1, 2));
                    endBlockPause = readInt(tapeBuffer, tapePos + 3, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    bitsLastByte = tapeBuffer[tapePos + 5] & 0xff;
                    blockLen = readInt(tapeBuffer, tapePos + 6, 3);
                    tapePos += 9;
                    state = State.NEWDR_BYTE;
                    idxHeader++;
                    repeat = false;
                    break;
                case CdtBlock.CSW_RECORDING:
                    LOGGER.debug("CSW Recording block");
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
                    // Undone on first execution
                    ppi.changeCasseteDataInput();
                    repeat = false;
                    break;
                case CdtBlock.GENERALIZED_DATA:
                    LOGGER.warn("Generalized Data block (Unsupported). Skipping");
                    endBlockPause = readInt(tapeBuffer, tapePos + 5, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    idxHeader++;
                    break;
                case CdtBlock.SILENCE:
                    LOGGER.debug("Pause or Stop the Tape block");
                    endBlockPause = readInt(tapeBuffer, tapePos + 1, 2)
                            * (END_BLOCK_PAUSE / 1000);
                    tapePos += 3;
                    state = State.PAUSE_STOP;
                    idxHeader++;
                    repeat = false;
                    break;
                case CdtBlock.GROUP_START:
                    LOGGER.debug("Group Start block");
                    idxHeader++;
                    break;
                case CdtBlock.GROUP_END:
                    LOGGER.debug("Group End block");
                    idxHeader++;
                    break;
                case CdtBlock.JUMP_TO_BLOCK:
                    short target = (short) readInt(tapeBuffer, tapePos + 1, 2);
                    LOGGER.debug("Jump to Block {} block", target);
                    idxHeader += target;
                    break;
                case CdtBlock.LOOP_START:
                    nLoops = readInt(tapeBuffer, tapePos + 1, 2);
                    LOGGER.debug("Loop Start ({}) block", nLoops);
                    loopStart = ++idxHeader;
                    break;
                case CdtBlock.LOOP_END:
                    LOGGER.debug("Loop End block. Remaining {}", nLoops);
                    if (--nLoops == 0) {
                        idxHeader++;
                        break;
                    }
                    idxHeader = loopStart;
                    break;
                case CdtBlock.CALL_SEQUENCE:
                    LOGGER.debug("Call Sequence block");
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
                case CdtBlock.RETURN_FROM_SEQUENCE:
                    LOGGER.debug("Return from Sequence block");
                    if (nCalls < callSeq.length) {
                        idxHeader = callBlk + callSeq[nCalls++];
                    } else {
                        idxHeader = callBlk + 1;
                        callSeq = null;
                    }
                    break;
                case CdtBlock.SELECT_BLOCK:
                    LOGGER.debug("Select Block block");
                    idxHeader++;
                    break;
                case CdtBlock.STOP_TAPE_48KMODE:
                    LOGGER.debug("Stop Tape in 48K Mode block");
                    idxHeader++;
                    break;
                case CdtBlock.SET_SIGNAL_LEVEL:
                    LOGGER.debug("Set Signal Level block");
                    ppi.setCasseteDataInput(tapeBuffer[tapePos + 5] != 0);
                    idxHeader++;
                    break;
                case CdtBlock.TEXT_DESCRIPTION:
                    LOGGER.debug("Text Description block");
                    idxHeader++;
                    break;
                case CdtBlock.MESSAGE_BLOCK:
                    LOGGER.debug("Message block");
                    idxHeader++;
                    break;
                case CdtBlock.ARCHIVE_INFO:
                    LOGGER.debug("Archive Info block");
                    idxHeader++;
                    break;
                case CdtBlock.HARDWARE_TYPE:
                    LOGGER.debug("Hardware Type block");
                    idxHeader++;
                    break;
                case CdtBlock.CUSTOM_INFO_BLOCK:
                    LOGGER.debug("Custom Info block");
                    idxHeader++;
                    break;
                case CdtBlock.GLUE_BLOCK:
                    LOGGER.debug("Glue block");
                    idxHeader++;
                    break;
                default:
                    LOGGER.warn("Unrecognized CdtBlock of type {}", String
                            .format("%02x", tapeBuffer[tapePos]));
                    repeat = false;
                    idxHeader++;
            }
        }
        LOGGER.debug("At position {}, block {}", tapePos, idxHeader);
        notifyBlockChangeListeners(currentBlock);
    }


    public void play() {
        if (!playing) {
            if (idxHeader > blockOffsets.size()) {
                throw new IllegalStateException("Trying to play with blocks exhausted");
            }
            state = State.START;
            tapePos = blockOffsets.get(idxHeader);
            clock.addClockTimeoutListener(this);
            clockTimeout();
            playing = true;
        }
    }


    public void stop() {
        if (playing) {
            state = State.STOP;
            clock.removeClockTimeoutListener(this);
            playing = false;
            LOGGER.debug("On tape stop pos: {}/{}, header: {}/{}",
                    tapePos, tapeBuffer.length,
                    idxHeader, blockOffsets.size());

            eot = (tapePos >= tapeBuffer.length) || (idxHeader >= blockOffsets.size());
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
            throw new TapeFinishedException("CdtTapePlayer completed");
        }
    }

    @Override
    public String toString() {
        return "CdtTapePlayer{" +
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
