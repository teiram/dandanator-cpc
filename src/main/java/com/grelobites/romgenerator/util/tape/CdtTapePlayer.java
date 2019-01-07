package com.grelobites.romgenerator.util.tape;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.emulator.Clock;
import com.grelobites.romgenerator.util.emulator.ClockTimeout;
import com.grelobites.romgenerator.util.emulator.ClockTimeoutListener;
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

    //Standard block timings
    private static final int LEADER_LENGHT          = adjustDuration(2168);
    private static final int SYNC1_LENGHT           = adjustDuration(667);
    private static final int SYNC2_LENGHT           = adjustDuration(735);
    private static final int ZERO_LENGHT            = adjustDuration(855);
    private static final int ONE_LENGHT             = adjustDuration(1710);
    private static final int HEADER_PILOT_PULSES    = adjustDuration(8063);
    private static final int DATA_PILOT_PULSES      = adjustDuration(3223);
    private static final int MILLISECOND_TSTATES    = 4000;

    public enum State {
        START, STOP, LAST_PULSE, PAUSE, PAUSE_STOP,
        LEADER, LEADER_NOCHANGE, SYNC,
        NEWBYTE, NEWBYTE_NOCHANGE, NEWBIT, SECOND_HALF_BIT,
        TZX_HEADER,
        PURE_TONE, PURE_TONE_NOCHANGE,
        PULSE_SEQUENCE, PULSE_SEQUENCE_NOCHANGE,
        NEWDR_BYTE, NEWDR_BIT,
        CSW_RLE, CSW_ZRLE
    }


    private State state;
    private boolean playing;
    private boolean invertedOutput = false;
    private byte[] tapeBuffer;
    private int currentBlockIndex;
    private int currentTapePosition;
    private int currentBlockLength;
    private int bitTime;
    private final Clock clock;
    private ClockTimeout clockTimeout;
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
    private boolean casseteInput = false;

    private List<BlockChangeListener> blockChangeListeners = new ArrayList<>();

    //Adjust duration in 3.5Mhz clock pulses to 4Mhz clock pulses
    private static int adjustDuration(int duration) {
        int adjustedDuration = (40 * duration) / 35;
        LOGGER.debug("adjustDuration {} -> {}", duration, adjustedDuration);
        return adjustedDuration;
    }

    public CdtTapePlayer(Clock clock, Ppi ppi) {
        this.clock = clock;
        this.ppi = ppi;
        state = State.STOP;
        currentTapePosition = 0;
        casseteInput = invertedOutput;
        currentBlockIndex = 0;
        playing = false;
        clockTimeout = new ClockTimeout();
        clockTimeout.setListener(this);
    }

    public void addBlockChangeListener(BlockChangeListener listener) {
        blockChangeListeners.add(listener);
    }

    public void removeBlockChangeListener(BlockChangeListener listener) {
        blockChangeListeners.remove(listener);
    }

    public void setInvertedOutput(boolean invertedOutput) {
        this.invertedOutput = invertedOutput;
    }

    public boolean isInvertedOutput() {
        return invertedOutput;
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
        return String.format("Playing=%s, state=%s, position=%d, block=%d/%d",
                playing, state, currentTapePosition, currentBlockIndex + 1, blockOffsets.size());
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
                case CdtBlockId.STANDARD_SPEED:
                    if (tapeBuffer.length - offset < 5) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 3, 2);
                    offset += len + 5;
                    break;
                case CdtBlockId.TURBO_SPEED:
                    if (tapeBuffer.length - offset < 19) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 16, 3);
                    offset += len + 19;
                    break;
                case CdtBlockId.PURE_TONE:
                    offset += 5;
                    break;
                case CdtBlockId.PULSE_SEQUENCE:
                    if (tapeBuffer.length - offset < 2) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 1] & 0xff;
                    offset += len * 2 + 2;
                    break;
                case CdtBlockId.PURE_DATA_BLOCK:
                    if (tapeBuffer.length - offset < 11) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 8, 3);
                    offset += len + 11;
                    break;
                case CdtBlockId.DIRECT_RECORDING:
                    if (tapeBuffer.length - offset < 9) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 6, 3);
                    offset += len + 9;
                    break;
                case CdtBlockId.CSW_RECORDING:
                case CdtBlockId.GENERALIZED_DATA:
                    if (tapeBuffer.length - offset < 5) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 1, 4);
                    offset += len + 5;
                    break;
                case CdtBlockId.SILENCE:
                case CdtBlockId.JUMP_TO_BLOCK:
                case CdtBlockId.LOOP_START:
                    offset += 3;
                    break;
                case CdtBlockId.GROUP_START:
                    if (tapeBuffer.length - offset < 2) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 1] & 0xff;
                    offset += len + 2;
                    break;
                case CdtBlockId.GROUP_END:
                case CdtBlockId.LOOP_END:
                case CdtBlockId.RETURN_FROM_SEQUENCE:
                    offset++;
                    break;
                case CdtBlockId.CALL_SEQUENCE:
                    if (tapeBuffer.length - offset < 3) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 1, 2);
                    offset += len * 2 + 3;
                    break;
                case CdtBlockId.SELECT_BLOCK:
                case CdtBlockId.ARCHIVE_INFO:
                    if (tapeBuffer.length - offset < 3) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 1, 2);
                    offset += len + 3;
                    break;
                case CdtBlockId.STOP_TAPE_48KMODE:
                    offset += 5;
                    break;
                case CdtBlockId.SET_SIGNAL_LEVEL:
                    offset += 6;
                    break;
                case CdtBlockId.TEXT_DESCRIPTION:
                    if (tapeBuffer.length - offset < 2) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 1] & 0xff;
                    offset += len + 2;
                    break;
                case CdtBlockId.MESSAGE_BLOCK:
                    if (tapeBuffer.length - offset < 3) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 2] & 0xff;
                    offset += len + 3;
                    break;
                case CdtBlockId.HARDWARE_TYPE:
                    if (tapeBuffer.length - offset < 2) {
                        breakWithError(offset);
                    }
                    len = tapeBuffer[offset + 1] & 0xff;
                    offset += len * 3 + 2;
                    break;
                case CdtBlockId.CUSTOM_INFO_BLOCK:
                    if (tapeBuffer.length - offset < 21) {
                        breakWithError(offset);
                    }
                    len = readInt(tapeBuffer, offset + 17, 4);
                    offset += len + 21;
                    break;
                case CdtBlockId.GLUE_BLOCK:
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

        currentTapePosition = currentBlockIndex = readBytes = 0;
        blockOffsets.clear();
        eot = playing = false;

        state = State.STOP;
        findBlockOffsets();
    }

    public void rewind() {
        state = State.STOP;
        currentTapePosition = currentBlockIndex = readBytes = 0;
        eot = playing = false;
    }

    public void eject() {
        stop();
        currentTapePosition = currentBlockIndex = readBytes = 0;
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
                    currentTapePosition = blockOffsets.get(currentBlockIndex);
                    casseteInput = invertedOutput;
                    state = State.TZX_HEADER;
                    repeat = true;
                    break;
                case LEADER:
                    casseteInput = !casseteInput;
                case LEADER_NOCHANGE:
                    if (leaderPulses-- > 0) {
                        state = State.LEADER;
                        clockTimeout.setTimeout(leaderLength);
                        break;
                    }
                    clockTimeout.setTimeout(sync1Length);
                    state = State.SYNC;
                    break;
                case SYNC:
                    casseteInput = !casseteInput;
                    clockTimeout.setTimeout(sync2Length);
                    state = currentBlockLength > 0 ? State.NEWBYTE : State.PAUSE;
                    break;
                case NEWBYTE_NOCHANGE:
                    //To Undo on NEWBIT case
                    casseteInput = !casseteInput;
                case NEWBYTE:
                    mask = 0x80; //Starts on MSB bit
                case NEWBIT:
                    casseteInput = !casseteInput;
                    if ((tapeBuffer[currentTapePosition] & mask) == 0) {
                        bitTime = zeroLength;
                    } else {
                        bitTime = oneLength;
                    }
                    state = State.SECOND_HALF_BIT;
                    clockTimeout.setTimeout(bitTime);
                    break;
                case SECOND_HALF_BIT:
                    casseteInput = !casseteInput;
                    clockTimeout.setTimeout(bitTime);
                    mask >>>= 1;
                    if (currentBlockLength == 1 && bitsLastByte < 8) {
                        if (mask == (0x80 >>> bitsLastByte)) {
                            state = State.LAST_PULSE;
                            currentBlockLength = 0;
                            currentTapePosition++;
                            break;
                        }
                    }
                    if (mask != 0) {
                        state = State.NEWBIT;
                        break;
                    }
                    currentTapePosition++;
                    if (--currentBlockLength > 0) {
                        state = State.NEWBYTE;
                    } else {
                        state = State.LAST_PULSE;
                    }
                    break;
                case LAST_PULSE:
                    casseteInput = !casseteInput;
                    state = State.PAUSE;
                    repeat = true;
                    break;
                case PAUSE:
                    state = State.TZX_HEADER;
                    if (endBlockPause == 0) {
                        repeat = true;
                    } else {
                        clockTimeout.setTimeout(endBlockPause);
                    }
                    break;
                case TZX_HEADER:
                    if (currentBlockIndex >= blockOffsets.size()) {
                        state = State.STOP;
                        repeat = true;
                        break;
                    }
                    decodeTzxHeader();
                    repeat = true;
                    break;
                case PURE_TONE:
                    casseteInput = !casseteInput;
                case PURE_TONE_NOCHANGE:
                    if (leaderPulses-- > 0) {
                        clockTimeout.setTimeout(leaderLength);
                        state = State.PURE_TONE;
                        break;
                    }
                    state = State.TZX_HEADER;
                    repeat = true;
                    break;
                case PULSE_SEQUENCE:
                    casseteInput = !casseteInput;
                case PULSE_SEQUENCE_NOCHANGE:
                    if (leaderPulses-- > 0) {
                        leaderLength = adjustDuration(readInt(tapeBuffer, currentTapePosition, 2));
                        clockTimeout.setTimeout(leaderLength);
                        currentTapePosition += 2;
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
                    if ((tapeBuffer[currentTapePosition] & mask) != 0) {
                        earState = true;
                        casseteInput = true;
                    } else {
                        earState = false;
                        casseteInput = false;
                    }
                    timeout = 0;

                    while (((tapeBuffer[currentTapePosition] & mask) != 0) == earState) {
                        timeout += zeroLength;

                        mask >>>= 1;
                        if (mask == 0) {
                            mask = 0x80;
                            currentTapePosition++;
                            if (--currentBlockLength == 0) {
                                state = State.LAST_PULSE;
                                break;
                            }
                        } else {
                            if (currentBlockLength == 1 && bitsLastByte < 8) {
                                if (mask == (0x80 >>> bitsLastByte)) {
                                    state = State.LAST_PULSE;
                                    currentTapePosition++;
                                    break;
                                }
                            }
                        }
                    }
                    clockTimeout.setTimeout(timeout);
                    break;
                case PAUSE_STOP:
                    if (endBlockPause == 0) {
                        state = State.TZX_HEADER;
                        repeat = true;
                    } else {
                        casseteInput = invertedOutput;
                        state = State.TZX_HEADER;
                        clockTimeout.setTimeout(endBlockPause);
                    }
                    break;
                case CSW_RLE:
                    if (currentBlockLength == 0) {
                        state = State.PAUSE;
                        repeat = true;
                    }

                    casseteInput = !casseteInput;

                    timeout = tapeBuffer[currentTapePosition++] & 0xff;
                    currentBlockLength--;
                    if (timeout == 0) {
                        timeout = adjustDuration(readInt(tapeBuffer, currentTapePosition, 4));
                        currentTapePosition += 4;
                        currentBlockLength -= 4;
                    }

                    timeout *= cswStatesSample;
                    clockTimeout.setTimeout(timeout);
                    break;
                case CSW_ZRLE:
                    casseteInput = !casseteInput;

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
                        clockTimeout.setTimeout(timeout);

                    } catch (IOException ioe) {
                        LOGGER.warn("Reading stream", ioe);
                        eot = true;
                    }
                    break;
            }
        } while (repeat);
        ppi.setCasseteDataInput(casseteInput);
    }

    private void decodeTzxHeader() {
        boolean repeat = true;
        int currentBlock = currentBlockIndex;

        while (repeat) {
            if (currentBlockIndex >= blockOffsets.size()) {
                return;
            }

            currentTapePosition = blockOffsets.get(currentBlockIndex);

            switch (tapeBuffer[currentTapePosition] & 0xff) {
                case CdtBlockId.STANDARD_SPEED:
                    leaderLength = LEADER_LENGHT;
                    sync1Length = SYNC1_LENGHT;
                    sync2Length = SYNC2_LENGHT;
                    zeroLength = ZERO_LENGHT;
                    oneLength = ONE_LENGHT;
                    bitsLastByte = 8;
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 1, 2);
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 3, 2);
                    currentTapePosition += 5;
                    leaderPulses =
                            (tapeBuffer[currentTapePosition] & 0xff) < 0x80 ? HEADER_PILOT_PULSES : DATA_PILOT_PULSES;
                    state = State.LEADER_NOCHANGE;
                    currentBlockIndex++;
                    /*
                    if (currentBlockIndex >= blockOffsets.size() && endBlockPause > 1000) {
                        endBlockPause = 1;
                    }
                    */
                    endBlockPause *= MILLISECOND_TSTATES;
                    LOGGER.debug("Standard Speed block [endBlockPause={} ms, leaderPulses={}, currentBlockLength={}, tapePosition={}]",
                            endBlockPause / MILLISECOND_TSTATES, leaderPulses, currentBlockLength, currentTapePosition);
                    repeat = false;
                    break;
                case CdtBlockId.TURBO_SPEED:
                    leaderLength =  adjustDuration(readInt(tapeBuffer, currentTapePosition + 1, 2));
                    sync1Length =   adjustDuration(readInt(tapeBuffer, currentTapePosition + 3, 2));
                    sync2Length =   adjustDuration(readInt(tapeBuffer, currentTapePosition + 5, 2));
                    zeroLength =    adjustDuration(readInt(tapeBuffer, currentTapePosition + 7, 2));
                    oneLength =     adjustDuration(readInt(tapeBuffer, currentTapePosition + 9, 2));
                    leaderPulses = readInt(tapeBuffer, currentTapePosition + 11, 2);
                    bitsLastByte = tapeBuffer[currentTapePosition + 13] & 0xff;
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 14, 2);
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 16, 3);
                    currentTapePosition += 19;
                    state = State.LEADER_NOCHANGE;
                    currentBlockIndex++;
                    endBlockPause *= MILLISECOND_TSTATES;
                    LOGGER.debug("Turbo Speed block[leaderLength={}, sync1Length={}, sync2Length={}, zeroLength={}, " +
                            "oneLength={}, leaderPulses={}, bitsLastByte={}, endBlockPause={} ms, currentBlockLength={}, " +
                            "tapePosition={}]",
                        leaderLength, sync1Length, sync2Length,
                        zeroLength, oneLength, leaderPulses, bitsLastByte, endBlockPause / MILLISECOND_TSTATES,
                            currentBlockLength, currentTapePosition);
                    repeat = false;
                    break;
                case CdtBlockId.PURE_TONE:
                    leaderLength =  adjustDuration(readInt(tapeBuffer, currentTapePosition + 1, 2));
                    leaderPulses =  readInt(tapeBuffer, currentTapePosition + 3, 2);
                    currentTapePosition += 5;
                    state = State.PURE_TONE_NOCHANGE;
                    currentBlockIndex++;
                    repeat = false;
                    LOGGER.debug("Pure Tone block [leaderLength={}, leaderPulses={}]",
                            leaderLength, leaderPulses);
                    break;
                case CdtBlockId.PULSE_SEQUENCE:
                    leaderPulses = tapeBuffer[currentTapePosition + 1] & 0xff;
                    currentTapePosition += 2;
                    state = State.PULSE_SEQUENCE_NOCHANGE;
                    currentBlockIndex++;
                    LOGGER.debug("Pulse Sequence block [leaderPulses={}]", leaderPulses);
                    repeat = false;
                    break;
                case CdtBlockId.PURE_DATA_BLOCK:
                    zeroLength =    adjustDuration(readInt(tapeBuffer, currentTapePosition + 1, 2));
                    oneLength =     adjustDuration(readInt(tapeBuffer, currentTapePosition + 3, 2));
                    bitsLastByte = tapeBuffer[currentTapePosition + 5] & 0xff;
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 6, 2)
                            * MILLISECOND_TSTATES;
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 8, 3);
                    currentTapePosition += 11;
                    state = State.NEWBYTE_NOCHANGE;
                    currentBlockIndex++;
                    repeat = false;
                    LOGGER.debug("Pure data block [zeroLength={}, oneLength={}, bitsLastByte={}, endBlockPause={} ms, currentBlockLength={}]",
                            zeroLength,
                            oneLength,
                            bitsLastByte,
                            endBlockPause / MILLISECOND_TSTATES,
                            currentBlockLength);
                    break;
                case CdtBlockId.DIRECT_RECORDING: // Direct Data Block
                    LOGGER.debug("Direct Recording block");
                    zeroLength = adjustDuration(readInt(tapeBuffer, currentTapePosition + 1, 2));
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 3, 2)
                            * MILLISECOND_TSTATES;
                    bitsLastByte = tapeBuffer[currentTapePosition + 5] & 0xff;
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 6, 3);
                    currentTapePosition += 9;
                    state = State.NEWDR_BYTE;
                    currentBlockIndex++;
                    repeat = false;
                    break;
                case CdtBlockId.CSW_RECORDING:
                    LOGGER.debug("CSW Recording block");
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 5, 2)
                            * MILLISECOND_TSTATES;
                    cswStatesSample = 3500000.0f / readInt(tapeBuffer, currentTapePosition + 7, 3);
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 1, 4) - 10;
                    if (tapeBuffer[currentTapePosition + 10] == 0x02) {
                        state = State.CSW_ZRLE;
                        bais = new ByteArrayInputStream(tapeBuffer, currentTapePosition + 15, currentBlockLength);
                        iis = new InflaterInputStream(bais);
                    } else {
                        state = State.CSW_RLE;
                    }
                    currentTapePosition += 15;
                    currentBlockIndex++;
                    // Undone on first execution
                    casseteInput = !casseteInput;
                    repeat = false;
                    break;
                case CdtBlockId.GENERALIZED_DATA:
                    LOGGER.warn("Generalized Data block (Unsupported). Skipping");
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 5, 2)
                            * MILLISECOND_TSTATES;
                    currentBlockIndex++;
                    break;
                case CdtBlockId.SILENCE:
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 1, 2)
                            * MILLISECOND_TSTATES;
                    currentTapePosition += 3;
                    state = State.PAUSE_STOP;
                    currentBlockIndex++;
                    LOGGER.debug("Pause or Stop the Tape block. EndBlockPause {} ms",
                            endBlockPause / MILLISECOND_TSTATES);
                    repeat = false;
                    break;
                case CdtBlockId.GROUP_START:
                    LOGGER.debug("Group Start block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.GROUP_END:
                    LOGGER.debug("Group End block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.JUMP_TO_BLOCK:
                    short target = (short) readInt(tapeBuffer, currentTapePosition + 1, 2);
                    LOGGER.debug("Jump to Block {} block", target);
                    currentBlockIndex += target;
                    break;
                case CdtBlockId.LOOP_START:
                    nLoops = readInt(tapeBuffer, currentTapePosition + 1, 2);
                    LOGGER.debug("Loop Start ({}) block", nLoops);
                    loopStart = ++currentBlockIndex;
                    break;
                case CdtBlockId.LOOP_END:
                    LOGGER.debug("Loop End block. Remaining {}", nLoops);
                    if (--nLoops == 0) {
                        currentBlockIndex++;
                        break;
                    }
                    currentBlockIndex = loopStart;
                    break;
                case CdtBlockId.CALL_SEQUENCE:
                    LOGGER.debug("Call Sequence block");
                    if (callSeq == null) {
                        nCalls = readInt(tapeBuffer, currentTapePosition + 1, 2);
                        callSeq = new short[nCalls];
                        for (int idx = 0; idx < nCalls; idx++) {
                            callSeq[idx] = (short) (readInt(tapeBuffer, currentTapePosition + idx * 2 + 3, 2));
                        }
                        callBlk = currentBlockIndex;
                        nCalls = 0;
                        currentBlockIndex += callSeq[nCalls++];
                    } else {
                        LOGGER.warn("CALL_SEQUENCE blocks can't be nested. Skipping");
                        currentBlockIndex++;
                    }
                    break;
                case CdtBlockId.RETURN_FROM_SEQUENCE:
                    LOGGER.debug("Return from Sequence block");
                    if (nCalls < callSeq.length) {
                        currentBlockIndex = callBlk + callSeq[nCalls++];
                    } else {
                        currentBlockIndex = callBlk + 1;
                        callSeq = null;
                    }
                    break;
                case CdtBlockId.SELECT_BLOCK:
                    LOGGER.debug("Select Block block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.STOP_TAPE_48KMODE:
                    LOGGER.debug("Stop Tape in 48K Mode block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.SET_SIGNAL_LEVEL:
                    LOGGER.debug("Set Signal Level block");
                    casseteInput = tapeBuffer[currentTapePosition + 5] != 0;
                    currentBlockIndex++;
                    break;
                case CdtBlockId.TEXT_DESCRIPTION:
                    LOGGER.debug("Text Description block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.MESSAGE_BLOCK:
                    LOGGER.debug("Message block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.ARCHIVE_INFO:
                    LOGGER.debug("Archive Info block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.HARDWARE_TYPE:
                    LOGGER.debug("Hardware Type block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.CUSTOM_INFO_BLOCK:
                    LOGGER.debug("Custom Info block");
                    currentBlockIndex++;
                    break;
                case CdtBlockId.GLUE_BLOCK:
                    LOGGER.debug("Glue block");
                    currentBlockIndex++;
                    break;
                default:
                    LOGGER.warn("Unrecognized CdtBlockId of type {}", String
                            .format("%02x", tapeBuffer[currentTapePosition]));
                    repeat = false;
                    currentBlockIndex++;
            }
            if (repeat) {
                currentBlock = currentBlockIndex;
            }
        }
        LOGGER.debug("block {} data starting at position {}, tstates {}", currentBlock, currentTapePosition,
                clock.getTstates());
        notifyBlockChangeListeners(currentBlock);
    }


    public void play() {
        if (!playing) {
            if (currentBlockIndex > blockOffsets.size()) {
                throw new IllegalStateException("Trying to play with blocks exhausted");
            }
            LOGGER.debug("On tape play: {}", this);
            state = State.START;
            currentTapePosition = blockOffsets.get(currentBlockIndex);
            clock.addClockTimeout(clockTimeout);
            timeout(0);
            playing = true;
        }
    }

    public void pause() {
        if (playing) {
            LOGGER.debug("On tape pause: {}", this);
            clock.removeClockTimeout(clockTimeout);
            //Compensate for the pause/resume sequence? Lala needs something like this to work
            //But breaks some others like 1942
            /*
            if (currentBlockIndex == 3 && clockTimeout.remaining() < 2000) {
                clockTimeout.append(5000000);
            }
            */
            playing = false;
        }
    }

    public void resume() {
        if (!playing) {
            LOGGER.debug("On tape resume: {}", this);
            clock.addClockTimeout(clockTimeout);
            playing = true;
        }
    }

    public void stop() {
        if (playing) {
            LOGGER.debug("On tape stop: {}", this);
            state = State.STOP;
            clock.removeClockTimeout(clockTimeout);
            playing = false;
            eot = (currentTapePosition >= tapeBuffer.length) || (currentBlockIndex >= blockOffsets.size());
            if (eot && throwOnEot) {
                throw new TapeFinishedException("Tape Finished");
            }
        }
    }

    public int getCurrentTapePosition() {
        return currentTapePosition;
    }

    public boolean isInLastBlock() {
        return currentBlockIndex == blockOffsets.size() &&
                tapeBuffer.length - currentTapePosition < 10;
    }

    public int getTapeLength() {
        return tapeBuffer.length;
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
    public void timeout(long tstates) {
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
                ", currentBlockIndex=" + currentBlockIndex +
                ", currentTapePosition=" + currentTapePosition +
                ", currentBlockLength=" + currentBlockLength +
                ", currentMask=" + String.format("0x%02x", mask) +
                ", blockOffsets=" + blockOffsets +
                ", blocks=" + (blockOffsets != null ? blockOffsets.size() : "undefined") +
                ", tapeBuffer.length=" + (tapeBuffer != null ? tapeBuffer.length : "null") +
                ", clockTimeout=" + (clockTimeout != null ? clockTimeout : "null") +
                ", eot=" + eot +
                '}';
    }
}
