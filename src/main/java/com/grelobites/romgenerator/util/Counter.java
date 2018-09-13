package com.grelobites.romgenerator.util;

public class Counter {
    int value;
    final int mask;

    public Counter(int bits) {
        mask = (1 << bits) - 1;
        value = 0;
    }

    public int increment() {
        value = (value + 1) & mask;
        return value;
    }

    public int value() {
        return value;
    }

    public int mask(int mask) {
        value &= mask;
        return value;
    }

    public void reset() {
        value = 0;
    }

}
