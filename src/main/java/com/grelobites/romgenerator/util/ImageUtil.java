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
    private static int[] PIXEL_INDEXES_MODE0;
    private static int[] PIXEL_INDEXES_MODE1;

	private static int getXBorderSize(CrtcDisplayData crtcDisplayData) {
	    int width = crtcDisplayData.getVisibleWidth() * 16;
	    return Math.max(0, (Constants.CPC_SCREEN_WIDTH - width) / 2);
    }

    private static int getYBorderSize(CrtcDisplayData crtcDisplayData) {
        int height = crtcDisplayData.getVisibleHeight() * 16;
        return Math.max(0, (Constants.CPC_SCREEN_HEIGHT - height) / 2);
    }

    private static int getHeight(CrtcDisplayData crtcDisplayData) {
	    return Math.min(crtcDisplayData.getVisibleHeight() * 8, 200);
    }

    private static int getWidth(CrtcDisplayData crtcDisplayData) {
	    return Math.min(crtcDisplayData.getVisibleWidth() * 2, 80);
    }

    private static int getPixelIndexesMode0(int pixelData) {
	    if (PIXEL_INDEXES_MODE0 == null) {
	        PIXEL_INDEXES_MODE0 = new int[256];
	        for (int i = 0; i < 256; i++) {
	            PIXEL_INDEXES_MODE0[i] =
                        (((i & 0x02) << 2) |
                                ((i & 0x20) >> 3) |
                                ((i & 0x08) >> 2) |
                                ((i & 0x80) >> 7)) |
                                ((((i & 0x01) << 3) |
                                        ((i & 0x10) >> 2) |
                                        ((i & 0x04) >> 1) |
                                        ((i & 0x40) >> 6)) << 4);
            }
        }
	    return PIXEL_INDEXES_MODE0[pixelData & 0xff];
    }

    private static int getPixelIndexesMode1(int pixelData) {
	    if (PIXEL_INDEXES_MODE1 == null) {
	        PIXEL_INDEXES_MODE1 = new int[256];
	        for (int i = 0; i < 256; i++) {
	            PIXEL_INDEXES_MODE1[i] =
                        (((i & 0x08) >> 2) |
                                ((i & 0x80) >> 7)) |
                                ((((i & 0x04) >> 1) |
                                        ((i & 0x40) >> 6)) << 2) |
                                (((i & 0x02) |
                                        ((i & 0x20) >> 5)) << 4) |
                                ((((i & 0x01) << 1) |
                                        ((i & 0x10) >> 4)) << 6);
            }
        }
	    return PIXEL_INDEXES_MODE1[pixelData & 0xff];
    }

    private static int getPixelColorIndexMode0(int pixelData, int position) {
	    return (getPixelIndexesMode0(pixelData) >>> (position * 4)) & 0x0f;
    }

    private static void writeToImageMode0(PixelWriter writer, byte[] data,
                                          CrtcDisplayData crtcDisplayData,
                                          byte[] palette) {
        int height = getHeight(crtcDisplayData);
        int width = getWidth(crtcDisplayData);
        int xBorderSize = getXBorderSize(crtcDisplayData);
        int yBorderSize = getYBorderSize(crtcDisplayData);
        LOGGER.debug("Rendering image in mode 0 with height={}, width={}, xBorderSize={}, " +
                        "yBorderSize={}",
                height, width, xBorderSize, yBorderSize);

        for (int y = 0; y < height; y++) {
            int lineAddress = (((y / 8) * width) + ((y % 8) * 2048) + crtcDisplayData.getDisplayOffset()) % Constants.SLOT_SIZE;
            for (int x = 0; x < width; x++) {
                int pixelData = data[lineAddress + x];
                int color0 = CpcColor.hardIndexedArgb(palette[getPixelColorIndexMode0(pixelData, 0)]);
                int color1 = CpcColor.hardIndexedArgb(palette[getPixelColorIndexMode0(pixelData, 1)]);
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 4; j++) {
                        writer.setArgb(4 * ((2 * x) + 0) + j + xBorderSize, y * 2 + i + yBorderSize, color0);
                        writer.setArgb(4 * ((2 * x) + 1) + j + xBorderSize, y * 2 + i + yBorderSize, color1);
                    }
                }
            }
        }
    }

    private static int getPixelColorIndexMode1(int pixelData, int position) {
        return (getPixelIndexesMode1(pixelData) >>> (position * 2)) & 0x03;
    }

    private static void writeToImageMode1(PixelWriter writer, byte[] data,
                                          CrtcDisplayData crtcDisplayData, byte[] palette) {
	    int height = getHeight(crtcDisplayData);
	    int width = getWidth(crtcDisplayData);
        int xBorderSize = getXBorderSize(crtcDisplayData);
        int yBorderSize = getYBorderSize(crtcDisplayData);
        LOGGER.debug("Rendering image in mode 1 with height={}, width={}, xBorderSize={}, " +
                        "yBorderSize={}",
                height, width, xBorderSize, yBorderSize);

        for (int y = 0; y < height; y++) {
            int lineAddress = (((y / 8) * width) + ((y % 8) * 2048) + crtcDisplayData.getDisplayOffset()) % Constants.SLOT_SIZE;
            for (int x = 0; x < width; x++) {
                int pixelData = Byte.toUnsignedInt(data[lineAddress + x]);
                int color0 = CpcColor.hardIndexedArgb(palette[getPixelColorIndexMode1(pixelData, 0)]);
                int color1 = CpcColor.hardIndexedArgb(palette[getPixelColorIndexMode1(pixelData, 1)]);
                int color2 = CpcColor.hardIndexedArgb(palette[getPixelColorIndexMode1(pixelData, 2)]);
                int color3 = CpcColor.hardIndexedArgb(palette[getPixelColorIndexMode1(pixelData, 3)]);

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
        int height = getHeight(crtcDisplayData);
        int width = getWidth(crtcDisplayData);
        int xBorderSize = getXBorderSize(crtcDisplayData);
        int yBorderSize = getYBorderSize(crtcDisplayData);
        LOGGER.debug("Rendering image in mode 2 with height={}, width={}, xBorderSize={}, "
                + "yBorderSize={}",
                height, width, xBorderSize, yBorderSize);

        for (int y = 0; y < height; y++) {
			int lineAddress = (((y / 8) * width) + ((y % 8) * 2048) + crtcDisplayData.getDisplayOffset()) % Constants.SLOT_SIZE;
			for (int x = 0; x < width; x++) {
				int pixelData = data[lineAddress + x];

				for (int i = 0; i < 2; i++) {
					writer.setArgb(x * 8 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexedArgb(palette[(pixelData & 0x80) != 0 ? 1 : 0]));
					writer.setArgb(x * 8 + 1 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexedArgb(palette[(pixelData & 0x40) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 2 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexedArgb(palette[(pixelData & 0x20) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 3 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexedArgb(palette[(pixelData & 0x10) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 4 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexedArgb(palette[(pixelData & 0x08) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 5 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexedArgb(palette[(pixelData & 0x04) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 6 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexedArgb(palette[(pixelData & 0x02) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 7 + xBorderSize, y * 2 + i + yBorderSize,
                            CpcColor.hardIndexedArgb(palette[(pixelData & 0x01) != 0 ? 1 : 0]));
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
        fillBackground(image, writer, CpcColor.hardIndexedArgb(palette[16]));
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
