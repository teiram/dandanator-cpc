package com.grelobites.romgenerator.util.emulator;

import com.grelobites.romgenerator.util.tape.CDTTapePlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoaderDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderDetector.class);
    private long lastTstates = 0;
    private int lastB = 0;
    private final CDTTapePlayer tapePlayer;
    private final Clock clock;
    private int successiveReads;

    public LoaderDetector(CDTTapePlayer tapePlayer) {
        this.tapePlayer = tapePlayer;
        this.clock = tapePlayer.getClock();
    }

    public void reset() {
        lastTstates = clock.getTstates();
        lastB = 0;
    }

    public void onAudioInput(Z80 processor) {
        if (!tapePlayer.isEOT()) {
            long tstatesDelta = clock.getTstates() - lastTstates;
            int bdiff = (processor.getRegB() - lastB) & 0xff;

            //LOGGER.debug("On audio detector with bdiff " + bdiff + " and tstatesDelta " + tstatesDelta);
            if (tapePlayer.isPlaying()) {
                if (tstatesDelta > 1000 ||
                        (bdiff != 1 && bdiff != 0 && bdiff != 0xff)) {
                    successiveReads++;
                    if (successiveReads >= 2) {
                        LOGGER.debug("LoaderDetector stops tapePlayer " + tapePlayer
                                + " on tstatesDelta "
                                + tstatesDelta + ", bdiff " + bdiff);
                        tapePlayer.stop();
                    }
                } else {
                    successiveReads = 0;
                }
            } else {
                if (tstatesDelta <= 500 && (bdiff == 1 || bdiff == 0xff)) {
                    successiveReads++;
                    if (successiveReads >= 10) {
                        LOGGER.debug("LoaderDetector starts tapePlayer " + tapePlayer
                                + " on tstatesDelta "
                                + tstatesDelta + ", bdiff " + bdiff);
                        tapePlayer.play();
                    }
                } else {
                    successiveReads = 0;
                }
            }
            lastB = processor.getRegB();
            lastTstates = clock.getTstates();
        }
    }

}
