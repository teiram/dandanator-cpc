package com.grelobites.romgenerator.util;

/*
Firmware Number 	Hardware Number 	Colour Name 	R % 	G % 	B % 	Hexadecimal 	RGB values 	Colour
0 	54h 	Black 	0 	0 	0 	#000000 	0/0/0
1 	44h (or 50h) 	Blue 	0 	0 	50 	#000080 	0/0/128
2 	55h 	Bright Blue 	0 	0 	100 	#0000FF 	0/0/255
3 	5Ch 	Red 	50 	0 	0 	#800000 	128/0/0
4 	58h 	Magenta 	50 	0 	50 	#800080 	128/0/128
5 	5Dh 	Mauve 	50 	0 	100 	#8000FF 	128/0/255
6 	4Ch 	Bright Red 	100 	0 	0 	#FF0000 	255/0/0
7 	45h (or 48h) 	Purple 	100 	0 	50 	#FF0080 	255/0/128
8 	4Dh 	Bright Magenta 	100 	0 	100 	#FF00FF 	255/0/255
9 	56h 	Green 	0 	50 	0 	#008000 	0/128/0
10 	46h 	Cyan 	0 	50 	50 	#008080 	0/128/128
11 	57h 	Sky Blue 	0 	50 	100 	#0080FF 	0/128/255
12 	5Eh 	Yellow 	50 	50 	0 	#808000 	128/128/0
13 	40h (or 41h) 	White 	50 	50 	50 	#808080 	128/128/128
14 	5Fh 	Pastel Blue 	50 	50 	100 	#8080FF 	128/128/255
15 	4Eh 	Orange 	100 	50 	0 	#FF8000 	255/128/0
16 	47h 	Pink 	100 	50 	50 	#FF8080 	255/128/128
17 	4Fh 	Pastel Magenta 	100 	50 	100 	#FF80FF 	255/128/255
18 	52h 	Bright Green 	0 	100 	0 	#00FF00 	0/255/0
19 	42h (or 51h) 	Sea Green 	0 	100 	50 	#00FF80 	0/255/128
20 	53h 	Bright Cyan 	0 	100 	100 	#00FFFF 	0/255/255
21 	5Ah 	Lime 	50 	100 	0 	#80FF00 	128/255/0
22 	59h 	Pastel Green 	50 	100 	50 	#80FF80 	128/255/128
23 	5Bh 	Pastel Cyan 	50 	100 	100 	#80FFFF 	128/255/255
24 	4Ah 	Bright Yellow 	100 	100 	0 	#FFFF00 	255/255/0
25 	43h (or 49h) 	Pastel Yellow 	100 	100 	50 	#FFFF80 	255/255/128
26 	4Bh 	Bright White 	100 	100 	100 	#FFFFFF 	255/255/255
 */

public enum CpcColor {
	BLACK(0xFF000000),
	BLUE(0xFF000080),
	BRIGHTBLUE(0xFF0000FF),
	RED(0xFF800000),
	MAGENTA(0xFF800080),
	MAUVE(0xFF8000FF),
	BRIGHTRED(0xFFFF0000),
	PURPLE(0xFFFF0080),
	BRIGHTMAGENTA(0xFFFF00FF),
	GREEN(0xFF008000),
	CYAN(0xFF008080),
	SKYBLUE(0xFF0080FF),
	YELLOW(0xFF808000),
	WHITE(0xFF808080),
	PASTELBLUE(0xFF8080FF),
	ORANGE(0xFFFF8000),
	PINK(0xFFFF8080),
	PASTELMAGENTA(0xFFFF80FF),
	BRIGHTGREEN(0xFF00FF00),
	SEAGREEN(0xFF00FF80),
	BRIGHTCYAN(0xFF00FFFF),
	LIME(0xFF80FF00),
	PASTELGREEN(0xFF80FF80),
	PASTELCYAN(0xFF80FFFF),
	BRIGHTYELLOW(0xFFFFFF00),
	PASTELYELLOW(0xFFFFFF80),
	BRIGHTWHITE(0xFFFFFFFF);
	
	private static CpcColor[] FIRM_INDEXED = {
			BLACK,          BLUE,           BRIGHTBLUE,     RED,
            MAGENTA,        MAUVE,	        BRIGHTRED,      PURPLE,
            BRIGHTMAGENTA,  GREEN,          CYAN,			SKYBLUE,
            YELLOW,         WHITE,          PASTELBLUE,     ORANGE,
			PINK,           PASTELMAGENTA,  BRIGHTGREEN,    SEAGREEN,
			BRIGHTCYAN,     LIME,           PASTELGREEN,    PASTELCYAN,
			BRIGHTYELLOW,   PASTELYELLOW,   BRIGHTWHITE
	};

	private static CpcColor[] HARD_INDEXED = {
            FIRM_INDEXED[13],    FIRM_INDEXED[13],    FIRM_INDEXED[19],    FIRM_INDEXED[25],
            FIRM_INDEXED[1],     FIRM_INDEXED[7],     FIRM_INDEXED[10],    FIRM_INDEXED[16],
            FIRM_INDEXED[7],     FIRM_INDEXED[25],    FIRM_INDEXED[24],    FIRM_INDEXED[26],
            FIRM_INDEXED[6],     FIRM_INDEXED[8],     FIRM_INDEXED[15],    FIRM_INDEXED[17],
            FIRM_INDEXED[1],     FIRM_INDEXED[19],    FIRM_INDEXED[18],    FIRM_INDEXED[20],
            FIRM_INDEXED[0],     FIRM_INDEXED[2],     FIRM_INDEXED[9],     FIRM_INDEXED[11],
            FIRM_INDEXED[4],     FIRM_INDEXED[22],    FIRM_INDEXED[21],    FIRM_INDEXED[23],
            FIRM_INDEXED[3],     FIRM_INDEXED[5],     FIRM_INDEXED[12],    FIRM_INDEXED[14],
    };

	private final int argb;
	
	CpcColor(int argb) {
		this.argb = argb;
	}
	
	public int argb() {
		return argb;
	}
	
	public static int firmIndexed(int paletteIndex) {
		return FIRM_INDEXED[paletteIndex].argb();
	}

	public static int hardIndexed(int paletteIndex) {
	    return HARD_INDEXED[paletteIndex & 0x1f].argb();
    }
}
