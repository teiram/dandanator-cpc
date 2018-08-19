package com.grelobites.romgenerator.util.emulator;


public class Z80State {
    //8 bit registers
    private int regA, regB, regC, regD, regE, regH, regL;
    // Flags
    private int regF;
    //Last instruction modified the flags
    private boolean flagQ;
    //Alternate registers -- 8 bits
    private int regAx;
    private int regFx;
    private int regBx, regCx, regDx, regEx, regHx, regLx;

    //Program Counter -- 16 bits
    private int regPC;
    //X Index register -- 16 bits
    private int regIX;
    //Y Index register -- 16 bits
    private int regIY;
    //Stack pointer -- 16 bits
    private int regSP;
    //Interrupt vector -- 8 bits
    private int regI;
    //Memory refresh -- 7 bits
    private int regR;
    //Interrupt flip-flops
    private boolean ffIFF1 = false;
    private boolean ffIFF2 = false;
    //EI enables interrupts after next instruction (unless it's also an EI)
    private boolean pendingEI = false;
    //NMI line status
    private boolean activeNMI = false;
    //Interrupt enabled status
    private boolean activeINT = false;
    //Interrupt mode
    private Z80.IntMode modeINT = Z80.IntMode.IM0;
    //True while executing HALT
    private boolean halted = false;

    /**
     * Internal CPU register used as follows:
     *
     * ADD HL,xx      = H value before addition
     * LD r,(IX/IY+d) = Upper byte of IX/IY+d
     * JR d           = Upper byte of destination jump address
     */
    private int memptr;

    public final int getRegA() {
        return regA;
    }

    public final void setRegA(int value) {
        regA = value & 0xff;
    }
    
    public final int getRegF() {
        return regF;
    }

    public final void setRegF(int value) {
        regF = value & 0xff;
    }

    public final int getRegB() {
        return regB;
    }

    public final void setRegB(int value) {
        regB = value & 0xff;
    }

    public final int getRegC() {
        return regC;
    }

    public final void setRegC(int value) {
        regC = value & 0xff;
    }

    public final int getRegD() {
        return regD;
    }

    public final void setRegD(int value) {
        regD = value & 0xff;
    }

    public final int getRegE() {
        return regE;
    }

    public final void setRegE(int value) {
        regE = value & 0xff;
    }

    public final int getRegH() {
        return regH;
    }

    public final void setRegH(int value) {
        regH = value & 0xff;
    }

    public final int getRegL() {
        return regL;
    }

    public final void setRegL(int value) {
        regL = value & 0xff;
    }

    // Acceso a registros alternativos de 8 bits
    public final int getRegAx() {
        return regAx;
    }

    public final void setRegAx(int value) {
        regAx = value & 0xff;
    }
    
    public final int getRegFx() {
        return regFx;
    }

    public final void setRegFx(int value) {
        regFx = value & 0xff;
    }

    public final int getRegBx() {
        return regBx;
    }

    public final void setRegBx(int value) {
        regBx = value & 0xff;
    }

    public final int getRegCx() {
        return regCx;
    }

    public final void setRegCx(int value) {
        regCx = value & 0xff;
    }

    public final int getRegDx() {
        return regDx;
    }

    public final void setRegDx(int value) {
        regDx = value & 0xff;
    }

    public final int getRegEx() {
        return regEx;
    }

    public final void setRegEx(int value) {
        regEx = value & 0xff;
    }

    public final int getRegHx() {
        return regHx;
    }

    public final void setRegHx(int value) {
        regHx = value & 0xff;
    }

    public final int getRegLx() {
        return regLx;
    }

    public final void setRegLx(int value) {
        regLx = value & 0xff;
    }

    // Acceso a registros de 16 bits
    public final int getRegAF() {
        return (regA << 8) | regF;
    }

    public final void setRegAF(int word) {
        regA = (word >>> 8) & 0xff;

        regF = word & 0xff;
    }

    public final int getRegAFx() {
        return (regAx << 8) | regFx;
    }

    public final void setRegAFx(int word) {
        regAx = (word >>> 8) & 0xff;
        regFx = word & 0xff;
    }

    public final int getRegBC() {
        return (regB << 8) | regC;
    }

    public final void setRegBC(int word) {
        regB = (word >>> 8) & 0xff;
        regC = word & 0xff;
    }

    public final int getRegBCx() {
        return (regBx << 8) | regCx;
    }

    public final void setRegBCx(int word) {
        regBx = (word >>> 8) & 0xff;
        regCx = word & 0xff;
    }

    public final int getRegDE() {
        return (regD << 8) | regE;
    }

    public final void setRegDE(int word) {
        regD = (word >>> 8) & 0xff;
        regE = word & 0xff;
    }

    public final int getRegDEx() {
        return (regDx << 8) | regEx;
    }

    public final void setRegDEx(int word) {
        regDx = (word >>> 8) & 0xff;
        regEx = word & 0xff;
    }

    public final int getRegHL() {
        return (regH << 8) | regL;
    }

    public final void setRegHL(int word) {
        regH = (word >>> 8) & 0xff;
        regL = word & 0xff;
    }

    public final int getRegHLx() {
        return (regHx << 8) | regLx;
    }

    public final void setRegHLx(int word) {
        regHx = (word >>> 8) & 0xff;
        regLx = word & 0xff;
    }

    // Acceso a registros de propósito específico
    public final int getRegPC() {
        return regPC;
    }

    public final void setRegPC(int address) {
        regPC = address & 0xffff;
    }

    public final int getRegSP() {
        return regSP;
    }

    public final void setRegSP(int word) {
        regSP = word & 0xffff;
    }

    public final int getRegIX() {
        return regIX;
    }

    public final void setRegIX(int word) {
        regIX = word & 0xffff;
    }

    public final int getRegIY() {
        return regIY;
    }

    public final void setRegIY(int word) {
        regIY = word & 0xffff;
    }

    public final int getRegI() {
        return regI;
    }

    public final void setRegI(int value) {
        regI = value & 0xff;
    }

    public final int getRegR() {
        return regR;
    }

    public final void setRegR(int value) {
        regR = value & 0xff;
    }

    // Acceso al registro oculto MEMPTR
    public final int getMemPtr() {
        return memptr;
    }

    public final void setMemPtr(int word) {
        memptr = word & 0xffff;
    }
    
    // Acceso a los flip-flops de interrupción
    public final boolean isIFF1() {
        return ffIFF1;
    }

    public final void setIFF1(boolean state) {
        ffIFF1 = state;
    }

    public final boolean isIFF2() {
        return ffIFF2;
    }

    public final void setIFF2(boolean state) {
        ffIFF2 = state;
    }

    public final boolean isNMI() {
        return activeNMI;
    }
    
    public final void setNMI(boolean nmi) {
        activeNMI = nmi;
    }

    // La línea de NMI se activa por impulso, no por nivel
    public final void triggerNMI() {
        activeNMI = true;
    }

    // La línea INT se activa por nivel
    public final boolean isINTLine() {
        return activeINT;
    }
    
    public final void setINTLine(boolean intLine) {
        activeINT = intLine;
    }

    public final Z80.IntMode getIM() {
        return modeINT;
    }

    public final void setIM(Z80.IntMode mode) {
        modeINT = mode;
    }

    public final boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean state) {
        halted = state;
    }
    
    public final boolean isPendingEI() {
        return pendingEI;
    }
    
    public final void setPendingEI(boolean state) {
        pendingEI = state;
    }

    public boolean isFlagQ() {
        return flagQ;
    }

    public void setFlagQ(boolean flagQ) {
        this.flagQ = flagQ;
    }

    @Override
    public String toString() {
        return "Z80State{" +
                "AF=0x" + Integer.toHexString(getRegAF()) +
                ", BC=0x" + Integer.toHexString(getRegBC()) +
                ", DE=0x" + Integer.toHexString(getRegDE()) +
                ", HL=0x" + Integer.toHexString(getRegHL()) +
                ", AF'=0x" + Integer.toHexString(getRegAFx()) +
                ", BC'=0x" + Integer.toHexString(getRegBCx()) +
                ", DE'=0x" + Integer.toHexString(getRegDEx()) +
                ", HL'=0x" + Integer.toHexString(getRegHLx()) +
                ", PC=0x" + Integer.toHexString(regPC) +
                ", IX=0x" + Integer.toHexString(regIX) +
                ", IY=0x" + Integer.toHexString(regIY) +
                ", SP=0x" + Integer.toHexString(regSP) +
                ", I=0x" + Integer.toHexString(regI) +
                ", R=0x" + Integer.toHexString(regR) +
                ", memptr=0x" + memptr +
                ", flagQ=" + flagQ +
                ", IFF1=" + ffIFF1 +
                ", IFF2=" + ffIFF2 +
                ", pendingEI=" + pendingEI +
                ", activeNMI=" + activeNMI +
                ", activeINT=" + activeINT +
                ", intMode=" + modeINT +
                ", halted=" + halted +
                '}';
    }
}
