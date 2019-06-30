package com.grelobites.romgenerator.util.eewriter;

import javafx.beans.property.DoubleProperty;

import java.util.Optional;

public interface DataProducer {

    void send();

    int id();

    void onFinalization(Runnable onFinalization);

    void onDataChunkSent(Runnable onDataChunkSent);
    
    DoubleProperty progressProperty();

}
