package com.grelobites.romgenerator.util;

import java.util.Arrays;

public class CpcGradient {
    private static final int GRADIENT_SIZE = 8;

    private CpcColor[] colors = new CpcColor[GRADIENT_SIZE];

    public CpcGradient(CpcColor color) {
        Arrays.fill(colors, color);
    }

    public CpcGradient(CpcColor color1, int length, CpcColor color2) {
        Arrays.fill(colors, 0, length, color1);
        Arrays.fill(colors, length, colors.length, color2);
    }

    CpcColor getColor(int rasterLine) {
        return colors[rasterLine % GRADIENT_SIZE];
    }
}
