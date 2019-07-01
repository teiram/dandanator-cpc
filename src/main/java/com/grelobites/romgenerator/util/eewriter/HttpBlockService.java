package com.grelobites.romgenerator.util.eewriter;

import com.grelobites.romgenerator.LoaderConfiguration;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.view.LoaderController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

public class HttpBlockService extends BlockServiceSupport implements BlockService  {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpBlockService.class);
    private static final int DELAY_BETWEEN_REQUESTS = 5000;

    private String url;

    public HttpBlockService(LoaderController controller, LoaderConfiguration configuration) {
        super(controller);
        this.url = configuration.getHttpUrl();
    }

    public void close() {}

    public DataProducer getDataProducer(byte[] data) {
        return new HttpPostDataProducer(url, data);
    }

    private Optional<DataProducer> getBlockDataProducer(int slot) {
        int blockSize = LoaderConfiguration.getInstance().getBlockSize();
        byte[] buffer = new byte[blockSize];
        Optional<byte[]> romsetByteArray = getRomsetByteArray();
        if (romsetByteArray.isPresent()) {
            System.arraycopy(romsetByteArray.get(), slot * blockSize, buffer, 0, blockSize);
            return Optional.of(new HttpPostDataProducer(url, slot, buffer));
        } else {
            return Optional.empty();
        }
    }

    private void handleIncomingData() {
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            InputStream response = connection.getInputStream();
            String data = new String(Util.fromInputStream(response), "UTF-8");
            LOGGER.debug("Got data {}", data);
        } catch (Exception e) {
            LOGGER.info("Error on http request to dandanator", e);
        }
    }

    public void run() {
        try {
            state = State.RUNNING;
            while (state == State.RUNNING) {
                try {
                    handleIncomingData();
                    Thread.sleep(DELAY_BETWEEN_REQUESTS);
                } catch (Exception e) {
                    LOGGER.error("In Http request", e);
                    state = State.STOPPING;
                }
            }
        } finally {
            close();
        }
        LOGGER.debug("Exiting HttpBlockService service thread");
        state = State.STOPPED;
    }

}
