package com.grelobites.romgenerator.util.wav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WavOutputStream {

    private static final int CPC_CLOCK = 4000000;
    private static final int WAV_HEADER_LENGTH = 44;

    private byte[] getWavHeader(int wavDataLength) {
        int byteRate = format.getSampleRate() * format.getChannelType().channels();
        int sampleRate = format.getSampleRate();
        short numChannels = (short) format.getChannelType().channels();

        ByteBuffer buffer = ByteBuffer.allocate(WAV_HEADER_LENGTH)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".getBytes())                                 //ChunkID
                .putInt(wavDataLength + 36)                             //ChunkSize
                .put("WAVE".getBytes())                                 //Format
                .put("fmt ".getBytes())                                 //Subchunk1ID
                .putInt(0x00000010)                                     //Subchunk1Size (16 for PCM)
                .putShort((short) 1)                                    //AudioFormat 1=PCM
                .putShort(numChannels)                                  //NumChannels
                .putInt(sampleRate)                                     //SampleRate
                .putInt(byteRate)                                       //ByteRate
                .putShort(numChannels)                                  //Block align
                .putShort((short) 8)                                    //Bits per sample
                .put("data".getBytes())                                 //Subchunk2ID
                .putInt(wavDataLength);                                 //Subchunk2Size
        return buffer.array();
    }

    private ByteArrayOutputStream wavStream = new ByteArrayOutputStream();
    private final OutputStream out;
    private WavFormat format;

    private long tStatesToSamples(long tstates) {
        long upper = tstates * format.getSampleRate();
        //return upper / CPC_CLOCK + (upper % CPC_CLOCK == 0 ? 0 : 1);
        return (upper + (CPC_CLOCK / 2)) / CPC_CLOCK;
    }

    private int getLowValue() {
        return format.isReversePhase() ? format.getHighValue() : format.getLowValue();
    }

    private int getHighValue() {
        return format.isReversePhase() ? format.getLowValue() : format.getHighValue();
    }

    private void writeSamples(long samples, boolean value) {
        int highValue = getHighValue();
        int lowValue = getLowValue();
        for (long i = 0; i < samples; i++) {
            wavStream.write(value ? highValue : lowValue);
            if (format.getChannelType() == ChannelType.STEREO) {
                wavStream.write(value ? highValue: lowValue);
            } else if (format.getChannelType() == ChannelType.STEREOINV) {
                wavStream.write(value ? lowValue: highValue);
            }
        }
    }

    public void writeValue(long tstates, boolean value) {
        long samples = tStatesToSamples(tstates);
        writeSamples(samples, value);
    }

    public WavOutputStream(OutputStream out, WavFormat format) {
        this.out = out;
        this.format = format;
    }

    public void flush() throws IOException {
        wavStream.flush();
        out.write(getWavHeader(wavStream.size()));
        out.write(wavStream.toByteArray());
        out.flush();
        wavStream.reset();
    }
}
