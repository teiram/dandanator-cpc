package com.grelobites.romgenerator.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.grelobites.romgenerator.Constants;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtil.class);

	private static void writeToImageMode0(PixelWriter writer, byte[] data,
									 int offset, byte[] palette) {
		for (int y = 0; y < 200; y++) {
			int lineAddress = (((y / 8) * 80) + ((y % 8) * 2048) + offset) % Constants.SLOT_SIZE;
			for (int x = 0; x < 80; x++) {
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
						writer.setArgb(4 * ((2 * x) + 0) + j, y * 2 + i, color0);
						writer.setArgb(4 * ((2 * x) + 1) + j, y * 2 + i, color1);
					}
				}
			}
		}
	}

	private static void writeToImageMode1(PixelWriter writer, byte[] data,
										  int offset, byte[] palette) {
		for (int y = 0; y < 200; y++) {
			int lineAddress = (((y / 8) * 80) + ((y % 8) * 2048) + offset) % Constants.SLOT_SIZE;
            for (int x = 0; x < 80; x++) {
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
						writer.setArgb(2 * ((4 * x) + 0) + j, y * 2 + i, color0);
						writer.setArgb(2 * ((4 * x) + 1) + j, y * 2 + i, color1);
						writer.setArgb(2 * ((4 * x) + 2) + j, y * 2 + i, color2);
						writer.setArgb(2 * ((4 * x) + 3) + j, y * 2 + i, color3);
					}
				}
			}
		}
	}

    private static void writeToImageMode1(PixelWriter writer, byte[] data,
                                          int offset, int width, int height, byte[] palette) {
	    height = height * 8;
	    width = width * 2;
	    int xBorderSize = (640 - width * 8) / 2;
	    int yBorderSize = (400 - height * 2) / 2;
        for (int y = 0; y < height; y++) {
            int lineAddress = (((y / 8) * width) + ((y % 8) * 2048) + offset) % Constants.SLOT_SIZE;
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
										  int offset, byte[] palette) {
		for (int y = 0; y < 200; y++) {
			int lineAddress = (((y / 8) * 80) + ((y % 8) * 2048) + offset) % Constants.SLOT_SIZE;
			for (int x = 0; x < 80; x++) {
				int pixelData = data[lineAddress + x];

				for (int i = 0; i < 2; i++) {
					writer.setArgb(x * 8, y * 2 + i,
                            CpcColor.hardIndexed(palette[(pixelData & 0x80) != 0 ? 1 : 0]));
					writer.setArgb(x * 8 + 1, y * 2 + i,
                            CpcColor.hardIndexed(palette[(pixelData & 0x40) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 2, y * 2 + i,
                            CpcColor.hardIndexed(palette[(pixelData & 0x20) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 3, y * 2 + i,
                            CpcColor.hardIndexed(palette[(pixelData & 0x10) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 4, y * 2 + i,
                            CpcColor.hardIndexed(palette[(pixelData & 0x08) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 5, y * 2 + i,
                            CpcColor.hardIndexed(palette[(pixelData & 0x04) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 6, y * 2 + i,
                            CpcColor.hardIndexed(palette[(pixelData & 0x02) != 0 ? 1 : 0]));
                    writer.setArgb(x * 8 + 7, y * 2 + i,
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
                0,
                Util.fromInputStream(data, Constants.CPC_PALETTE_SIZE));

    }
	public static <T extends WritableImage> T scrLoader(T image,
													  int screenMode,
													  byte[] slot,
													  int screenOffset,
													  byte[] palette) {
		LOGGER.debug("scrLoader with screenMode {}, screenOffset {}", screenMode, screenOffset);
		PixelWriter writer = image.getPixelWriter();
		switch (screenMode) {
			case 0:
				writeToImageMode0(writer, slot, screenOffset, palette);
				break;
			case 1:
				writeToImageMode1(writer, slot, screenOffset, palette);
				break;
			case 2:
				writeToImageMode2(writer, slot, screenOffset, palette);
				break;
		}
		return image;
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
                                                        int screenOffset,
                                                        int width,
                                                        int height,
                                                        byte[] palette) {
        LOGGER.debug("scrLoader with screenMode {}, screenOffset {}", screenMode, screenOffset);
        PixelWriter writer = image.getPixelWriter();
        fillBackground(image, writer, CpcColor.hardIndexed(palette[16]));
        switch (screenMode) {
            case 0:
                writeToImageMode0(writer, slot, screenOffset, palette);
                break;
            case 1:
                writeToImageMode1(writer, slot, screenOffset, width, height, palette);
                break;
            case 2:
                writeToImageMode2(writer, slot, screenOffset, palette);
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
}
