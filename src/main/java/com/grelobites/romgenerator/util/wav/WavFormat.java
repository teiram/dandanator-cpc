package com.grelobites.romgenerator.util.wav;

public class WavFormat {
    public static final int SRATE_44100 = 44100;
    public static final int SRATE_48000 = 48000;

    private static final int DEFAULT_LOW_VALUE = 0x40;
    private static final int DEFAULT_HIGH_VALUE = 0xC0;

    private int sampleRate = SRATE_48000;
    private ChannelType channelType = ChannelType.STEREOINV;
    private int lowValue = DEFAULT_LOW_VALUE;
    private int highValue = DEFAULT_HIGH_VALUE;
    private boolean reversePhase = false;

    public static class Builder {
        private WavFormat outputFormat = new WavFormat();

        public Builder withSampleRate(int sampleRate) {
            outputFormat.setSampleRate(sampleRate);
            return this;
        }

        public Builder withChannelType(ChannelType channelType) {
            outputFormat.setChannelType(channelType);
            return this;
        }

        public Builder withReversePhase(boolean reversePhase) {
            outputFormat.setReversePhase(reversePhase);
            return this;
        }

        public Builder withLowValue(int lowValue) {
            outputFormat.setLowValue(lowValue);
            return this;
        }

        public Builder withHighValue(int highValue) {
            outputFormat.setHighValue(highValue);
            return this;
        }

        public WavFormat build() {
            return outputFormat;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public WavFormat(int sampleRate, ChannelType channelType,
                     int pilotDurationMillis) {
        this.sampleRate = sampleRate;
        this.channelType = channelType;
    }

    public WavFormat() {}

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public int getLowValue() {
        return lowValue;
    }

    public void setLowValue(int lowValue) {
        this.lowValue = lowValue;
    }

    public int getHighValue() {
        return highValue;
    }

    public void setHighValue(int highValue) {
        this.highValue = highValue;
    }

    public boolean isReversePhase() {
        return reversePhase;
    }

    public void setReversePhase(boolean reversePhase) {
        this.reversePhase = reversePhase;
    }

}
