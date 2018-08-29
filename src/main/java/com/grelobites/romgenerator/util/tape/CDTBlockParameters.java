package com.grelobites.romgenerator.util.tape;

public class CDTBlockParameters {
    private int leaderLength;
    private int sync1Length;
    private int sync2Length;
    private int zeroLength;
    private int oneLength;
    private int leaderPulses;
    private int bitsLastByte;
    private int endBlockPause;
    private int blockLength;

    //Adjust duration in 3.5Mhz clock pulses to 4Mhz clock pulses
    private static int adjustDuration(int duration) {
        return (duration * 40) / 35;
    }

    public int getLeaderLength() {
        return leaderLength;
    }

    public void setLeaderLength(int leaderLength) {
        this.leaderLength = leaderLength;
    }

    public int getSync1Length() {
        return sync1Length;
    }

    public void setSync1Length(int sync1Length) {
        this.sync1Length = sync1Length;
    }

    public int getSync2Length() {
        return sync2Length;
    }

    public void setSync2Length(int sync2Length) {
        this.sync2Length = sync2Length;
    }

    public int getZeroLength() {
        return zeroLength;
    }

    public void setZeroLength(int zeroLength) {
        this.zeroLength = zeroLength;
    }

    public int getOneLength() {
        return oneLength;
    }

    public void setOneLength(int oneLength) {
        this.oneLength = oneLength;
    }

    public int getLeaderPulses() {
        return leaderPulses;
    }

    public void setLeaderPulses(int leaderPulses) {
        this.leaderPulses = leaderPulses;
    }

    public int getBitsLastByte() {
        return bitsLastByte;
    }

    public void setBitsLastByte(int bitsLastByte) {
        this.bitsLastByte = bitsLastByte;
    }

    public int getEndBlockPause() {
        return endBlockPause;
    }

    public void setEndBlockPause(int endBlockPause) {
        this.endBlockPause = endBlockPause;
    }

    public int getBlockLength() {
        return blockLength;
    }

    public void setBlockLength(int blockLength) {
        this.blockLength = blockLength;
    }
}
