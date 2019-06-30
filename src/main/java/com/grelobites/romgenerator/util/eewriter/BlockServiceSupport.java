package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.EepromWriterConfiguration;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.view.EepromWriterController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

public abstract class BlockServiceSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockServiceSupport.class);
    private static final String SERVICE_THREAD_NAME = "SerialBlockService";

    protected Thread serviceThread;
    protected Runnable onDataReceived;
    protected final EepromWriterController controller;
    protected byte[] romsetByteArray;

    protected enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    protected State state = State.STOPPED;

    public BlockServiceSupport(EepromWriterController controller) {
        this.controller = controller;
    }

    public void setOnDataReceived(Runnable onDataReceived) {
        this.onDataReceived = onDataReceived;
    }

    public void resetRomset() {
        this.romsetByteArray = null;
    }

    public abstract void run();

    public void start() {
        this.serviceThread = new Thread(null, this::run, SERVICE_THREAD_NAME);
        this.serviceThread.setDaemon(true);
        this.serviceThread.start();
    }

    public void stop() {
        if (state == State.RUNNING) {
            state = State.STOPPING;
            while (state != State.STOPPED) {
                try {
                    serviceThread.join();
                } catch (InterruptedException e) {
                    LOGGER.debug("Interrupted while waiting for Block Service to stop", e);
                }
            }
        }
    }

    protected Optional<byte[]> getRomsetByteArray() {
        EepromWriterConfiguration configuration = EepromWriterConfiguration.getInstance();
        ApplicationContext applicationContext = controller.getApplicationContext();
        try {
            if (romsetByteArray == null) {
                if (configuration.getCustomRomSetPath() != null) {
                    try (FileInputStream fis = new FileInputStream(configuration.getCustomRomSetPath())) {
                        romsetByteArray = Util.fromInputStream(fis);
                    }
                } else {
                    if (applicationContext.getRomSetHandler().generationAllowedProperty().get()) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        applicationContext.getRomSetHandler().exportRomSet(bos);
                        romsetByteArray = bos.toByteArray();
                    } else {
                        LOGGER.info("Generation of romset byte array currently unavailable");
                    }
                }
            }
            return Optional.ofNullable(romsetByteArray);
        } catch (IOException ioe) {
            LOGGER.error("Trying to get Romset byte array", ioe);
            return Optional.empty();
        }
    }

}
