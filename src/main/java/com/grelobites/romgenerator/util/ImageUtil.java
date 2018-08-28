package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.CrtcDisplayData;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ImageUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtil.class);

	private static int getXBorderSize(CrtcDisplayData crtcDisplayData) {
	    int width = crtcDisplayData.getVisibleWidth() * 16;
	    return Math.max(0, (Constants.CPC_SCREEN_WIDTH - width) / 2);
    }

    private static int getYBorderSize(CrtcDisplayData crtcDisplayData) {
        int height = crtcDisplayData.getVisibleHeight() * 16;
        return Math.max(0, (Constants.CPC_SCREEN_HEIGHT - height) / 2);
    }

    private static void writeToImageMode0(PixelWriter writer, byte[] data,
                                          CrtcDisplayData crtcDisplayData,
                                          byte[] palette) {
        int height = crtcDisplayData.getVisibleHeight() * 8;
        int width = crtcDisplayData.getVisibleWidth() * 2;
        int xBorderSize = getXBorderSize(crtcDisplayData);
        int yBorderSize = getYBorderSize(crtcDisplayData);
        LOGGER.debug("Rendering image in mode 0 with height={}, width={}, xBorderSize={}, yBorderSize={}",
                height, width, xBorderSize, yBorderSize);

        for (int y = 0; y < height; y++) {
            int lineAddress = (((y / 8) * width) + ((y % 8) * 2048) + crtcDisplayData.getDisplayOffset())
                    % Constants.SLOT_SIZE;
            for (int x = 0; x < width; x++) {
                int pixelData = data[lineAddress + x];
                int color0Index = ((pixelData & 0x02) << 2) |
                        ((pixelData & 0x20) >> 3) |
                        ((pixelData & 0x08) >> 2) |
                        ((pixelData & 0x80) >> 7);
                int color1Index = ((pixelData & 0x01) << 3) |
                        ((pixelData & 0x10) >> 2) |
                        ((pixelData & 0x04) >> 1) |
                        ((pixelData & 0x40) >> 6);
                int color0 = CpcColor.hardIndexed(palette[color0Index]);
                int color1 = CpcColor.hardIndexed(palette[color1Index]);
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        writer.setArgb(4 * ((2 * x) + 0) + j + xBorderSize, y * 2 + i + yBorderSize, color0);
                        writer.setArgb(4 * ((2 * x) + 1) + j + xBorderSize, y * 2 + i + yBorderSize, color1);
                    }
                }
            }
        }
    }

    private static void writeToImageMode1(PixelWriter writer, byte[] data,
                                          CrtcDisplayData crtcDisplayData, byte[] palette) {
	    int height = Math.min(200, crtcDisplayData.getVisibleHeight() * 8);
	    int width = crtcDisplayData.getVisibleWidth() * 2;
        int xBorderSize = getXBorderSize(crtcDisplayData);
        int yBorderSize = getYBorderSize(crtcDisplayData);
        LOGGER.debug("Rendering image in mode 1 with height={}, width={}, xBorderSize={}, yBorderSize={}",
                height, width, xBorderSize, yBorderSize);

        for (int y = 0; y < height; y++) {
            int lineAddress = (((y / 8) * width) + ((y % 8) * 2048) + crtcDisplayData.getDisplayOffset())
                    % Constants.SLOT_SIZE;
            for (int x = 0; x < width; x++) {
                int pixelData = Byte.toUnsignedInt(data[lineAddress + x]);
                int color0Index = ((pixelData & 0x08) >> 2) |
                        ((pixelData & 0x80) >> 7);
                int color1Index = ((pixelData & 0x04) >> 1) |
                        ((pixelData & 0x40) >> 6);
                int color2Index = (pixelData & 0x02) |
                        ((pixelData & 0x20) >> 5);
                int color3Index = ((pixelData & 0x01) << 1) |
                        ((pixelData & 0x10) >> 4);

                int color0 = CpcColor.hardIndexed(palette[color0Index]);
                int color1 = CpcColor.hardIndexed(palette[color1Index]);
                int color2 = CpcColor.hardIndexed(palette[color2Index]);
                int color3 = CpcColor.hardIndexed(palette[color3Index]);

                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        writer.setArgb(2 * ((4 * x) + 0) + j + xBorderSize, y * 2 + i + yBorderSize, color0);
                        writer.setArgb(2 * ((4 * x) + 1) + j + xBorderSize, y * 2 + i + yBorderSize, color1);
                        writer.setArgb(2 * ((4 * x) + 2) + j + xBorderSize, y * 2 + i + yBorderSize, color2);
                        writer.setArgb(2 * ((4 * x) + 3) + j + xBorderSize, y * 2 + i + yBorderSize, color3);
                    }
                }
            }
        }
    }

    private static void writeToImageMode2(PixelWriter writer, byte[] data,
										  CrtcDisplayData crtcDisplayData,
                                          byte[] palette) {
        int height = crtcDisplayData.getVisibleHeight() * 8;
        int width = crtcDisplayData.getVisibleWidth() * 2;
        int xBorderSize = getXBorderSize(crtcDisplayData);
        int yBorderSize = getYBorderSize(crtcDisplayData);

        LOGGER.debug("Rendering image in mode 2 with height={}, width={}, xBorderSize={}, yBorderSize={}",
                height, width, xBorderSize, yBorderSize);

        for (int y = 0; y < height; y++) {
			int lineAddress = (((y / 8) * width) + ((y % 8) * 2048) + crtcDisplayData.getDisplayOffset())
                    % Constants.SLOT_SIZE;
			for (int x = 0; x < width; x++) {
				int pixelData = data[lineAddress + x];

				for (int i = 0; i < 2; i++) {
					writer.setArgb(x * 8 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexed(palette[(pixelData & 0x80) != 0 ? 1 : 0]));
					writer.setArgb(x * 8 + 1 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexed(palette[(pixelData & 0x40) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 2 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexed(palette[(pixelData & 0x20) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 3 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexed(palette[(pixelData & 0x10) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 4 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexed(palette[(pixelData & 0x08) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 5 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexed(palette[(pixelData & 0x04) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 6 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexed(palette[(pixelData & 0x02) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 7 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexed(palette[(pixelData & 0x01) != 0 ? 1 : 0]));
                }
			}
		}
	}

	public static <T extends WritableImage> T scrLoader(T image,
                                                        int screenMode,
                                                        InputStream data)
    throws IOException {
	    return scrLoader(image, screenMode,
                Util.fromInputStream(data, Constants.CPC_SCREEN_SIZE),
                CrtcDisplayData.DEFAULT_VALUE,
                Util.fromInputStream(data, Constants.CPC_PALETTE_SIZE));

    }

	private static void fillBackground(WritableImage image, PixelWriter writer, int color) {
	    for (int y = 0; y < image.getHeight(); y++) {
	        for (int x = 0; x < image.getWidth(); x++) {
	            writer.setArgb(x, y, color);
            }
        }
    }
    public static <T extends WritableImage> T scrLoader(T image,
                                                        int screenMode,
                                                        byte[] slot,
                                                        CrtcDisplayData crtcDisplayData,
                                                        byte[] palette) {
        LOGGER.debug("scrLoader with screenMode {}, crtcDisplayData {}", screenMode, crtcDisplayData);
        PixelWriter writer = image.getPixelWriter();
        fillBackground(image, writer, CpcColor.hardIndexed(palette[16]));
        switch (screenMode) {
            case 0:
                writeToImageMode0(writer, slot, crtcDisplayData, palette);
                break;
            case 1:
                writeToImageMode1(writer, slot, crtcDisplayData, palette);
                break;
            case 2:
                writeToImageMode2(writer, slot, crtcDisplayData, palette);
                break;
        }
        return image;
    }

    public static byte[] streamToByteArray(InputStream stream) throws IOException {
		byte[] buffer = new byte[1024];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			int length;
			while ((length = stream.read(buffer)) != -1) {
				os.write(buffer, 0, length);
			}
			os.flush();
			return os.toByteArray();
		}
	}
	
	public static WritableImage newScreenshot() {
		return new WritableImage(Constants.CPC_SCREEN_WIDTH,
				Constants.CPC_SCREEN_HEIGHT);
	}

	public static boolean isValidScreenFile(File screenFile) {
		return screenFile.isFile() && screenFile.canRead() && screenFile.length() == Constants.CPC_SCREEN_SIZE;
	}

    public static byte[] fillCpcImage(byte[] screen, byte[] palette) {
        byte[] image = new byte[Constants.CPC_SCREEN_SIZE + Constants.CPC_PALETTE_SIZE];
        System.arraycopy(screen, 0, image, 0, screen.length);
        System.arraycopy(palette, 0, image, Constants.CPC_SCREEN_SIZE,
                palette.length);
        return image;
    }

    public static byte[] embeddedPalette(byte[] screen) {
	    if (screen.length == Constants.CPC_SCREEN_SIZE) {
            return Arrays.copyOfRange(screen, 16384 - 17, 16384);
        } else {
	        throw new IllegalArgumentException("Screen of invalid size " + screen.length);
        }
    }
}
