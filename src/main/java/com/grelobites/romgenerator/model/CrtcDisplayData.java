package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class CrtcDisplayData {
    private static final int DEFAULT_CRTC_WIDTH = 40;
    private static final int DEFAULT_CRTC_HEIGHT = 25;

    private int visibleWidth;
    private int visibleHeight;
    private int displayOffset;

    public static CrtcDisplayData DEFAULT_VALUE = newBuilder()
            .withDisplayOffset(0)
            .withVisibleWidth(DEFAULT_CRTC_WIDTH)
            .withVisibleHeight(DEFAULT_CRTC_HEIGHT)
            .build();

    public static class Builder {
        private CrtcDisplayData displayData = new CrtcDisplayData();

        public Builder withVisibleWidth(int visibleWidth) {
            displayData.setVisibleWidth(visibleWidth);
            return this;
        }

        public Builder withVisibleHeight(int visibleHeight) {
            displayData.setVisibleHeight(visibleHeight);
            return this;
        }

        public Builder withDisplayOffset(int displayOffset) {
            displayData.setDisplayOffset(displayOffset);
            return this;
        }

        public CrtcDisplayData build() {
            return displayData;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static CrtcDisplayData fromInputStream(InputStream is) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(Util.fromInputStream(is, 4));
        return CrtcDisplayData.newBuilder()
                .withVisibleWidth(Integer.valueOf(buffer.get()).byteValue())
                .withVisibleHeight(Integer.valueOf(buffer.get()).byteValue())
                .withDisplayOffset(Integer.valueOf(buffer.getShort()).shortValue())
                .build();
    }

    public int getVisibleWidth() {
        return visibleWidth;
    }

    public void setVisibleWidth(int visibleWidth) {
        this.visibleWidth = visibleWidth;
    }

    public int getVisibleHeight() {
        return visibleHeight;
    }

    public void setVisibleHeight(int visibleHeight) {
        this.visibleHeight = visibleHeight;
    }

    public int getDisplayOffset() {
        return displayOffset;
    }

    public void setDisplayOffset(int displayOffset) {
        this.displayOffset = displayOffset;
    }

    @Override
    public String toString() {
        return "CrtcDisplayData{" +
                "visibleWidth=" + visibleWidth +
                ", visibleHeight=" + visibleHeight +
                ", displayOffset=" + displayOffset +
                '}';
    }
}
