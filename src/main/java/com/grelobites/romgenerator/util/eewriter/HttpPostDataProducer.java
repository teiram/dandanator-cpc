package com.grelobites.romgenerator.util.eewriter;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

public class HttpPostDataProducer extends DataProducerSupport implements DataProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpPostDataProducer.class);
    private static final int SEND_BUFFER_SIZE = 1024;

    private URLConnection connection;

    private void init(String url) {
        try {
            connection = new URL(url).openConnection();
        } catch (Exception e) {
            LOGGER.error("Opening HTTP Connection to {}", url);
            throw new RuntimeException(e);
        }
    }

    public HttpPostDataProducer(String url, int block, byte[] data) {
        super(block, data);
        init(url);
        setupBlockData(block, data);
    }

    public HttpPostDataProducer(String url, byte[] rawData) {
        super(rawData);
        init(url);
    }

    @Override
    public void send() {
        connection.setDoOutput(true); //POST
        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        try (OutputStream output = connection.getOutputStream()) {
            int sentBytesCount = 0;
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            byte[] sendBuffer = new byte[SEND_BUFFER_SIZE];
            while (sentBytesCount < data.length) {
                int count = bis.read(sendBuffer);
                LOGGER.debug("Sending block of " + count + " bytes");
                if (count < SEND_BUFFER_SIZE) {
                    output.write(Arrays.copyOfRange(sendBuffer, 0, count));
                } else {
                    output.write(sendBuffer);
                }
                sentBytesCount += count;
                if (onDataSent != null) {
                    Platform.runLater(onDataSent);
                }
                final double progress = 1.0 * sentBytesCount / data.length;
                Platform.runLater(() -> progressProperty.set(progress));
            }
            int status = ((HttpURLConnection) connection).getResponseCode();
            if (status >= 300) {
                throw new RuntimeException("HTTP Server sent error response");
            }
        } catch (Exception e) {
            LOGGER.error("Exception during send process", e);
            throw new RuntimeException(e);
        } finally {
            Platform.runLater(onFinalization);
        }
    }

}
