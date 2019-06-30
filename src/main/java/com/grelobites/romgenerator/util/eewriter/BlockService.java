package com.grelobites.romgenerator.util.eewriter;

public interface BlockService {

    void start();

    void stop();

    void close();

    void setOnDataReceived(Runnable onDataReceived);

    void resetRomset();

    DataProducer getDataProducer(byte[] data);
}
