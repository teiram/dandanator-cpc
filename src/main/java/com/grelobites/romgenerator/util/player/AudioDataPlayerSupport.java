package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.tape.Binary;
import com.grelobites.romgenerator.util.tape.CdtBuilder;
import com.grelobites.romgenerator.util.wav.CdtWavOutputStream;
import com.grelobites.romgenerator.util.wav.WavFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioDataPlayerSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioDataPlayerSupport.class);

    private File temporaryFile;

    private File getTemporaryFile() throws IOException {
        if (temporaryFile == null) {
            temporaryFile = File.createTempFile("romgenerator", ".wav");
        }
        return temporaryFile;
    }

    protected void cleanup() {
        if (temporaryFile != null) {
            if (!temporaryFile.delete()) {
                LOGGER.warn("Unable to delete temporary file " + temporaryFile);
            }
            temporaryFile = null;
        }
    }

    public File getRescueLoaderAudioFile() throws IOException {
        File wavFile = getTemporaryFile();
        FileOutputStream wavOutputStream = new FileOutputStream(wavFile);

        CdtBuilder builder = new CdtBuilder();
        builder.addBinary(Binary.builder()
                .withData(Constants.getRescueLoader())
                .withLoadAddress(Constants.RESCUE_LOADER_ADDRESS)
                .withExecAddress(Constants.RESCUE_LOADER_ADDRESS)
                .withName(Constants.RESCUE_LOADER_NAME)
                .build());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        builder.dump(bos);
        InputStream cdtStream = new ByteArrayInputStream(bos.toByteArray());
        CdtWavOutputStream converter = new CdtWavOutputStream(WavFormat.DEFAULT_FORMAT, cdtStream,
                wavOutputStream);
        converter.flush();

        wavOutputStream.close();
        return wavFile;
    }

}
