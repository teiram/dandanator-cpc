package com.grelobites.romgenerator.util.emulator;

public class ClockTimeout {
    protected long remaining;
    private ClockTimeoutListener listener;

    public long remaining() {
        return remaining;
    }

    public void append(long value) {
        remaining += value;
    }

    public ClockTimeoutListener getListener() {
        return listener;
    }

    public void setListener(ClockTimeoutListener listener) {
        this.listener = listener;
    }

    //Minimum remaining to 10-tstates. Why?
    public void setTimeout(long remaining) {
        this.remaining = remaining > 10 ? remaining : 10;
    }

    @Override
    public String toString() {
        return "ClockTimeout{" +
                "remaining=" + remaining +
                '}';
    }
}
