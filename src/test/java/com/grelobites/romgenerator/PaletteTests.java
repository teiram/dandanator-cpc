package com.grelobites.romgenerator;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class PaletteTests {

    private Map<Integer, Integer> mappings = new TreeMap<>();

    private void init() {
        mappings.put(0x54, 0);
        mappings.put(0x44, 1); mappings.put(0x50, 1);
        mappings.put(0x55, 2);
        mappings.put(0x5C, 3);
        mappings.put(0x58, 4);
        mappings.put(0x5D, 5);
        mappings.put(0x4C, 6);
        mappings.put(0x45, 7); mappings.put(0x48, 7);
        mappings.put(0x4D, 8);
        mappings.put(0x56, 9);
        mappings.put(0x46, 10);
        mappings.put(0x57, 11);
        mappings.put(0x5E, 12);
        mappings.put(0x40, 13); mappings.put(0x41, 13);
        mappings.put(0x5F, 14);
        mappings.put(0x4E, 15);
        mappings.put(0x47, 16);
        mappings.put(0x4F, 17);
        mappings.put(0x52, 18);
        mappings.put(0x42, 19); mappings.put(0x51, 19);
        mappings.put(0x53, 20);
        mappings.put(0x5A, 21);
        mappings.put(0x59, 22);
        mappings.put(0x5B, 23);
        mappings.put(0x4A, 24);
        mappings.put(0x43, 25); mappings.put(0x49, 25);
        mappings.put(0x4B, 26);
    }

    public PaletteTests() {
        init();
    }

    public static void main(String[] args) {
        PaletteTests tests = new PaletteTests();
        int minValue = Collections.min(tests.mappings.keySet());
        for (int key : tests.mappings.keySet()) {
            System.out.println(String.format("Key %02x=%02x (%d) -> %d", key, key & 0x1f, key - minValue, tests.mappings.get(key)));
            //System.out.println(String.format("COLOR_ARRAY[%d], ", tests.mappings.get(key)));
        }

    }

}
