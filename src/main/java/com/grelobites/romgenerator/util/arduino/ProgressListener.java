package com.grelobites.romgenerator.util.arduino;

@FunctionalInterface
public interface ProgressListener {
    void onProgressUpdate(double value);
}
