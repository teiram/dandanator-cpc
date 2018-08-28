package com.grelobites.romgenerator.util.emulator.peripheral;

public enum KeyboardCode {
    /*
    Bit:
Line 	7 	6 	5 	4 	3 	2 	1 	0
&40 	F Dot 	ENTER 	F3 	F6 	F9 	CURDOWN 	CURRIGHT 	CURUP
&41 	F0 	F2 	F1 	F5 	F8 	F7 	COPY 	CURLEFT
&42 	CONTROL 	\ 	SHIFT 	F4 	] 	RETURN 	[ 	CLR
&43 	. 	/ 	 : 	 ; 	P 	@ 	- 	^
&44 	, 	M 	K 	L 	I 	O 	9 	0
&45 	SPACE 	N 	J 	H 	Y 	U 	7 	8
&46 	V 	B 	F 	G (Joy2 fire) 	T (Joy2 right) 	R (Joy2 left) 	5 (Joy2 down) 	6 (Joy 2 up)
&47 	X 	C 	D 	S 	W 	E 	3 	4
&48 	Z 	CAPSLOCK 	A 	TAB 	Q 	ESC 	2 	1
&49 	DEL 	Joy 1 Fire 3 (CPC only) 	Joy 1 Fire 2 	Joy1 Fire 1 	Joy1 right 	Joy1 left 	Joy1 down 	Joy1 up
     */

    KEY_FDOT    (0, 1 << 7),
    KEY_ENTER   (0, 1 << 6),
    KEY_F3      (0, 1 << 5),
    KEY_F6      (0, 1 << 4),
    KEY_F9      (0, 1 << 3),
    KEY_CURDOWN (0, 1 << 2),
    KEY_CURRIGHT(0, 1 << 1),
    KEY_CURUP   (0, 1),

    KEY_F0      (1, 1 << 7),
    KEY_F2      (1, 1 << 6),
    KEY_F1      (1, 1 << 5),
    KEY_F5      (1, 1 << 4),
    KEY_F8      (1, 1 << 3),
    KEY_F7      (1, 1 << 2),
    KEY_COPY    (1, 1 << 1),
    KEY_CURLEFT (1, 1),
    //&42 	CONTROL 	\ 	SHIFT 	F4 	] 	RETURN 	[ 	CLR

    KEY_CONTROL     (2, 1 << 7),
    KEY_BACKSLASH   (2, 1 << 6),
    KEY_SHIFT       (2, 1 << 5),
    KEY_F4          (2, 1 << 4),
    KEY_CLOSEBRACKET(2, 1 << 3),
    KEY_RETURN      (2, 1 << 2),
    KEY_OPENBRACKET (2, 1 << 1),
    KEY_CLR         (2, 1),

    //&43 	. 	/ 	 : 	 ; 	P 	@ 	- 	^

    KEY_DOT         (3, 1 << 7),
    KEY_SLASH       (3, 1 << 6),
    KEY_COLON       (3, 1 << 5),
    KEY_SEMICOLON   (3, 1 << 4),
    KEY_P           (3, 1 << 3),
    KEY_AT          (3, 1 << 2),
    KEY_DASH        (3, 1 << 1),
    KEY_CIRCUNFLEX  (3, 1),

    //&44 	, 	M 	K 	L 	I 	O 	9 	0

    KEY_COMMA       (4, 1 << 7),
    KEY_M           (4, 1 << 6),
    KEY_K           (4, 1 << 5),
    KEY_L           (4, 1 << 4),
    KEY_I           (4, 1 << 3),
    KEY_O           (4, 1 << 2),
    KEY_9           (4, 1 << 1),
    KEY_0           (4, 1),

    //&45 	SPACE 	N 	J 	H 	Y 	U 	7 	8
    KEY_SPACE       (5, 1 << 7),
    KEY_N           (5, 1 << 6),
    KEY_J           (5, 1 << 5),
    KEY_H           (5, 1 << 4),
    KEY_Y           (5, 1 << 3),
    KEY_U           (5, 1 << 2),
    KEY_7           (5, 1 << 1),
    KEY_8           (5, 1),

    //&46 	V 	B 	F 	G (Joy2 fire) 	T (Joy2 right) 	R (Joy2 left) 	5 (Joy2 down) 	6 (Joy 2 up)

    KEY_V           (6, 1 << 7),
    KEY_B           (6, 1 << 6),
    KEY_F           (6, 1 << 5),
    KEY_G           (6, 1 << 4),
    KEY_T           (6, 1 << 3),
    KEY_R           (6, 1 << 2),
    KEY_5           (6, 1 << 1),
    KEY_6           (6, 1),

    //&47 	X 	C 	D 	S 	W 	E 	3 	4

    KEY_X           (7, 1 << 7),
    KEY_C           (7, 1 << 6),
    KEY_D           (7, 1 << 5),
    KEY_S           (7, 1 << 4),
    KEY_W           (7, 1 << 3),
    KEY_E           (7, 1 << 2),
    KEY_3           (7, 1 << 1),
    KEY_4           (7, 1),

    //&48 	Z 	CAPSLOCK 	A 	TAB 	Q 	ESC 	2 	1
    KEY_Z           (8, 1 << 7),
    KEY_CAPSLOCK    (8, 1 << 6),
    KEY_A           (8, 1 << 5),
    KEY_TAB         (8, 1 << 4),
    KEY_Q           (8, 1 << 3),
    KEY_ESC         (8, 1 << 2),
    KEY_2           (8, 1 << 1),
    KEY_1           (8, 1),

    //&49 	DEL 	Joy 1 Fire 3 (CPC only) 	Joy 1 Fire 2 	Joy1 Fire 1
    // Joy1 right 	Joy1 left 	Joy1 down 	Joy1 up

    KEY_DEL         (9, 1 << 7),
    KEY_JOY1_FIRE3  (9, 1 << 6),
    KEY_JOY1_FIRE2  (9, 1 << 5),
    KEY_JOY1_FIRE1  (9, 1 << 4),
    KEY_JOY1_RIGHT  (9, 1 << 3),
    KEY_JOY1_LEFT   (9, 1 << 2),
    KEY_JOY1_DOWN   (9, 1 << 1),
    KEY_JOY1_UP     (9, 1);







    private int line;
    private int mask;
    KeyboardCode(int line, int mask) {
        this.line = line;
        this.mask = mask;
    }

    public int line() {
        return line;
    }

    public int mask() {
        return mask;
    }


}
