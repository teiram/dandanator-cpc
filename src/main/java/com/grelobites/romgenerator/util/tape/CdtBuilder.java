package com.grelobites.romgenerator.util.tape;

import com.grelobites.romgenerator.util.tape.cdtblock.PauseBlock;
import com.grelobites.romgenerator.util.tape.cdtblock.StandardSpeedDataBlock;
import com.grelobites.romgenerator.util.tape.cdtblock.TurboSpeedDataBlock;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CdtBuilder {
    private static final int INITIAL_PAUSE_LENGTH = 5000;
    private static final int PILOT_LENGTH = 2340;
    private static final int PILOT_PULSES = 4096;
    private static final int SYNC1_LENGTH = 1190;
    private static final int SYNC2_LENGTH = SYNC1_LENGTH;
    private static final int ZERO_LENGTH = 1187;
    private static final int ONE_LENGTH = 2375;
    private static final int HEADER_PAUSE_LENGTH = 14;
    private static final int BLOCK_PAUSE_LENGTH = 2000;

    private List<CdtBlock> blocks = new ArrayList<>();

    public void addCdtBlock(CdtBlock block) {
        blocks.add(block);
    }

    public void dump(OutputStream os) throws IOException {
        new CdtHeader().dump(os);
        for (CdtBlock block: blocks) {
            block.dump(os);
        }
    }

    public void addBinary(Binary binary) {
        addCdtBlock(PauseBlock.builder().withPauseLength(INITIAL_PAUSE_LENGTH).build());

        BinaryRecordIterator iterator = new BinaryRecordIterator(binary);
        byte [] dataRecord;
        boolean firstRecord = true;
        while ((dataRecord = iterator.next()) != null) {
            addCdtBlock(TurboSpeedDataBlock.builder()
                    .withPilotLength(PILOT_LENGTH)
                    .withPilotPulses(PILOT_PULSES)
                    .withSync1Length(SYNC1_LENGTH)
                    .withSync2Length(SYNC2_LENGTH)
                    .withZeroLength(ZERO_LENGTH)
                    .withOneLength(ONE_LENGTH)
                    .withData(dataRecord)
                    .withPauseLength(firstRecord ? HEADER_PAUSE_LENGTH : BLOCK_PAUSE_LENGTH)
                    .build());
            firstRecord = false;
        }
    }
}
