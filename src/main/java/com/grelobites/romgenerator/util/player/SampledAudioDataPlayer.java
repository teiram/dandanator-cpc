package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.ApplicationContext;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SampledAudioDataPlayer extends AudioDataPlayerSupport implements DataPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampledAudioDataPlayer.class);
    private static final String SERVICE_THREAD_NAME = "AudioPlayerServiceThread";
    private static final int AUDIO_BUFFER_SIZE = 4096;
    private DoubleProperty progressProperty;
    private Runnable onFinalization;
    private File mediaFile;
    private static ExecutorService executor = Executors.newFixedThreadPool(1, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("Audio Player");
        return t;
    });

    private ApplicationContext applicationContext;
    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    private State state = State.STOPPED;

    private void init() {
        progressProperty = new SimpleDoubleProperty();
    }

    public SampledAudioDataPlayer(ApplicationContext applicationContext) throws IOException {
        progressProperty = new SimpleDoubleProperty();
        mediaFile = getRescueLoaderAudioFile();
    }

    private Mixer getMixer() {
        Mixer.Info[] mixerInfos =  AudioSystem.getMixerInfo();
        return AudioSystem.getMixer(mixerInfos[0]);
    }

    private void playAudioFile() {
        try {
            Mixer mixer = getMixer();
            LOGGER.debug("Playing audio on mixer " + mixer.getMixerInfo());
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(mediaFile);
            long length = mediaFile.length();
            int written = 0;
            AudioFormat audioFormat = audioInputStream.getFormat();
            LOGGER.debug("Audio format from media file is " + audioFormat);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine soundLine = (SourceDataLine) mixer.getLine(info);
            soundLine.open(audioFormat);
            soundLine.start();
            int nBytesRead = 0;
            byte[] sampledData = new byte[AUDIO_BUFFER_SIZE];
            while (nBytesRead != -1) {
                nBytesRead = audioInputStream.read(sampledData, 0, sampledData.length);

                if (nBytesRead > 0) {
                    written += soundLine.write(sampledData, 0, nBytesRead);
                }
                final double progress = 1.0 * written / length;
                Platform.runLater(() -> progressProperty.set(progress));
                if (state != State.RUNNING) {
                    LOGGER.debug("No longer in running state");
                    break;
                }
            }
            soundLine.drain();
            soundLine.stop();
            if (state == State.RUNNING && onFinalization != null) {
                //Only when we are not stopped programmatically (end of stream)
                Platform.runLater(onFinalization);
            }
        } catch (Exception e) {
            LOGGER.error("Playing audio", e);
        } finally {
            state = State.STOPPED;
            LOGGER.debug("State is now STOPPED");
        }
    }

    @Override
    public void send() {
        state = State.RUNNING;
        executor.execute(this::playAudioFile);
    }

    @Override
    public void stop() {
        if (state == State.RUNNING) {
            state = State.STOPPING;
            LOGGER.debug("State changed to STOPPING");

            while (state != State.STOPPED) {
                try {
                    executor.awaitTermination(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    LOGGER.debug("Interrupted while waiting player termination", e);
                }
            }
            LOGGER.debug("Stop operation acknowledged");
        }
    }

    @Override
    public void onFinalization(Runnable onFinalization) {
        this.onFinalization = onFinalization;
    }

    @Override
    public DoubleProperty progressProperty() {
        return progressProperty;
    }

}
