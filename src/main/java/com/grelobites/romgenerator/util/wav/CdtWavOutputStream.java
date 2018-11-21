package com.grelobites.romgenerator.util.wav;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.tape.CdtBlockId;
import com.grelobites.romgenerator.util.tape.CdtHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.InflaterInputStream;

public class CdtWavOutputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdtWavOutputStream.class);
    private WavOutputStream output;
    private byte[] tapeBuffer;
    private boolean casseteInput = false;

    private static final int LEADER_LENGHT = 2168;
    private static final int SYNC1_LENGHT = 667;
    private static final int SYNC2_LENGHT = 735;
    private static final int ZERO_LENGHT = 855;
    private static final int ONE_LENGHT = 1710;
    private static final int HEADER_PILOT_PULSES = 8063;
    private static final int DATA_PILOT_PULSES = 3223;
    private static final int MILLISECOND_TSTATES = 3500;

    public CdtWavOutputStream(WavFormat format, InputStream is, OutputStream os) {
        try {
            Optional<CdtHeader> header = CdtHeader.fromInputStream(is);
            if (header.isPresent()) {
                tapeBuffer = Util.fromInputStream(is);
            } else {
                throw new IllegalArgumentException("No header found in cdt tape stream");
            }
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Trying to read cdt tape stream", ioe);
        }

        this.output = new WavOutputStream(os, format);
    }

    private static int readInt(byte buffer[], int start, int len) {
        int res = 0;
        for (int idx = 0; idx < len; idx++) {
            res |= ((buffer[start + idx] << (idx * 8)) & (0xff << idx * 8));
        }
        return res;
    }

    private static void breakWithError(int offset) {
        throw new IllegalArgumentException("Invalid block/offset detected at " + offset);
    }

    private static int[] calculateBlockOffsets(byte[] tapeBuffer) {
        int offset = 0;
        int len;
        List<Integer> blockOffsets = new ArrayList<>();

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
        return blockOffsets.stream().mapToInt(i -> i).toArray();
    }

    private void dumpLeader(int leaderLength, int leaderPulses) {
        for (int i = 0; i < leaderPulses; i++) {
            output.writeValue(leaderLength, casseteInput);
            casseteInput = !casseteInput;
        }
    }

    private void dumpPulseSequence(int currentTapePosition, int pulses) {
        for (int i = 0; i < pulses; i++) {
            int length = readInt(tapeBuffer, currentTapePosition, 2);
            output.writeValue(length, casseteInput);
            casseteInput = !casseteInput;
            currentTapePosition += 2;
        }
    }

    private void dumpSync(int sync1Length, int sync2Length) {
        output.writeValue(sync1Length, casseteInput);
        casseteInput = !casseteInput;
        output.writeValue(sync2Length, casseteInput);
    }

    private void dumpPause(int pauseLength) {
        output.writeValue(pauseLength, false);
    }

    private void dumpData(int position, int length,
                          int zeroLength, int oneLength, int bitsLastByte) {
        while (length-- > 0) {
            int mask = 0x80;
            for (int j = (length > 0) ? 8 : bitsLastByte; j > 0; j--) {
                int duration = (tapeBuffer[position] & mask) != 0 ? oneLength : zeroLength;
                output.writeValue(duration, casseteInput);
                casseteInput = !casseteInput;
                output.writeValue(duration, casseteInput);
                casseteInput = !casseteInput;
                mask >>>= 1;
            }
            position++;
        }
        output.writeValue(MILLISECOND_TSTATES, casseteInput);
    }

    private void dumpDrData(int position, int length,
                            int bitLength, int bitsLastByte) {
        while (length-- > 0) {
            int mask = 0x80;
            for (int j = (length > 0) ? 8 : bitsLastByte; j > 0; j--) {
                output.writeValue(bitLength, (tapeBuffer[position] & mask) != 0);
                mask >>>= 1;
            }
            position++;
        }
    }

    private void dumpRleData(int position, int length, float cswStatesSample) {
        while (length-- > 0) {
            int duration = tapeBuffer[position++] & 0xff;
            if (duration == 0) {
                duration = readInt(tapeBuffer, position, 4);
                position += 4;
                length -= 4;
            }
            duration *= cswStatesSample;
            output.writeValue(duration, casseteInput);
            casseteInput = !casseteInput;
        }
    }

    private void dumpZrleData(int position, int length, float cswStatesSample) {
        try (InflaterInputStream iis = new InflaterInputStream(
                new ByteArrayInputStream(tapeBuffer, position, length))) {
            int duration;
            while ((duration = iis.read()) >= 0) {
                if (duration == 0) {
                    byte nSamples[] = new byte[4];
                    while (duration < 4) {
                        int count = iis.read(nSamples, duration,
                                nSamples.length - duration);
                        if (count == -1) {
                            return;
                        }
                        duration += count;
                    }
                    if (duration == 4) {
                        duration = readInt(nSamples, 0, 4);
                    }
                }
                if (duration > 0) {
                    duration *= cswStatesSample;
                    output.writeValue(duration, casseteInput);
                    casseteInput = !casseteInput;
                }
            }
        } catch (IOException ioe) {
            LOGGER.error("Opening compressed data stream", ioe);
        }
    }

    public void flush() throws IOException {
        int currentTapePosition;
        int zeroLength;
        int oneLength;
        int leaderLength;
        int leaderPulses;
        int sync1Length;
        int sync2Length;
        int bitsLastByte;
        int endBlockPause;
        int currentBlockLength;
        int nCalls = 0;
        short[] callSeq = null;
        int callBlk = 0;
        int nLoops = 0;
        int loopStart = 0;

        final int[] blockOffsets = calculateBlockOffsets(tapeBuffer);
        LOGGER.debug("Block offsets are " + Arrays.toString(blockOffsets));
        int currentBlockIndex = 0;

        while (currentBlockIndex < blockOffsets.length) {
            currentTapePosition = blockOffsets[currentBlockIndex];
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
                    currentBlockIndex++;
                    endBlockPause *= MILLISECOND_TSTATES;
                    LOGGER.debug("Standard Speed block [endBlockPause={}ms, leaderPulses={}, currentBlockLength={}, tapePosition={}]",
                            endBlockPause / MILLISECOND_TSTATES, leaderPulses, currentBlockLength, currentTapePosition);
                    dumpLeader(leaderLength, leaderPulses);
                    dumpSync(sync1Length, sync2Length);
                    dumpData(currentTapePosition, currentBlockLength, zeroLength, oneLength, bitsLastByte);
                    dumpPause(endBlockPause);
                    break;
                case CdtBlockId.TURBO_SPEED:
                    leaderLength = readInt(tapeBuffer, currentTapePosition + 1, 2);
                    sync1Length = readInt(tapeBuffer, currentTapePosition + 3, 2);
                    sync2Length = readInt(tapeBuffer, currentTapePosition + 5, 2);
                    zeroLength = readInt(tapeBuffer, currentTapePosition + 7, 2);
                    oneLength = readInt(tapeBuffer, currentTapePosition + 9, 2);
                    leaderPulses = readInt(tapeBuffer, currentTapePosition + 11, 2);
                    bitsLastByte = tapeBuffer[currentTapePosition + 13] & 0xff;
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 14, 2);
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 16, 3);
                    currentTapePosition += 19;
                    currentBlockIndex++;
                    endBlockPause *= MILLISECOND_TSTATES;
                    LOGGER.debug("Turbo Speed block[leaderLength={}, sync1Length={}, sync2Length={}, zeroLength={}, " +
                                    "oneLength={}, leaderPulses={}, bitsLastByte={}, endBlockPause={}ms, currentBlockLength={}, " +
                                    "tapePosition={}]",
                            leaderLength, sync1Length, sync2Length,
                            zeroLength, oneLength, leaderPulses, bitsLastByte, endBlockPause / MILLISECOND_TSTATES,
                            currentBlockLength, currentTapePosition);
                    dumpLeader(leaderLength, leaderPulses);
                    dumpSync(sync1Length, sync2Length);
                    dumpData(currentTapePosition, currentBlockLength, zeroLength, oneLength, bitsLastByte);
                    dumpPause(endBlockPause);
                    break;
                case CdtBlockId.PURE_TONE:
                    leaderLength = readInt(tapeBuffer, currentTapePosition + 1, 2);
                    leaderPulses = readInt(tapeBuffer, currentTapePosition + 3, 2);
                    currentBlockIndex++;
                    LOGGER.debug("Pure Tone block [leaderLength={}, leaderPulses={}]",
                            leaderLength, leaderPulses);
                    dumpLeader(leaderLength, leaderPulses);
                    break;
                case CdtBlockId.PULSE_SEQUENCE:
                    leaderPulses = tapeBuffer[currentTapePosition + 1] & 0xff;
                    currentTapePosition += 2;
                    currentBlockIndex++;
                    LOGGER.debug("Pulse Sequence block [leaderPulses={}]", leaderPulses);
                    dumpPulseSequence(currentTapePosition, leaderPulses);
                    break;
                case CdtBlockId.PURE_DATA_BLOCK:
                    LOGGER.debug("Pure Data block");
                    zeroLength = readInt(tapeBuffer, currentTapePosition + 1, 2);
                    oneLength = readInt(tapeBuffer, currentTapePosition + 3, 2);
                    bitsLastByte = tapeBuffer[currentTapePosition + 5] & 0xff;
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 6, 2)
                            * MILLISECOND_TSTATES;
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 8, 3);
                    currentTapePosition += 11;
                    currentBlockIndex++;
                    LOGGER.debug("Pure data block [zeroLength={}, oneLength={}, bitsLastByte={}, endBlockPause={}ms, currentBlockLength={}]",
                            zeroLength,
                            oneLength,
                            bitsLastByte,
                            endBlockPause / MILLISECOND_TSTATES,
                            currentBlockLength);
                    dumpData(currentTapePosition, currentBlockLength, zeroLength, oneLength, bitsLastByte);
                    dumpPause(endBlockPause);
                    break;
                case CdtBlockId.DIRECT_RECORDING: // Direct Data Block
                    LOGGER.debug("Direct Recording block");
                    zeroLength = readInt(tapeBuffer, currentTapePosition + 1, 2);
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 3, 2)
                            * MILLISECOND_TSTATES;
                    bitsLastByte = tapeBuffer[currentTapePosition + 5] & 0xff;
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 6, 3);
                    currentTapePosition += 9;
                    dumpDrData(currentTapePosition, currentBlockLength, zeroLength, bitsLastByte);
                    dumpPause(endBlockPause);
                    currentBlockIndex++;
                    break;
                case CdtBlockId.CSW_RECORDING:
                    LOGGER.debug("CSW Recording block");
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 5, 2)
                            * MILLISECOND_TSTATES;
                    float cswStatesSample = 3500000.0f / readInt(tapeBuffer, currentTapePosition + 7, 3);
                    currentBlockLength = readInt(tapeBuffer, currentTapePosition + 1, 4) - 10;
                    if (tapeBuffer[currentTapePosition + 10] == 0x02) {
                        dumpZrleData(currentTapePosition + 15, currentBlockLength, cswStatesSample);
                    } else {
                        dumpRleData(currentTapePosition + 15, currentBlockLength, cswStatesSample);
                    }
                    dumpPause(endBlockPause);
                    currentBlockIndex++;
                    break;
                case CdtBlockId.GENERALIZED_DATA:
                    LOGGER.warn("Generalized Data block (Unsupported). Skipping");
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 5, 2)
                            * MILLISECOND_TSTATES;
                    dumpPause(endBlockPause);
                    currentBlockIndex++;
                    break;
                case CdtBlockId.SILENCE:
                    LOGGER.debug("Pause or Stop the Tape block");
                    endBlockPause = readInt(tapeBuffer, currentTapePosition + 1, 2)
                            * MILLISECOND_TSTATES;
                    currentBlockIndex++;
                    dumpPause(endBlockPause);
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
                    currentBlockIndex++;
            }
        }
        output.flush();
    }
}

