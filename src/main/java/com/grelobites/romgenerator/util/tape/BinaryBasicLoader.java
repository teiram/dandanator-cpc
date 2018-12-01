package com.grelobites.romgenerator.util.tape;

import java.io.PrintStream;

public class BinaryBasicLoader {
    private static final int DATAS_PER_LINE = 10;
    private static final int LINE_STEP = 10;
    private Binary binary;

    private String arrayElements(byte[] data, int start, int length) {
        StringBuilder result = new StringBuilder();
        for (int i = start; i < start + length; i++) {
            if (i >= data.length) {
                break;
            }
            result.append(String.format(i == start ? "%02X" : ",%02X", data[i]));
        }
        return result.toString();
    }

    public BinaryBasicLoader(Binary binary) {
        this.binary = binary;
    }

    public void dump(PrintStream stream) {
        int currentLine = LINE_STEP;
        stream.println(String.format("%d FOR N=&%04X TO &%04X: READ A$", currentLine,
                binary.getLoadAddress(),
                binary.getLoadAddress() + binary.getData().length - 1));
        currentLine += LINE_STEP;
        stream.println(String.format("%d POKE N, VAL(\"&\"+A$):NEXT", currentLine));
        currentLine += LINE_STEP;
        stream.println(String.format("%d CALL &%04X",
                currentLine,
                binary.getExecAddress()));
        byte data[] = binary.getData();
        currentLine += LINE_STEP;
        for (int i = 0; i < (data.length + DATAS_PER_LINE - 1) / DATAS_PER_LINE; i++) {
            stream.println(String.format(
                    "%d DATA %s", currentLine,
                    arrayElements(data, i * DATAS_PER_LINE, DATAS_PER_LINE)));
            currentLine += LINE_STEP;
        }
    }
}
