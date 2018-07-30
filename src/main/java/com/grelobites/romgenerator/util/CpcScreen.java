package com.grelobites.romgenerator.util;

import java.io.IOException;

import com.grelobites.romgenerator.Constants;

import com.sun.prism.paint.Gradient;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpcScreen extends WritableImage {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpcScreen.class);

	private static final String DEFAULT_CHARSETPATH = "/charset.rom";

	private static final int[] COLUMNS_PER_MODE = new int[]{20, 40, 80};

	private static final int[] X_FACTORS = new int[]{4, 2, 1};

	private static final int[] Y_FACTORS = new int[]{2, 2, 2};

	private static final CpcColor[] DEFAULT_INKS = new CpcColor[] {
	        CpcColor.BLACK,
            CpcColor.BRIGHTWHITE,
            CpcColor.SEAGREEN,
            CpcColor.BRIGHTBLUE
    };

	private String charSetPath;
	private int mode;
	private byte[] charSet;

    private int lines;
    private int columns;
	private int xfactor;
	private int yfactor;

	private CpcColor[] inks = DEFAULT_INKS;
	private CpcGradient pen = new CpcGradient(CpcColor.BRIGHTYELLOW);
	private CpcColor paper = CpcColor.BLUE;
	
	public CpcScreen(int mode) {
        this(mode, Constants.CPC_SCREEN_WIDTH,
                Constants.CPC_SCREEN_HEIGHT);
	}

	private CpcScreen(int mode, int width, int height) {
        super(width, height);
        this.mode = mode;
        this.lines = 25;
        this.columns = COLUMNS_PER_MODE[mode];
        this.xfactor = X_FACTORS[mode];
        this.yfactor = Y_FACTORS[mode];
    }

	public String getCharSetPath() {
		if (charSetPath == null) {
			charSetPath = DEFAULT_CHARSETPATH;
		}
		return charSetPath;
	}
	
	public byte[] getCharSet() {
		if (charSet == null) {
			try {
				charSet = ImageUtil.streamToByteArray(CpcScreen.class
						.getResourceAsStream(getCharSetPath()));
			} catch (IOException e) {
				throw new IllegalStateException("Unable to load charset", e);
			}
		}
		return charSet;
	}

	public void setCharSetPath(String charSetPath) {
		this.charSetPath = charSetPath;
	}

	public void setCharSet(byte[] charSet) {
		this.charSet = charSet;
	}
	
	public CpcGradient getPen() {
		return pen;
	}

	public void setPen(CpcGradient pen) {
		this.pen = pen;
	}

	public void setPen(CpcColor pen) {
	    this.pen = new CpcGradient(pen);
    }

	public CpcColor getPaper() {
		return paper;
	}

	public void setPaper(CpcColor paper) {
		this.paper = paper;
	}

	public void setInks(CpcColor[] inks) {
	    this.inks = inks;
    }

	private byte charRasterLine(char c, int rasterLine) {
		int index = (c - 32) * 8 + rasterLine;
		return getCharSet()[index];
	}

	private int iconRasterLine(int code, int rasterLine) {
	    int index = (code - 32) * 8 + (rasterLine * 2);
	    return ((getCharSet()[index] & 0xff) << 8) | (getCharSet()[index + 1] & 0xff);
    }
	
	public void deleteChar(int line, int column) {
        if (line < lines && column < columns) {
            int xpos = column * 8;
            int ypos = line * 8;
            PixelWriter writer = getPixelWriter();
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                	printPixelWithFactor(writer, xpos + x, ypos + y, paper.argb());
                }
            }
        } else {
            LOGGER.debug("Out of bounds access to screen");
        }
	}
	
	public void printChar(char c, int line, int column) {
        if (line < lines && column < columns) {
            int xpos = column * 8;
            int ypos = line * 8;
            PixelWriter writer = getPixelWriter();
            for (int y = 0; y < 8; y++) {
                int mask = 0x80;
                byte charRasterLine = charRasterLine(c, y);
                for (int x = 0; x < 8; x++) {
                    int color = (charRasterLine & mask) != 0 ?
                            pen.getColor(y).argb() : paper.argb();
						printPixelWithFactor(writer, xpos + x, ypos + y, color);
                    mask >>= 1;
                }
            }
        } else {
            LOGGER.debug("Out of bounds access to screen");
        }
	}

	private int getColor(int index) {
	    return inks[index].argb();
    }

	public void printIcon(int code, int line, int column) {
        if (line < lines && column < columns) {
            int xpos = column * 8;
            int ypos = line * 8;
            PixelWriter writer = getPixelWriter();
            for (int y = 0; y < 8; y++) {
                int mask1 = 0x8000;
                int mask2 = 0x0800;
                int offset1 = 14;
                int offset2 = 11;
                int iconRasterLine = iconRasterLine(code, y);
                for (int x = 0; x < 8; x++) {
                    int colorIndex = ((iconRasterLine & mask1) >> offset1) |
                            ((iconRasterLine & mask2) >> offset2);
                    printPixelWithFactor(writer, xpos + x, ypos + y,
                            getColor(colorIndex));
                    mask1 >>= 1;
                    mask2 >>= 1;
                    offset1--;
                    offset2--;
                    if (x == 3) {
                        mask1 = 0x80;
                        mask2 = 0x08;
                        offset1 = 6;
                        offset2 = 3;
                    }
                }
            }
        } else {
            LOGGER.debug("Out of bounds access to screen");
        }
    }

    public void printSymbol(int code, int line, int column) {
	    printIcon(code, line, column);
	    printIcon(code + 2, line, column + 1);
	    printIcon(code + 4, line, column + 2);
    }
	private void printPixelWithFactor(PixelWriter writer, int xpos, int ypos, int color) {
		xpos *= xfactor;
		ypos *= yfactor;
		for (int y = 0; y < yfactor; y++) {
			for (int x = 0; x < xfactor; x++) {
				writer.setArgb(xpos + x, ypos + y, color);
			}
		}
	}
	public void printLine(String text, int line, int column) {
		for (int i = 0; i < text.length(); i++) {
			printChar(text.charAt(i), line, column++);
		}
	}
	
	public void deleteLine(int line) {
		for (int column = 0; column < columns; column++) {
			deleteChar(line, column);
		}
	}

    public void clearScreen() {
        for (int line = 0; line < lines; line++) {
            deleteLine(line);
        }
    }

	public int getLines() {
		return lines;
	}

	public int getColumns() {
		return columns;
	}

}
