package com.grelobites.romgenerator.util.emulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Clock {
    private static final Logger LOGGER = LoggerFactory.getLogger(Clock.class);

    private long tstates;
    private final List<ClockTimeout> clockTimeouts = new ArrayList<>();

    public void addClockTimeout(final ClockTimeout clockTimeout) {
        clockTimeouts.add(clockTimeout);
    }

    public void removeClockTimeout(final ClockTimeout clockTimeout) {
        for (int i = clockTimeouts.size() - 1; i >= 0; i--) {
            if (clockTimeouts.get(i).equals(clockTimeout))  {
                clockTimeouts.remove(i);
            }
        }
    }

    public long getTstates() {
        return tstates;
    }

    public void setTstates(long states) {
        tstates = states;
        for (ClockTimeout clockTimeout: clockTimeouts) {
            clockTimeout.remaining = 0;
        }
    }

    public void addTstates(long states) {
        tstates += states;

        for (ClockTimeout clockTimeout: clockTimeouts) {
            if (clockTimeout.remaining > 0) {
                clockTimeout.remaining -= states;
                if (clockTimeout.remaining <= 0) {
                    long error = clockTimeout.remaining;
                    clockTimeout.getListener().timeout(states);

                    if (clockTimeout.remaining > 0 && error < 0) {
                        //Timeout rearmed: Substract error from the last remaining
                        clockTimeout.remaining += error;
                    }
                }
            }
        }
    }

    public void reset() {
        tstates = 0;
        for (ClockTimeout clockTimeout: clockTimeouts) {
            clockTimeout.remaining = 0;
        }
    }

}
