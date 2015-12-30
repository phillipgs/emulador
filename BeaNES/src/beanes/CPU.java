/*
*  Copyright (C) 2008 Don Honerbrink, Chris Frericks
*
*  This file is part of BeaNES.
*
*  BeaNES is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*   BeaNES is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with BeaNES.  If not, see <http://www.gnu.org/licenses/>.
*/

package beanes;

import java.util.*;


public class CPU {
    
    private BeaNES nes;
    
    public static int CPU_MEMORY_SIZE = 0x10000;
    public static double FREQUENCY = 1789772.5d;
    
    public short[] cpuMemory;
    
    private short[] opcodeCycles;
    private short[] opcodeSizes;
    private short[] opcodeModes;
    private String[] opcodeNames;
    private String[] addressModeNames;
    
    private int numCyclesRan;
    private int numInstructionsRan;
    private int debugCounter;
    private int debugInitialInput;
    private int[] joypadsStrobeCount = new int[2];
    private int[] joypadLastWrite = new int[2];
    
    private int irqRequestType;
    private boolean irqRequested;
    
    public int regACC;
    public int regX;
    public int regY;
    public int regPC;
    public int regSP;
    public int flagCarry;
    public int flagZero;
    public int flagInterrupt;
    public int flagDecimal;
    public int flagBreak;
    public int flagNotUsed;
    public int flagOverflow;
    public int flagSign;
    
    public static final int IRQ_NORMAL = 0;
    public static final int IRQ_NMI = 1;
    public static final int IRQ_RESET = 2;
    
    private static final int ADDR_ZP = 0x00;
    private static final int ADDR_ZP_X = 0x01;
    private static final int ADDR_ZP_Y = 0x02;
    private static final int ADDR_ABS = 0x03;
    private static final int ADDR_ABS_X = 0x04;
    private static final int ADDR_ABS_Y = 0x05;
    private static final int ADDR_IMPLIED = 0x06;
    private static final int ADDR_ACC = 0x07;
    private static final int ADDR_IMMEDIATE = 0x08;
    private static final int ADDR_INDIR = 0x09;
    private static final int ADDR_INDIR_X = 0x0A;
    private static final int ADDR_INDIR_Y = 0x0B;
    private static final int ADDR_RELATIVE = 0x0C;
    
    public CPU(BeaNES nes) {
        this.nes = nes;
        initOpcodes();
        
        cpuMemory = new short[CPU_MEMORY_SIZE];
        hardReset();
    }
    
    
    public void reset() {
        regACC = 0;
        regX = 0;
        regY = 0;
        regPC = 0xC000;
        regSP = 0x01FF;
        flagCarry = 0;
        flagZero = 0;
        flagInterrupt = 1;
        flagDecimal = 0;
        flagBreak = 0;
        flagNotUsed = 1;
        flagOverflow = 0;
        flagSign = 0;
        
        requestIRQ(IRQ_RESET);
    }
   
    
    public void hardReset() {
        numCyclesRan = 0;
        numInstructionsRan = 0;
        debugCounter = 0;
        debugInitialInput = 0;
        
        joypadsStrobeCount[0] = 0;
        joypadsStrobeCount[1] = 0;
        joypadLastWrite[0] = -1;
        joypadLastWrite[1] = -1;
        
        irqRequestType = 0;
        irqRequested = false;
        
        reset();
    }
    
    
    public int getFlags() {
        int flags = 0;
        flags = flagSign;
        flags = (flags << 1)| flagOverflow;
        flags = (flags << 1)| flagNotUsed;
        flags = (flags << 1)| flagDecimal;
        flags = (flags << 1)| flagInterrupt;
        flags = (flags << 1)| flagZero;
        flags = (flags << 1)| flagCarry;
        return flags;
    }
    
    
    public void processIRQ() {
        if (irqRequested) {
               /* first check if interupt occured and if so,
                * 1. push program counter and status register on to the stack
                * 2. set interrupt disable flag to prevent further interrupts
                * 3. Load address of the interrupt handling routine from vector table
                *    into the program couner
                * 4. Execute interrupt handling routine
                * 5. After executing RTI (Return from interrupt instruction),
                *    pull program counter and status register values from stack
                * 6. Resume execution of program
                */
            int temp = 0;
            temp = flagSign | (temp << 1);
            temp = flagOverflow | (temp << 1);
            temp = flagNotUsed | (temp << 1);
            temp = flagBreak | (temp << 1);
            temp = flagDecimal | (temp << 1);
            temp = flagInterrupt | (temp << 1);
            temp = flagZero | (temp << 1);
            temp = flagCarry | (temp << 1);
            
            switch (irqRequestType) {
                case IRQ_NORMAL:
                    //System.out.println("Normal interrupt");
                    // do not run normal interrupt if disabled
                    if (flagInterrupt != 0)
                        break;
                    
                    push((short) ((regPC >> 8) & 0xFF));
                    push((short) (regPC & 0xFF));
                    push((short) temp);
                    
                    flagInterrupt = 1;
                    flagBreak = 0;
                    regPC = read(0xFFFE) | ((read(0xFFFF) << 8)&0xFF00);
                    break;
                    
                case IRQ_NMI:
                    //System.out.println("NMI interrupt");
                    // read PPU status & check if VBlank interrupts are enabled
                    push((short) ((regPC >> 8) & 0xFF));
                    push((short) (regPC & 0xFF));
                    push((short) temp);
                    regPC = read(0xFFFA) | ((read(0xFFFB) << 8)&0xFF00);
                    
                    //}
                    break;
                    
                    
                case IRQ_RESET:
                    //System.out.println("Reset interrupt");
                    regPC = read(0xFFFC) | ((read(0xFFFD) << 8)&0xFF00);
                    
                    break;
            }
            
            irqRequested = false;
        }
    }
    
    
    private int processAddressingMode(int addressingMode) {
        int address = 0;
        // get address location based off of addressing mode
        switch (addressingMode) {
            case ADDR_ZP:
                // Zero page
                address = read(regPC)&0xFF;
                regPC++;
                break;
                
            case ADDR_ZP_X:
                // Zero page,X
                address = (read(regPC) + regX)&0xFF;
                regPC++;
                break;
                
            case ADDR_ZP_Y:
                // Zero page,Y
                address = (read(regPC) + regY)&0xFF;
                regPC++;
                break;
                
            case ADDR_ABS:
                // Absolute
                address = read(regPC) | ((read(regPC+1) << 8)&0xFF00);
                regPC++;
                regPC++;
                break;
                
            case ADDR_ABS_X:
                // Absolute,X
                address = ((read(regPC) | ((read(regPC+1) << 8)&0xFF00)) + regX)&0xFFFF;
                regPC++;
                regPC++;
                break;
                
            case ADDR_ABS_Y:
                // Absolute,Y
                address = ((read(regPC) | ((read(regPC+1) << 8)&0xFF00)) + regY)&0xFFFF;
                regPC++;
                regPC++;
                break;
                
            case ADDR_IMPLIED:
                // Implied
                // nothing to do here
                break;
                
            case ADDR_ACC:
                // Accumulator
                address = regACC;
                break;
                
            case ADDR_IMMEDIATE:
                // Immediate
                address = regPC;
                regPC++;
                break;
                
            case ADDR_INDIR:
                // Indirect
                address = read(regPC) | ((read(regPC+1) << 8)&0xFF00);
                address = read(address) | ((read(address+1) << 8)&0xFF00);
                
                regPC++;
                regPC++;
                break;
                
            case ADDR_INDIR_X:
                // Indirect,X (pre-indexed)
                address = (regX + read(regPC))&0xFF;
                regPC++;
                regPC &= 0xFFFF;
                address = (read(address) | ((read(address+1) << 8)&0xFF00))&0xFFFF;
                break;
                
            case ADDR_INDIR_Y:
                // Indirect,Y (post-indexed)
                address = read(regPC);
                regPC++;
                regPC &= 0xFFFF;
                address = read(address) | ((read(address+1) << 8)&0xFF00);
                address += regY;
                break;
                
            case ADDR_RELATIVE:
                // Relative
                    /*
                     *can jump (-128 to 127) so msb is represents sign
                     * so if relative address to jump is less than 0x80 (128)
                     * jump forward, otherwise jump backwards 0xFF (256) to go
                     * backwards in memory
                     */
                address = read(regPC);
                regPC++;
                regPC &= 0xFFFF;
                
                if(address<0x80){
                    address = regPC + address;
                }else{
                    address = regPC + (address - 0x100);
                }
                break;
        }
        
        address &= 0xFFFF;
        regPC &= 0xFFFF;
        return address;
    }
    
    
    public int processNextInstruction() {
        
        int opcode;
        int addressingMode;
        int cycles;
        int cyclesToWait = 0;
        int size;
        int address = 0;
        int temp = 0;
        regPC &= 0xFFFF;
        
        processIRQ();
        
        
        //read next instruction from memory map
        opcode = read(regPC);
        
        addressingMode = opcodeModes[opcode];
        cycles = opcodeCycles[opcode];
        size = opcodeSizes[opcode];
        
        regPC++;
        regPC &= 0xFFFF;
        
        address = processAddressingMode(addressingMode);
        
        
        
        switch (opcode) {
            
            case 0x69:
            case 0x65:
            case 0x75:
            case 0x6D:
            case 0x7D:
            case 0x79:
            case 0x61:
            case 0x71:
                    /* ADC
                     * Adds value in accumulator A with the value in the address specified. If there is a carry, the flag should be set.
                     */
                temp = read(address) + regACC + flagCarry;
                // overflow if you add two positive numbers (or subtract two negative numbers) and the result changes the MSB, overflow
                // (cannot overflow if you add a positive number and a negative number together)
                flagOverflow = ((!(((regACC ^ read(address)) & 0x80) != 0) && (((regACC ^ temp) & 0x80)) != 0) ? 1 : 0);
                flagCarry = (temp > 0xFF) ? 1 : 0;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regACC = temp & 0xFF;
                break;
                
            case 0x29:
            case 0x25:
            case 0x35:
            case 0x2D:
            case 0x3D:
            case 0x39:
            case 0x21:
            case 0x31:
                    /* AND
                     * ANDs the value in the accumulator with the value in the address specified.
                     */
                temp = read(address) & regACC;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regACC = temp & 0xFF;
                break;
                
            case 0x0A:
            case 0x06:
            case 0x16:
            case 0x0E:
            case 0x1E:
                    /* ASL
                     * Arithmatically shifts the bits in the accumulator to the left.
                     */
                
                temp = (addressingMode == ADDR_ACC) ? regACC : read(address);
                flagCarry = ((temp & 0x80) >> 7) & 1; //Loads the MSB into the carry flag
                temp = (temp << 1) & 0xFE;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                if (addressingMode == ADDR_ACC) {
                    regACC = temp;
                } else {
                    write(address, (short) temp);
                }
                break;
                
            case 0x24:
            case 0x2C:
                    /* BIT
                     * This is a bit test. It is the AND operation, but the contents are not stored in the accumulator.
                     */
                
                temp = read(address);
                flagSign = (temp >> 7) & 1;
                flagOverflow = (temp >> 6) & 1;
                temp &= regACC;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                
                break;
                
            case 0x30:
                    /* BMI
                     *Branches only if the negative flag is set to 1.
                     */
                if (flagSign != 0) {
                    regPC = address;
                }
                
                break;
            case 0x10:
                    /* BPL
                     * Branches only if the negative flag is a 0.
                     */
                if (flagSign == 0) {
                    regPC = address;
                }
                break;
                
                
                
            case 0x50:
                    /* BVC
                     *Branches only if the overflow flag is set to 0.
                     */
                if (flagOverflow == 0) {
                    regPC = address;
                }
                break;
                
            case 0x70:
                    /* BVS
                     *Branches only if the negative flag is set to 1.
                     */
                if (flagOverflow != 0) {
                    regPC = address;
                }
                break;
                
            case 0x90:
                    /* BCC
                     *Branches only if the carry flag is set to 0.
                     */
                if (flagCarry == 0) {
                    regPC = address;
                }
                break;
                
            case 0xB0:
                    /* BCS
                     *Branches only if the carry flag is set to 1.
                     */
                if (flagCarry != 0) {
                    regPC = address;
                }
                break;
                
            case 0xD0:
                    /* BNE
                     * Branch on not zero.
                     */
                if (flagZero == 0) {
                    regPC = address;
                }
                break;
                
            case 0xF0:
                    /* BEQ
                     * Branch on equal.
                     */
                if (flagZero != 0) {
                    regPC = address;
                }
                
                break;
                
            case 0x00:
                    /* BRK
                     * BRK causes a non-maskable interrupt and increments the program counter by one.
                     * Therefore an RTI will go to the address of the BRK +2
                     * so that BRK may be used to replace a two-short instruction for debugging and the subsequent RTI will be correct.
                     */
                regPC++;
                push((short)((regPC>>8)&0xFF));
                push((short)(regPC&0xFF));
                flagBreak = 1;
                
                temp = 0;
                temp = flagSign | (temp << 1);
                temp = flagOverflow | (temp << 1);
                temp = flagNotUsed | (temp << 1);
                temp = flagBreak | (temp << 1);
                temp = flagDecimal | (temp << 1);
                temp = flagInterrupt | (temp << 1);
                temp = flagZero | (temp << 1);
                temp = flagCarry | (temp << 1);
                
                push((short) temp);
                
                flagInterrupt = 1;
                regPC = read(0xFFFE) | ((read(0xFFFF) << 8)&0xFF00);
                
                //System.out.println("BRK regPC " + Integer.toHexString(regPC));
                break;
                
            case 0xC9:
            case 0xC5:
            case 0xD5:
            case 0xCD:
            case 0xDD:
            case 0xD9:
            case 0xC1:
            case 0xD1:
                    /* CMP
                     * This subtracts the value of the accumulator and the value in the address specified.
                     * It does NOT save the result into A, but it will set or clear flags.
                     */
                temp = regACC - read(address);
                flagCarry = (temp >= 0)? 1 : 0;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                break;
                
            case 0xE0:
            case 0xE4:
            case 0xEC:
                    /* CPX
                     * This subtracts the value of the X register and the value in the address specified.
                     * It does NOT save the result into X, but it will set or clear flags.
                     */
                temp = regX - read(address);
                flagCarry = (temp >= 0)? 1 : 0;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                
                
                break;
                
            case 0xC0:
            case 0xC4:
            case 0xCC:
                    /* CPY
                     * This subtracts the value of the Y register and the value in the address specified.
                     * It does NOT save the result into Y, but it will set or clear flags.
                     */
                temp = regY - read(address);
                flagCarry = (temp >= 0)? 1 : 0;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                
                
                break;
                
            case 0xC6:
            case 0xD6:
            case 0xCE:
            case 0xDE:
                    /* DEC
                     * Subtracts the value in memory by 1
                     */
                temp = (read(address) - 1) & 0xFF;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                write(address, (short) temp);
                
                
                break;
                
            case 0x49:
            case 0x45:
            case 0x55:
            case 0x4D:
            case 0x5D:
            case 0x59:
            case 0x41:
            case 0x51:
                    /* EOR
                     * Performs the XOR operation on A and the value in the specified address.
                     * The result is stored into A.
                     */
                temp = (read(address) ^ regACC) & 0xFF;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regACC = temp;
                
                break;
                
            case 0x18:
                    /* CLC
                     * Clears carry flag to 0
                     */
                flagCarry = 0;
                
                
                break;
                
            case 0x38:
                    /* SEC
                     * Sets carry flag to 1
                     */
                flagCarry = 1;
                
                
                break;
                
            case 0x58:
                    /* CLI
                     * Clears interrupt flag to 0
                     */
                flagInterrupt = 0;
                
                
                break;
                
            case 0x78:
                    /* SEI
                     * Sets interrupt flag to 1
                     */
                flagInterrupt = 1;
                
                
                break;
                
            case 0xB8:
                    /* CLV
                     * Clears overflow flag to 0
                     */
                flagOverflow = 0;
                
                break;
                
            case 0xD8:
                    /* CLD
                     * Clears decimal flag to 0
                     */
                flagDecimal = 0;
                
                
                break;
                
            case 0xF8:
                    /* SED
                     * Sets decimal flag to 1
                     */
                flagDecimal = 1;
                
                
                break;
                
                
            case 0xE6:
            case 0xF6:
            case 0xEE:
            case 0xFE:
                    /* INC
                     * Increments the value in accumulator A by 1
                     */
                
                temp = (read(address) + 1) & 0xFF;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                write(address, (short) temp);
                
                break;
                
            case 0x4C:
            case 0x6C:
                    /* JMP
                     * The program jumps to the location specified. This function changes the
                     * Program Counter to where the jump address specifies.
                     */
                regPC = address&0xFFFF;
                
                break;
                
            case 0x20:
                    /* JSR
                     * The program jumps to the location specified. This function changes the
                     * Program Counter to where the jump address specifies.
                     */
                push((short) (((regPC-1) >> 8) & 0xFF)); // push msb of address
                push((short) ((regPC-1) & 0xFF));      // push lsb of address
                
                regPC = address;
                break;
                
            case 0xA9:
            case 0xA5:
            case 0xB5:
            case 0xAD:
            case 0xBD:
            case 0xB9:
            case 0xA1:
            case 0xB1:
                    /* LDA
                     * Loads accumulator A with the value found in the given address.
                     */
                temp = read(address);
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regACC = temp;
                
                break;
                
            case 0xA2:
            case 0xA6:
            case 0xB6:
            case 0xAE:
            case 0xBE:
                    /* LDX
                     * Loads register X with the value found in the given address.
                     */
                temp = read(address);
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regX = temp;
                
                break;
                
            case 0xA0:
            case 0xA4:
            case 0xB4:
            case 0xAC:
            case 0xBC:
                    /* LDY
                     * Loads register Y with the value foudn in the given address.
                     */
                temp = read(address);
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regY = temp;
                
                break;
                
            case 0x4A:
            case 0x46:
            case 0x56:
            case 0x4E:
            case 0x5E:
                    /* LSR
                     * Shifts the contents of A to the right once.
                     */
                temp = (addressingMode == ADDR_ACC) ? regACC : read(address);
                
                flagCarry = (temp & 1); //Loads the LSB into the carry flag
                temp = (temp >> 1) & 0x7F;  // shift right
                flagSign = 0;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                
                if (addressingMode == ADDR_ACC) {
                    regACC = temp;
                } else {
                    write(address, (short) temp);
                }
                
                break;
                
            case 0xEA:
                    /* NOP
                     * No Operation, just uses a CPU cycle.
                     */
                
                
                break;
                
            case 0x09:
            case 0x15:
            case 0x0D:
            case 0x1D:
            case 0x19:
            case 0x11:
            case 0x01:
            case 0x05:
                    /* ORA
                     *Logical Inclusive OR. Bitwise OR with A and a source. The result will be
                     *false only if both bits are 0; otherwise it will be 1.
                     */
                temp = (regACC | read(address)) & 0xFF;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regACC = temp;
                
                
                break;
                
            case 0xAA:
                    /* TAX
                     * Transfers the contents from A to X
                     */
                regX = regACC;
                flagSign = (regACC >> 7) & 1;
                flagZero = ((regACC & 0xFF) == 0) ? 1 : 0;
                
                break;
                
            case 0x8A:
                    /* TXA
                     * Transfers the contents from X to A
                     */
                regACC = regX;
                flagSign = (regACC >> 7) & 1;
                flagZero = ((regACC & 0xFF) == 0) ? 1 : 0;
                
                
                break;
                
            case 0xCA:
                    /* DEX
                     * Decrements the value in register X by 1
                     */
                temp = (regX - 1) & 0xFF;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regX = temp;
                
                break;
                
                
            case 0xE8:
                    /* INX
                     * increments the value in register X by 1
                     */
                temp = (regX + 1) & 0xFF;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regX = temp;
                
                break;
                
            case 0xA8:
                    /* TAY
                     * Transfers the contents from A to Y
                     */
                regY = regACC;
                flagSign = (regACC >> 7) & 1;
                flagZero = ((regACC & 0xFF) == 0) ? 1 : 0;
                
                break;
                
            case 0x98:
                    /* TYA
                     * Transfers the contents from Y to A
                     */
                regACC = regY;
                flagSign = (regACC >> 7) & 1;
                flagZero = ((regACC & 0xFF) == 0) ? 1 : 0;
                
                break;
                
            case 0x88:
                    /* DEY
                     * Decrements the value in register Y by 1
                     */
                temp = (regY - 1) & 0xFF;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regY = temp;
                
                break;
                
            case 0xC8:
                    /* INY
                     * Increments the value in register Y by 1
                     */
                temp = (regY + 1) & 0xFF;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regY = temp;
                
                break;
                
            case 0x2A:
            case 0x26:
            case 0x36:
            case 0x2E:
            case 0x3E:
                    /* ROL
                     * Shifts the contents in accumulator A to the left. The rightmost bit (LSB) is filled
                     * with the value of the carry flag and the leftmost bit (MSB) is sent to the carry flag bit
                     */
                
                temp = (addressingMode == ADDR_ACC) ? regACC : read(address);
                
                int ncarry = (temp >> 7) & 1;
                temp = ((temp << 1) & 0xFE) | flagCarry;
                flagCarry = ncarry;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                
                if (addressingMode == ADDR_ACC) {
                    regACC = temp;
                } else {
                    write(address, (short) temp);
                }
                
                break;
                
            case 0x6A:
            case 0x66:
            case 0x76:
            case 0x6E:
            case 0x7E:
                    /* ROR
                     * Shifts the contents in accumulator A to the right. The leftmost bit (LSB) is filled
                     * with the value of the carry flag and the rightmost bit (MSB) is sent to the carry flag bit
                     */
                /*
                temp = (regACC) & 1;
                regACC = ((regACC >> 1) & 0x7F) | (flagCarry << 7);
                flagCarry = temp;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                 */
                temp = (addressingMode == ADDR_ACC) ? regACC : read(address);
                int oldTemp = temp;
                int lcarry = temp & 1;
                temp = ((temp >> 1) & 0x7F) | ((flagCarry<<7)&0xFF);
                flagCarry = lcarry;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                
                if (addressingMode == ADDR_ACC) {
                    regACC = temp;
                } else {
                    write(address, (short) temp);
                }
                
                
                break;
                
            case 0x40:
                    /* RTI
                     * After the interrupt code is executed, this
                     * returns to where the program was left restoring the PC and the
                     * flags that were pushed on the stack when the interrupt began.
                     */
                
                temp = pull();
                flagCarry = temp & 1;
                flagZero = (temp >> 1) & 1;
                flagInterrupt = (temp >> 2) & 1;
                flagDecimal = (temp >> 3) & 1;
                flagBreak = (temp >> 4) & 1;
                flagNotUsed = (temp >> 5) & 1;
                flagOverflow = (temp >> 6) & 1;
                flagSign = (temp >> 7) & 1;
                
                regPC = (pull()&0xFF) | ((pull() << 8)&0xFF00);
                break;
                
            case 0x60:
                    /* RTS
                     * This returns from a subroutine to the next
                     * instruction to where it was called, pulling PC from the stack.
                     */
                regPC = ((pull())&0xFF) | ((pull() << 8)&0xFF00);
                regPC++;
                break;
                
            case 0xE9:
            case 0xE5:
            case 0xF5:
            case 0xED:
            case 0xFD:
            case 0xF9:
            case 0xE1:
            case 0xF1:
                    /* SBC
                     * Subtracts the value of the accumulator and the stored value in the given address.
                     * This operation can change the value in the carry flag.
                     */
                temp = regACC - read(address) - (1 - flagCarry);
                // overflow if you add two positive numbers (or subtract two negative numbers) and the result changes the MSB, overflow
                // (cannot overflow if you add a positive number and a negative number together)
                flagOverflow = (((((regACC ^ read(address)) & 0x80) != 0) && (((regACC ^ temp) & 0x80)) != 0) ? 1 : 0);
                flagCarry = (temp < 0) ? 0 : 1;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regACC = temp & 0xFF;
                break;
                
            case 0x85:
            case 0x95:
            case 0x8D:
            case 0x9D:
            case 0x99:
            case 0x81:
            case 0x91:
                    /* STA
                     * Stores the value of the accumulator in the specified address.
                     */
                write(address, (short) regACC);
                
                break;
                
            case 0x9A:
                    /* TXS
                     * Transfer register X to the stack pointer
                     */
                regSP = regX+0x0100;
                
                break;
                
            case 0xBA:
                    /* TSX
                     * Transfers the contents of where the stack pointer points into register X.
                     */
                temp = regSP-0x0100;
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regX = temp;
                break;
                
            case 0x48:
                    /* PHA
                     * Pushes the contents of the accumulator onto the stack.
                     */
                push((short) regACC);
                
                break;
                
            case 0x68:
                    /* PLA
                     * Pulls the contents of the stack pointer in the accumulator.
                     */
                temp = pull();
                flagSign = (temp >> 7) & 1;
                flagZero = ((temp & 0xFF) == 0) ? 1 : 0;
                regACC = temp;
                
                break;
                
                
            case 0x08:
                    /* PHP
                     * Pushes the contents of the flag register onto the stack.
                     */
                flagBreak = 1;
                temp = 0;
                temp = flagSign | (temp << 1);
                temp = flagOverflow | (temp << 1);
                temp = flagNotUsed | (temp << 1);
                temp = flagBreak | (temp << 1);
                temp = flagDecimal | (temp << 1);
                temp = flagInterrupt | (temp << 1);
                temp = flagZero | (temp << 1);
                temp = flagCarry | (temp << 1);
                
                push((short) temp);
                break;
                
                
            case 0x28:
                    /* PLP
                     * Pulls the contents of the flag register from the stack.
                     */
                temp = pull();
                flagCarry = temp & 1;
                flagZero = (temp >> 1) & 1;
                flagInterrupt = (temp >> 2) & 1;
                flagDecimal = (temp >> 3) & 1;
                flagBreak = (temp >> 4) & 1;
                flagNotUsed = (temp >> 5) & 1;
                flagOverflow = (temp >> 6) & 1;
                flagSign = (temp >> 7) & 1;
                break;
                
            case 0x86:
            case 0x96:
            case 0x8E:
                    /* STX
                     * Stores the value of register X into specified address.
                     */
                write(address, (short) regX);
                
                break;
                
                
            case 0x84:
            case 0x94:
            case 0x8C:
                    /* STY
                     * Stores the value of register Y into specified address.
                     */
                
                write(address, (short) regY);
                
                break;
            default:
                System.out.println("ILLEGAL OPCODE! " + Integer.toHexString(opcode));
                debugCounter = 0;
                break;
        }
        
        
        if(debugCounter > 0)
            debugCounter--;
        
        numInstructionsRan++;
        
        return opcodeSizes[opcode];
        
    }
    
    
    private int pull() {
        regSP++;
        regSP = (regSP & 0xFF) | 0x0100;
        return nes.getMapper().read(regSP);
    }
    
    
    private void push(short value) {
        nes.getMapper().write(regSP, value);
        regSP--;
        
        regSP = (regSP & 0xFF) | 0x0100;
    }
    
    
    public int read(int address) {
        return nes.getMapper().read(address);
    }
    
    
    public void writeJoypadInput(int num, short value) {
        if(value == 0 && joypadLastWrite[0] == 1) {
            joypadsStrobeCount[0] = 0;
            joypadsStrobeCount[1] = 0;
        }
        joypadLastWrite[0] = value;
        
    }
    
    
    public short readJoypadInput(int num) {
        int value = 0;
        
        switch(joypadsStrobeCount[num]) {
            case 0:
                value = nes.getJoypadInput(num).getButtonState(JoypadInput.BUTTON_A);
                break;
            case 1:
                value = nes.getJoypadInput(num).getButtonState(JoypadInput.BUTTON_B);
                break;
            case 2:
                value = nes.getJoypadInput(num).getButtonState(JoypadInput.BUTTON_SELECT);
                break;
            case 3:
                value = nes.getJoypadInput(num).getButtonState(JoypadInput.BUTTON_START);
                break;
            case 4:
                value = nes.getJoypadInput(num).getButtonState(JoypadInput.BUTTON_UP);
                break;
            case 5:
                value = nes.getJoypadInput(num).getButtonState(JoypadInput.BUTTON_DOWN);
                break;
            case 6:
                value = nes.getJoypadInput(num).getButtonState(JoypadInput.BUTTON_LEFT);
                break;
            case 7:
                value = nes.getJoypadInput(num).getButtonState(JoypadInput.BUTTON_RIGHT);
                break;
                
            case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15: case 16: case 17:
                value = 0;
                break;
                
            case 18:
                value = (short)((num == 0)?0:1);
                break;
                
            case 19:
                value = (short)((num == 0)?1:0);
                break;
                
                
            default:
                value = 0;
        }
        
        joypadsStrobeCount[num]++;
        if(joypadsStrobeCount[num] == 24)
            joypadsStrobeCount[num] = 0;
        
        return (short)value;
    }
    
    
    public void requestIRQ(int irqType) {
        if (irqRequested && irqRequestType == IRQ_NORMAL)
            return;
        
        irqRequested = true;
        irqRequestType = irqType;
    }
    
    
    public void write(int address, short value) {
        nes.getMapper().write(address, value);
    }
    
    
    public void initOpcodes() {
        opcodeCycles = new short[256];
        opcodeSizes = new short[256];
        opcodeModes = new short[256];
        opcodeNames = new String[256];
        addressModeNames = new String[0x14];
        
        /*
         * opcode sizes
         */
        // ADC - Add memory and Carry to Accumulator
        opcodeSizes[0x69] = 2; // Immediate
        opcodeSizes[0x65] = 2; // Zero Page
        opcodeSizes[0x75] = 2; // Zero Page, X
        opcodeSizes[0x6D] = 3; // Absolute
        opcodeSizes[0x7D] = 3; // Absolute, X
        opcodeSizes[0x79] = 3; // Absolute, Y
        opcodeSizes[0x61] = 2; // Indirect, X
        opcodeSizes[0x71] = 2; // Indirect, Y
        // AND - Logically AND Memory with Accumulator
        opcodeSizes[0x29] = 2; // Immediate
        opcodeSizes[0x25] = 2; // Zero Page
        opcodeSizes[0x35] = 2; // Zero Page, X
        opcodeSizes[0x2D] = 3; // Absolute
        opcodeSizes[0x3D] = 3; // Absolute, X
        opcodeSizes[0x39] = 3; // Absolute, Y
        opcodeSizes[0x21] = 2; // Indirect, X
        opcodeSizes[0x31] = 2; // Indirect, Y
        // ASL - Shift Left (Memory or Accumulator)
        opcodeSizes[0x0A] = 1; // Accumulator
        opcodeSizes[0x06] = 2; // Zero Page
        opcodeSizes[0x16] = 2; // Zero Page, X
        opcodeSizes[0x0E] = 3; // Absolute
        opcodeSizes[0x1E] = 3; // Absolute, X
        // BCC - Branch if Carry Flag Cleared
        opcodeSizes[0x90] = 2; // Relative
        // BCS - Branch if Carry Flag is Set
        opcodeSizes[0xB0] = 2; // Relative
        // BEQ - Branch if result equal to zero.
        opcodeSizes[0xF0] = 2; // Relative
        // BIT - Bit test
        opcodeSizes[0x24] = 2; // Zero Page
        opcodeSizes[0x2C] = 3; // Absolute
        // BMI - Branch if result is minus
        opcodeSizes[0x30] = 2; // relative
        // BNE - Branch if result is ont equal to zero
        opcodeSizes[0xD0] = 2; // Relative
        // BPL - Branch if result is plus
        opcodeSizes[0x10] = 2; // Relative
        // BRK - Break command
        opcodeSizes[0x00] = 1; // Implied
        // BVC - Branch if overflow flag (V) is cleared
        opcodeSizes[0x50] = 2; // Relative
        // BVS - Branch if overflow flag (V) is set
        opcodeSizes[0x70] = 2; // Relative
        // CLS - Clear carry flag
        opcodeSizes[0x18] = 1; // Relative
        // CLD - Clear decimal mode flag
        opcodeSizes[0xD8] = 1; // Relative
        // CLI - Clear interrupt disable flag
        opcodeSizes[0x58] = 1; // Relative
        // CLV - Clear overflow flag
        opcodeSizes[0xB8] = 1; // Relative
        // CMP - Compare accumulator with memory
        opcodeSizes[0xC9] = 2; // Immediate
        opcodeSizes[0xC5] = 2; // Zero Page
        opcodeSizes[0xD5] = 2; // Zero Page, X
        opcodeSizes[0xCD] = 3; // Absolute
        opcodeSizes[0xDD] = 3; // Absolute, X
        opcodeSizes[0xD9] = 3; // Absolute, Y
        opcodeSizes[0xC1] = 2; // Indirect, X
        opcodeSizes[0xD1] = 2; // Indirect, Y
        // CPX - Compare the X register with memory
        opcodeSizes[0xE0] = 2; // Immediate
        opcodeSizes[0xE4] = 2; // Zero Page
        opcodeSizes[0xEC] = 3; // Absolute
        // CPY - Compare Y register with memory
        opcodeSizes[0xC0] = 2; // Immediate
        opcodeSizes[0xC4] = 2; // Zero Page
        opcodeSizes[0xCC] = 3; // Absolute
        // DEC - Decrement memory contents
        opcodeSizes[0xC6] = 2; // Zero Page
        opcodeSizes[0xD6] = 2; // Zero Page, X
        opcodeSizes[0xCE] = 3; // Absolute
        opcodeSizes[0xDE] = 3; // Absolute, X
        // DEX - Decrement the X register
        opcodeSizes[0xCA] = 1; // Implied
        // DEY - Decrement the Y register
        opcodeSizes[0x88] = 1; // Implied
        // EOR -   Increment memory data
        opcodeSizes[0x49] = 2; // Immediate
        opcodeSizes[0x45] = 2; // Zero Page
        opcodeSizes[0x55] = 2; // Zero Page, X
        opcodeSizes[0x4D] = 3; // Absolute
        opcodeSizes[0x5D] = 3; // Absolute, X
        opcodeSizes[0x59] = 3; // Absolute, Y
        opcodeSizes[0x41] = 2; // Indirect, X
        opcodeSizes[0x51] = 2; // Indirect, Y
        // INC - Increment memory data
        opcodeSizes[0xE6] = 2; // Zero Page
        opcodeSizes[0xF6] = 2; // Zero Page, X
        opcodeSizes[0xEE] = 3; // Absolute
        opcodeSizes[0xFE] = 3; // Absolute, X
        // INX � Increment the X register
        opcodeSizes[0xE8] = 1; // Implied
        // INY - Increment the Y register
        opcodeSizes[0xC8] = 1; // Implied
        // JMP - Unconditional Jump
        opcodeSizes[0x4C] = 3; // Absolute
        opcodeSizes[0x2C] = 3; // Indirect
        // JSR - Jump to subroutine
        opcodeSizes[0x20] = 3; // Absolute
        // LDA - Load accumulator from memory
        opcodeSizes[0xA9] = 2; // Immediate
        opcodeSizes[0xA5] = 2; // Zero Page
        opcodeSizes[0xB5] = 2; // Zero Page, X
        opcodeSizes[0xAD] = 3; // Absolute
        opcodeSizes[0xBD] = 3; // Absolute, X
        opcodeSizes[0xB9] = 3; // Absolute, Y
        opcodeSizes[0xA1] = 2; // Indirect, X
        opcodeSizes[0xB1] = 2; // Indirect, Y
        // LDX - Load X from memory
        opcodeSizes[0xA2] = 2; // Immediate
        opcodeSizes[0xA6] = 2; // Zero Page
        opcodeSizes[0xB6] = 2; // Zero Page, Y
        opcodeSizes[0xAE] = 3; // Absolute
        opcodeSizes[0xBE] = 3; // Absolute, Y
        // LDY - Load Y from memory
        opcodeSizes[0xA0] = 2; // Immediate
        opcodeSizes[0xA4] = 2; // Zero Page
        opcodeSizes[0xB4] = 2; // Zero Page, X
        opcodeSizes[0xAC] = 3; // Absolute
        opcodeSizes[0xBC] = 3; // Absolute, X
        // LSR - Shift right (memory or accumulator)
        opcodeSizes[0x4A] = 1; // Accumulator
        opcodeSizes[0x46] = 2; // Zero Page
        opcodeSizes[0x56] = 2; // Zero Page, X
        opcodeSizes[0x4E] = 3; // Absolute
        opcodeSizes[0x5E] = 3; // Absolute, X
        // NOP - No Operation
        opcodeSizes[0xEA] = 1; // Implied
        // ORA - OR memory with accumulator
        opcodeSizes[0x09] = 2; // Immediate
        opcodeSizes[0x05] = 2; // Zero Page
        opcodeSizes[0x15] = 2; // Zero Page, X
        opcodeSizes[0x0D] = 3; // Absolute
        opcodeSizes[0x1D] = 3; // Absolute, X
        opcodeSizes[0x19] = 3; // Absolute, Y
        opcodeSizes[0x01] = 2; // Indirect, X
        opcodeSizes[0x11] = 2; // Indirect, Y
        // PHA - Push accumulator on stack
        opcodeSizes[0x48] = 1; // Implied
        // PHP - Push processor status register on stack
        opcodeSizes[0x08] = 1; // Implied
        // PLA - Pull accumulator from stack
        opcodeSizes[0x68] = 1; // Implied
        // ROL - Rotate left (accumulator or memory)
        opcodeSizes[0x2A] = 1; // Accumulator
        opcodeSizes[0x26] = 2; // Zero Page
        opcodeSizes[0x36] = 2; // Zero Page, X
        opcodeSizes[0x2E] = 3; // Absolute
        opcodeSizes[0x3E] = 3; // Absolute, X
        // ROR - Rotate right (accumulator or memory)
        opcodeSizes[0x6A] = 1; // Accumulator
        opcodeSizes[0x66] = 2; // Zero Page
        opcodeSizes[0x76] = 2; // Zero Page, X
        opcodeSizes[0x6E] = 3; // Absolute
        opcodeSizes[0x7E] = 3; // Absolute, X
        // RTI - Return from interrupt
        opcodeSizes[0x40] = 1; // Implied
        // RTS - Return from subroutine
        opcodeSizes[0x60] = 1; // Implied
        // SBC - Subtract memory from accumulator with borrow
        opcodeSizes[0xE9] = 2; // Immediate
        opcodeSizes[0xE5] = 2; // Zero Page
        opcodeSizes[0xF5] = 2; // Zero Page, X
        opcodeSizes[0xED] = 3; // Absolute
        opcodeSizes[0xFD] = 3; // Absolute, X
        opcodeSizes[0xF9] = 3; // Absolute, Y
        opcodeSizes[0xE1] = 2; // Indirect, X
        opcodeSizes[0xF1] = 2; // Indirect, Y
        // SEC - Set carry flag
        opcodeSizes[0x38] = 1; // Implied
        // SED - Set decimal mode flag
        opcodeSizes[0xF8] = 1; // Implied
        // SEI - Set interrupt disable flag
        opcodeSizes[0x78] = 1; // Implied
        // STA - Store accumulator in memory
        opcodeSizes[0x85] = 2; // Zero Page
        opcodeSizes[0x95] = 2; // Zero Page, X
        opcodeSizes[0x8D] = 3; // Absolute
        opcodeSizes[0x9D] = 3; // Absolute, X
        opcodeSizes[0x99] = 3; // Absolute, Y
        opcodeSizes[0x81] = 2; // Indirect, X
        opcodeSizes[0x91] = 2; // Indirect, Y
        // STX - Store X register in memory
        opcodeSizes[0x86] = 2; // Zero Page
        opcodeSizes[0x96] = 2; // Zero Page, Y
        opcodeSizes[0x8E] = 3; // Absolute
        // STY - Store Y register in memory
        opcodeSizes[0x84] = 2; // Zero Page
        opcodeSizes[0x94] = 2; // Zero Page, X
        opcodeSizes[0x8C] = 3; // Absolute
        // TAX - Transfer accumulator to X
        opcodeSizes[0xAA] = 1; // Implied
        // TAY - Transfer accumulator to Y
        opcodeSizes[0xA8] = 1; // Implied
        // TSX - Transfer the stack pointer to the X register
        opcodeSizes[0xBA] = 1; // Implied
        // TXA - Transfer X to the accumulator
        opcodeSizes[0x8A] = 1; // Implied
        // TXS - Transfer X to the stack pointer
        opcodeSizes[0x9A] = 1; // Implied
        // TYA - OR memory with accumulator
        opcodeSizes[0x98] = 1; // Implied
        
        
        /*
         * opcode cycles
         */
        // ADC - Add memory and Carry to Accumulator
        opcodeCycles[0x69] = 2; // Immediate
        opcodeCycles[0x65] = 3; // Zero Page
        opcodeCycles[0x75] = 4; // Zero Page, X
        opcodeCycles[0x6D] = 4; // Absolute
        opcodeCycles[0x7D] = 4; // Absolute, X
        opcodeCycles[0x79] = 4; // Absolute, Y
        opcodeCycles[0x61] = 6; // Indirect, X
        opcodeCycles[0x71] = 5; // Indirect, Y
        // AND - Logically AND Memory with Accumulator
        opcodeCycles[0x29] = 2; // Immediate
        opcodeCycles[0x25] = 3; // Zero Page
        opcodeCycles[0x35] = 4; // Zero Page, X
        opcodeCycles[0x2D] = 4; // Absolute
        opcodeCycles[0x3D] = 4; // Absolute, X
        opcodeCycles[0x39] = 4; // Absolute, Y
        opcodeCycles[0x21] = 6; // Indirect, X
        opcodeCycles[0x31] = 5; // Indirect, Y
        // ASL - Shift Left (Memory or Accumulator)
        opcodeCycles[0x0A] = 2; // Accumulator
        opcodeCycles[0x06] = 5; // Zero Page
        opcodeCycles[0x16] = 6; // Zero Page, X
        opcodeCycles[0x0E] = 6; // Absolute
        opcodeCycles[0x1E] = 7; // Absolute, X
        // BCC - Branch if Carry Flag Cleared
        opcodeCycles[0x90] = 2; // Relative
        // BCS - Branch if Carry Flag is Set
        opcodeCycles[0xB0] = 2; // Relative
        // BEQ - Branch if result equal to zero.
        opcodeCycles[0xF0] = 2; // Relative
        // BIT - Bit test
        opcodeCycles[0x24] = 3; // Zero Page
        opcodeCycles[0x2C] = 4; // Absolute
        // BMI - Branch if result is minus
        opcodeCycles[0x30] = 2; // relative
        // BNE - Branch if result is ont equal to zero
        opcodeCycles[0xD0] = 2; // Relative
        // BPL - Branch if result is plus
        opcodeCycles[0x10] = 2; // Relative
        // BRK - Break command
        opcodeCycles[0x00] = 7; // Implied
        // BVC - Branch if overflow flag (V) is cleared
        opcodeCycles[0x50] = 2; // Relative
        // BVS - Branch if overflow flag (V) is set
        opcodeCycles[0x70] = 2; // Relative
        // CLS - Clear carry flag
        opcodeCycles[0x18] = 2; // Relative
        // CLD - Clear decimal mode flag
        opcodeCycles[0xD8] = 2; // Relative
        // CLI - Clear interrupt disable flag
        opcodeCycles[0x58] = 2; // Relative
        // CLV - Clear overflow flag
        opcodeCycles[0xB8] = 2; // Relative
        // CMP - Compare accumulator with memory
        opcodeCycles[0xC9] = 3; // Immediate
        opcodeCycles[0xC5] = 3; // Zero Page
        opcodeCycles[0xD5] = 4; // Zero Page, X
        opcodeCycles[0xCD] = 4; // Absolute
        opcodeCycles[0xDD] = 4; // Absolute, X
        opcodeCycles[0xD9] = 4; // Absolute, Y
        opcodeCycles[0xC1] = 6; // Indirect, X
        opcodeCycles[0xD1] = 5; // Indirect, Y
        // CPX - Compare the X register with memory
        opcodeCycles[0xE0] = 2; // Immediate
        opcodeCycles[0xE4] = 3; // Zero Page
        opcodeCycles[0xEC] = 4; // Absolutenow that
        // CPY - Compare Y register with memory
        opcodeCycles[0xC0] = 2; // Immediate
        opcodeCycles[0xC4] = 3; // Zero Page
        opcodeCycles[0xCC] = 4; // Absolute
        // DEC - Decrement memory contents
        opcodeCycles[0xC6] = 5; // Zero Page
        opcodeCycles[0xD6] = 6; // Zero Page, X
        opcodeCycles[0xCE] = 6; // Absolute
        opcodeCycles[0xDE] = 7; // Absolute, X
        // DEX - Decrement the X register
        opcodeCycles[0xCA] = 2; // Implied
        // DEY - Decrement the Y register
        opcodeCycles[0x88] = 2; // Implied
        // EOR -   Increment memory data
        opcodeCycles[0x49] = 2; // Immediate
        opcodeCycles[0x45] = 3; // Zero Page
        opcodeCycles[0x55] = 4; // Zero Page, X
        opcodeCycles[0x4D] = 4; // Absolute
        opcodeCycles[0x5D] = 4; // Absolute, X
        opcodeCycles[0x59] = 4; // Absolute, Y
        opcodeCycles[0x41] = 6; // Indirect, X
        opcodeCycles[0x51] = 5; // Indirect, Y
        // INC - Increment memory data
        opcodeCycles[0xE6] = 5; // Zero Page
        opcodeCycles[0xF6] = 6; // Zero Page, X
        opcodeCycles[0xEE] = 6; // Absolute
        opcodeCycles[0xFE] = 7; // Absolute, X
        // INX � Increment the X register
        opcodeCycles[0xE8] = 2; // Implied
        // INY - Increment the Y register
        opcodeCycles[0xC8] = 2; // Implied
        // JMP - Unconditional Jump
        opcodeCycles[0x4C] = 3; // Absolute
        opcodeCycles[0x2C] = 5; // Indirect
        // JSR - Jump to subroutine
        opcodeCycles[0x20] = 6; // Absolute
        // LDA - Load accumulator from memory
        opcodeCycles[0xA9] = 2; // Immediate
        opcodeCycles[0xA5] = 3; // Zero Page
        opcodeCycles[0xB5] = 4; // Zero Page, X
        opcodeCycles[0xAD] = 4; // Absolute
        opcodeCycles[0xBD] = 4; // Absolute, X
        opcodeCycles[0xB9] = 4; // Absolute, Y
        opcodeCycles[0xA1] = 6; // Indirect, X
        opcodeCycles[0xB1] = 5; // Indirect, Y
        // LDX - Load X from memory
        opcodeCycles[0xA2] = 2; // Immediate
        opcodeCycles[0xA6] = 3; // Zero Page
        opcodeCycles[0xB6] = 4; // Zero Page, Y
        opcodeCycles[0xAE] = 4; // Absolute
        opcodeCycles[0xBE] = 4; // Absolute, Y
        // LDY - Load Y from memory
        opcodeCycles[0xA0] = 2; // Immediate
        opcodeCycles[0xA4] = 3; // Zero Page
        opcodeCycles[0xB4] = 4; // Zero Page, X
        opcodeCycles[0xAC] = 4; // Absolute
        opcodeCycles[0xBC] = 4; // Absolute, X
        // LSR - Shift right (memory or accumulator)
        opcodeCycles[0x4A] = 2; // Accumulator
        opcodeCycles[0x46] = 5; // Zero Page
        opcodeCycles[0x56] = 6; // Zero Page, X
        opcodeCycles[0x4E] = 6; // Absolute
        opcodeCycles[0x5E] = 7; // Absolute, X
        // NOP - No Operation
        opcodeCycles[0xEA] = 2; // Implied
        // ORA - OR memory with accumulator
        opcodeCycles[0x09] = 2; // Immediate
        opcodeCycles[0x05] = 3; // Zero Page
        opcodeCycles[0x15] = 4; // Zero Page, X
        opcodeCycles[0x0D] = 4; // Absolute
        opcodeCycles[0x1D] = 4; // Absolute, X
        opcodeCycles[0x19] = 4; // Absolute, Y
        opcodeCycles[0x01] = 6; // Indirect, X
        opcodeCycles[0x11] = 5; // Indirect, Y
        // PHA - Push accumulator on stack
        opcodeCycles[0x48] = 3; // Implied
        // PHP - Push processor status register on stack
        opcodeCycles[0x08] = 3; // Implied
        // PLA - Pull accumulator from stack
        opcodeCycles[0x68] = 4; // Implied
        // ROL - Rotate left (accumulator or memory)
        opcodeCycles[0x2A] = 2; // Accumulator
        opcodeCycles[0x26] = 5; // Zero Page
        opcodeCycles[0x36] = 6; // Zero Page, X
        opcodeCycles[0x2E] = 6; // Absolute
        opcodeCycles[0x3E] = 7; // Absolute, X
        // ROR - Rotate right (accumulator or memory)
        opcodeCycles[0x6A] = 2; // Accumulator
        opcodeCycles[0x66] = 5; // Zero Page
        opcodeCycles[0x76] = 6; // Zero Page, X
        opcodeCycles[0x6E] = 6; // Absolute
        opcodeCycles[0x7E] = 7; // Absolute, X
        // RTI - Return from interrupt
        opcodeCycles[0x40] = 6; // Implied
        // RTS - Return from subroutine
        opcodeCycles[0x60] = 6; // Implied
        // SBC - Subtract memory from accumulator with borrow
        opcodeCycles[0xE9] = 2; // Immediate
        opcodeCycles[0xE5] = 3; // Zero Page
        opcodeCycles[0xF5] = 4; // Zero Page, X
        opcodeCycles[0xED] = 4; // Absolute
        opcodeCycles[0xFD] = 4; // Absolute, X
        opcodeCycles[0xF9] = 4; // Absolute, Y
        opcodeCycles[0xE1] = 6; // Indirect, X
        opcodeCycles[0xF1] = 5; // Indirect, Y
        // SEC - Set carry flag
        opcodeCycles[0x38] = 2; // Implied
        // SED - Set decimal mode flag
        opcodeCycles[0xF8] = 2; // Implied
        // SEI - Set interrupt disable flag
        opcodeCycles[0x78] = 2; // Implied
        // STA - Store accumulator in memory
        opcodeCycles[0x85] = 3; // Zero Page
        opcodeCycles[0x95] = 4; // Zero Page, X
        opcodeCycles[0x8D] = 4; // Absolute
        opcodeCycles[0x9D] = 5; // Absolute, X
        opcodeCycles[0x99] = 5; // Absolute, Y
        opcodeCycles[0x81] = 6; // Indirect, X
        opcodeCycles[0x91] = 6; // Indirect, Y
        // STX - Store X register in memory
        opcodeCycles[0x86] = 3; // Zero Page
        opcodeCycles[0x96] = 4; // Zero Page, Y
        opcodeCycles[0x8E] = 4; // Absolute
        // STY - Store Y register in memory
        opcodeCycles[0x84] = 3; // Zero Page
        opcodeCycles[0x94] = 4; // Zero Page, X
        opcodeCycles[0x8C] = 4; // Absolute
        // TAX - Transfer accumulator to X
        opcodeCycles[0xAA] = 2; // Implied
        // TAY - Transfer accumulator to Y
        opcodeCycles[0xA8] = 2; // Implied
        // TSX - Transfer the stack pointer to the X register
        opcodeCycles[0xBA] = 2; // Implied
        // TXA - Transfer X to the accumulator
        opcodeCycles[0x8A] = 2; // Implied
        // TXS - Transfer X to the stack pointer
        opcodeCycles[0x9A] = 2; // Implied
        // TYA - OR memory with accumulator
        opcodeCycles[0x98] = 2; // Implied
        
        
        // address modes
        // ADC (Add with Carry)
        opcodeModes[0x69] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0x65] = ADDR_ZP; //Zero Page
        opcodeModes[0x75] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x6D] = ADDR_ABS; //Absolute
        opcodeModes[0x7D] = ADDR_ABS_X; //Absolute,X
        opcodeModes[0x79] = ADDR_ABS_Y; //Absolute,Y
        opcodeModes[0x61] = ADDR_INDIR_X; //Indirect,X
        opcodeModes[0x71] = ADDR_INDIR_Y; //Indirect,Y
        // AND (bitwise AND with accumulator)
        opcodeModes[0x29] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0x25] = ADDR_ZP; //Zero Page
        opcodeModes[0x35] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x2D] = ADDR_ABS; //Absolute
        opcodeModes[0x3D] = ADDR_ABS_X; //Absolute,X
        opcodeModes[0x39] = ADDR_ABS_Y; //Absolute,Y
        opcodeModes[0x21] = ADDR_INDIR_X; //Indirect,X
        opcodeModes[0x31] = ADDR_INDIR_Y; //Indirect,Y
        // ASL (Arithmetic Shift Left)
        opcodeModes[0x0A] = ADDR_ACC; //Accumulator
        opcodeModes[0x06] = ADDR_ZP; //Zero Page
        opcodeModes[0x16] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x0E] = ADDR_ABS; //Absolute
        opcodeModes[0x1E] = ADDR_ABS_X; //Absolute,X
        // BIT (test BITs)
        opcodeModes[0x24] = ADDR_ZP; //Zero Page
        opcodeModes[0x2C] = ADDR_ABS; //Absolute
        // Branch Instructions
        // BPL(Branch on PLus)
        opcodeModes[0x10] = ADDR_RELATIVE; //Relative
        // BMI(Branch on MInus)
        opcodeModes[0x30] = ADDR_RELATIVE; //Relative
        // BVC(Branch on oVerflow Clear)
        opcodeModes[0x50] = ADDR_RELATIVE; //Relative
        // BVS(Branch on oVerflow Set)
        opcodeModes[0x70] = ADDR_RELATIVE; //Relative
        // BCC(Branch on Carry Clear)
        opcodeModes[0x90] = ADDR_RELATIVE; //Relative
        // BCS(Branch on Carry Set)
        opcodeModes[0xB0] = ADDR_RELATIVE; //Relative
        // BNE(Branch on Not Equal)
        opcodeModes[0xD0] = ADDR_RELATIVE; //Relative
        // BEQ(Branch on EQual)
        opcodeModes[0xF0] = ADDR_RELATIVE; //Relative
        // BRK (BReaK)
        opcodeModes[0x00] = ADDR_IMPLIED; //Implied
        // CMP (CoMPare accumulator)
        opcodeModes[0xC9] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0xC5] = ADDR_ZP; //Zero Page
        opcodeModes[0xD5] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0xCD] = ADDR_ABS; //Absolute
        opcodeModes[0xDD] = ADDR_ABS_X; //Absolute,X
        opcodeModes[0xD9] = ADDR_ABS_Y; //Absolute,Y
        opcodeModes[0xC1] = ADDR_INDIR_X; //Indirect,X
        opcodeModes[0xD1] = ADDR_INDIR_Y; //Indirect,Y
        // CPX (ComPare X register)
        opcodeModes[0xE0] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0xE4] = ADDR_ZP; //Zero Page
        opcodeModes[0xEC] = ADDR_ABS; //Absolute
        // CPY (ComPare Y register)
        opcodeModes[0xC0] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0xC4] = ADDR_ZP; //Zero Page
        opcodeModes[0xCC] = ADDR_ABS; //Absolute
        // DEC (DECrement memory)
        opcodeModes[0xC6] = ADDR_ZP; //Zero Page
        opcodeModes[0xD6] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0xCE] = ADDR_ABS; //Absolute
        opcodeModes[0xDE] = ADDR_ABS_X; //Absolute,X
        // EOR (bitwise Exclusive OR)
        opcodeModes[0x49] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0x45] = ADDR_ZP; //Zero Page
        opcodeModes[0x55] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x4D] = ADDR_ABS; //Absolute
        opcodeModes[0x5D] = ADDR_ABS_X; //Absolute,X
        opcodeModes[0x59] = ADDR_ABS_Y; //Absolute,Y
        opcodeModes[0x41] = ADDR_INDIR_X; //Indirect,X
        opcodeModes[0x51] = ADDR_INDIR_Y; //Indirect,Y
        // Flag (Processor Status) Instructions
        // CLC(CLear Carry)
        opcodeModes[0x18] = ADDR_IMPLIED; //Implied
        // SEC(SEt Carry)
        opcodeModes[0x38] = ADDR_IMPLIED; //Implied
        // CLI(CLear Interrupt)
        opcodeModes[0x58] = ADDR_IMPLIED; //Implied
        // SEI(SEt Interrupt)
        opcodeModes[0x78] = ADDR_IMPLIED; //Implied
        // CLV(CLear oVerflow)
        opcodeModes[0xB8] = ADDR_IMPLIED; //Implied
        // CLD(CLear Decimal)
        opcodeModes[0xD8] = ADDR_IMPLIED; //Implied
        // SED(SEt Decimal)
        opcodeModes[0xF8] = ADDR_IMPLIED; //Implied
        // INC (INCrement memory)
        opcodeModes[0xE6] = ADDR_ZP; //Zero Page
        opcodeModes[0xF6] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0xEE] = ADDR_ABS; //Absolute
        opcodeModes[0xFE] = ADDR_ABS_X; //Absolute,X
        // JMP (JuMP)
        opcodeModes[0x4C] = ADDR_ABS; //Absolute
        opcodeModes[0x6C] = ADDR_INDIR; //Indirect
        // JSR (Jump to SubRoutine)
        opcodeModes[0x20] = ADDR_ABS; //Absolute
        // LDA (LoaD Accumulator)
        opcodeModes[0xA9] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0xA5] = ADDR_ZP; //Zero Page
        opcodeModes[0xB5] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0xAD] = ADDR_ABS; //Absolute
        opcodeModes[0xBD] = ADDR_ABS_X; //Absolute,X
        opcodeModes[0xB9] = ADDR_ABS_Y; //Absolute,Y
        opcodeModes[0xA1] = ADDR_INDIR_X; //Indirect,X
        opcodeModes[0xB1] = ADDR_INDIR_Y; //Indirect,Y
        // LDX (LoaD X register)
        opcodeModes[0xA2] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0xA6] = ADDR_ZP; //Zero Page
        opcodeModes[0xB6] = ADDR_ZP_Y; //Zero Page,Y
        opcodeModes[0xAE] = ADDR_ABS; //Absolute
        opcodeModes[0xBE] = ADDR_ABS_Y; //Absolute,Y
        // LDY (LoaD Y register)
        opcodeModes[0xA0] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0xA4] = ADDR_ZP; //Zero Page
        opcodeModes[0xB4] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0xAC] = ADDR_ABS; //Absolute
        opcodeModes[0xBC] = ADDR_ABS_X; //Absolute,X
        // LSR (Logical Shift Right)
        opcodeModes[0x4A] = ADDR_ACC; //Accumulator
        opcodeModes[0x46] = ADDR_ZP; //Zero Page
        opcodeModes[0x56] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x4E] = ADDR_ABS; //Absolute
        opcodeModes[0x5E] = ADDR_ABS_X; //Absolute,X
        // NOP (No OPeration)
        opcodeModes[0xEA] = ADDR_IMPLIED; //Implied
        // ORA (bitwise OR with Accumulator)
        opcodeModes[0x09] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0x05] = ADDR_ZP; //Zero Page
        opcodeModes[0x15] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x0D] = ADDR_ABS; //Absolute
        opcodeModes[0x1D] = ADDR_ABS_X; //Absolute,X
        opcodeModes[0x19] = ADDR_ABS_Y; //Absolute,Y
        opcodeModes[0x01] = ADDR_INDIR_X; //Indirect,X
        opcodeModes[0x11] = ADDR_INDIR_Y; //Indirect,Y
        // Register Instructions
        // TAX(Transfer A to X)
        opcodeModes[0xAA] = ADDR_IMPLIED; //Implied
        // TXA(Transfer X to A)
        opcodeModes[0x8A] = ADDR_IMPLIED; //Implied
        // DEX(DEcrement X)
        opcodeModes[0xCA] = ADDR_IMPLIED; //Implied
        // INX(INcrement X)
        opcodeModes[0xE8] = ADDR_IMPLIED; //Implied
        // TAY(Transfer A to Y)
        opcodeModes[0xA8] = ADDR_IMPLIED; //Implied
        // TYA(Transfer Y to A)
        opcodeModes[0x98] = ADDR_IMPLIED; //Implied
        // DEY(DEcrement Y)
        opcodeModes[0x88] = ADDR_IMPLIED; //Implied
        // INY(INcrement Y)
        opcodeModes[0xC8] = ADDR_IMPLIED; //Implied
        // ROL (ROtate Left)
        opcodeModes[0x2A] = ADDR_ACC; //Accumulator
        opcodeModes[0x26] = ADDR_ZP; //Zero Page
        opcodeModes[0x36] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x2E] = ADDR_ABS; //Absolute
        opcodeModes[0x3E] = ADDR_ABS_X; //Absolute,X
        // ROR (ROtate Right)
        opcodeModes[0x6A] = ADDR_ACC; //Accumulator
        opcodeModes[0x66] = ADDR_ZP; //Zero Page
        opcodeModes[0x76] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x6E] = ADDR_ABS; //Absolute
        opcodeModes[0x7E] = ADDR_ABS_X; //Absolute,X
        // RTI (ReTurn from Interrupt)
        opcodeModes[0x40] = ADDR_IMPLIED; //Implied
        // RTS (ReTurn from Subroutine)
        opcodeModes[0x60] = ADDR_IMPLIED; //Implied
        // SBC (SuBtract with Carry)
        opcodeModes[0xE9] = ADDR_IMMEDIATE; //Immediate
        opcodeModes[0xE5] = ADDR_ZP; //Zero Page
        opcodeModes[0xF5] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0xED] = ADDR_ABS; //Absolute
        opcodeModes[0xFD] = ADDR_ABS_X; //Absolute,X
        opcodeModes[0xF9] = ADDR_ABS_Y; //Absolute,Y
        opcodeModes[0xE1] = ADDR_INDIR_X; //Indirect,X
        opcodeModes[0xF1] = ADDR_INDIR_Y; //Indirect,Y
        // STA (STore Accumulator)
        opcodeModes[0x85] = ADDR_ZP; //Zero Page
        opcodeModes[0x95] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x8D] = ADDR_ABS; //Absolute
        opcodeModes[0x9D] = ADDR_ABS_X; //Absolute,X
        opcodeModes[0x99] = ADDR_ABS_Y; //Absolute,Y
        opcodeModes[0x81] = ADDR_INDIR_X; //Indirect,X
        opcodeModes[0x91] = ADDR_INDIR_Y; //Indirect,Y
        // Stack Instructions
        // TXS(Transfer X to Stack ptr)
        opcodeModes[0x9A] = ADDR_IMPLIED; //Implied
        // TSX(Transfer Stack ptr to X)
        opcodeModes[0xBA] = ADDR_IMPLIED; //Implied
        // PHA(PusH Accumulator)
        opcodeModes[0x48] = ADDR_IMPLIED; //Implied
        // PLA(PuLl Accumulator)
        opcodeModes[0x68] = ADDR_IMPLIED; //Implied
        // PHP(PusH Processor status)
        opcodeModes[0x08] = ADDR_IMPLIED; //Implied
        // PLP(PuLl Processor status)
        opcodeModes[0x28] = ADDR_IMPLIED; //Implied
        // STX (STore X register)
        opcodeModes[0x86] = ADDR_ZP; //Zero Page
        opcodeModes[0x96] = ADDR_ZP_Y; //Zero Page,Y
        opcodeModes[0x8E] = ADDR_ABS; //Absolute
        // STY (STore Y register)
        opcodeModes[0x84] = ADDR_ZP; //Zero Page
        opcodeModes[0x94] = ADDR_ZP_X; //Zero Page,X
        opcodeModes[0x8C] = ADDR_ABS; //Absolute
        
        
        
        opcodeNames[0x69] = "ADC";
        opcodeNames[0x65] = "ADC";
        opcodeNames[0x75] = "ADC";
        opcodeNames[0x6D] = "ADC";
        opcodeNames[0x7D] = "ADC";
        opcodeNames[0x79] = "ADC";
        opcodeNames[0x61] = "ADC";
        opcodeNames[0x71] = "ADC";
        opcodeNames[0x29] = "AND";
        opcodeNames[0x25] = "AND";
        opcodeNames[0x35] = "AND";
        opcodeNames[0x2D] = "AND";
        opcodeNames[0x3D] = "AND";
        opcodeNames[0x39] = "AND";
        opcodeNames[0x21] = "AND";
        opcodeNames[0x31] = "AND";
        opcodeNames[0x0A] = "ASL";
        opcodeNames[0x06] ="ASL" ;
        opcodeNames[0x16] = "ASL";
        opcodeNames[0x0E] = "ASL";
        opcodeNames[0x1E] = "ASL";
        opcodeNames[0x24] = "BIT";
        opcodeNames[0x2C] = "BIT";
        opcodeNames[0x10] = "BPL";
        opcodeNames[0x30] = "BMI";
        opcodeNames[0x50] = "BVC";
        opcodeNames[0x70] = "BVS";
        opcodeNames[0x90] = "BCC";
        opcodeNames[0xB0] = "BCS";
        opcodeNames[0xD0] = "BNE";
        opcodeNames[0xF0] = "BEQ";
        opcodeNames[0x00] = "BRK";
        opcodeNames[0xC9] = "CMP";
        opcodeNames[0xC5] = "CMP";
        opcodeNames[0xD5] = "CMP";
        opcodeNames[0xCD] = "CMP";
        opcodeNames[0xDD] = "CMP";
        opcodeNames[0xD9] = "CMP";
        opcodeNames[0xC1] = "CMP";
        opcodeNames[0xD1] = "CMP";
        opcodeNames[0xE0] = "CPX";
        opcodeNames[0xE4] = "CPX";
        opcodeNames[0xEC] = "CPX";
        opcodeNames[0xC0] = "CPY";
        opcodeNames[0xC4] = "CPY";
        opcodeNames[0xCC] = "CPY";
        opcodeNames[0xC6] = "DEC";
        opcodeNames[0xD6] = "DEC";
        opcodeNames[0xCE] = "DEC";
        opcodeNames[0xDE] = "DEC";
        opcodeNames[0x49] = "EOR";
        opcodeNames[0x45] = "EOR";
        opcodeNames[0x55] = "EOR";
        opcodeNames[0x4D] = "EOR";
        opcodeNames[0x5D] = "EOR";
        opcodeNames[0x59] = "EOR";
        opcodeNames[0x41] = "EOR";
        opcodeNames[0x51] = "EOR";
        opcodeNames[0x18] = "CLC";
        opcodeNames[0x38] = "SEC";
        opcodeNames[0x58] = "CLI";
        opcodeNames[0x78] = "SEI";
        opcodeNames[0xB8] = "CLV";
        opcodeNames[0xD8] = "CLD";
        opcodeNames[0xF8] = "SED";
        opcodeNames[0xE6] = "INC";
        opcodeNames[0xF6] = "INC";
        opcodeNames[0xEE] = "INC";
        opcodeNames[0xFE] = "INC";
        opcodeNames[0x4C] = "JMP";
        opcodeNames[0x6C] = "JMP";
        opcodeNames[0x20] = "JSR";
        opcodeNames[0xA9] = "LDA";
        opcodeNames[0xA5] = "LDA";
        opcodeNames[0xB5] = "LDA";
        opcodeNames[0xAD] = "LDA";
        opcodeNames[0xBD] = "LDA";
        opcodeNames[0xB9] = "LDA";
        opcodeNames[0xA1] = "LDA";
        opcodeNames[0xB1] = "LDA";
        opcodeNames[0xA2] = "LDX";
        opcodeNames[0xA6] = "LDX";
        opcodeNames[0xB6] = "LDX";
        opcodeNames[0xAE] = "LDX";
        opcodeNames[0xBE] = "LDX";
        opcodeNames[0xA0] = "LDY";
        opcodeNames[0xA4] = "LDY";
        opcodeNames[0xB4] = "LDY";
        opcodeNames[0xAC] = "LDY";
        opcodeNames[0xBC] = "LDY";
        opcodeNames[0x4A] = "LSR";
        opcodeNames[0x46] = "LSR";
        opcodeNames[0x56] = "LSR";
        opcodeNames[0x4E] = "LSR";
        opcodeNames[0x5E] = "LSR";
        opcodeNames[0xEA] = "NOP";
        opcodeNames[0x09] = "ORA";
        opcodeNames[0x05] = "ORA";
        opcodeNames[0x15] = "ORA";
        opcodeNames[0x0D] = "ORA";
        opcodeNames[0x1D] = "ORA";
        opcodeNames[0x19] = "ORA";
        opcodeNames[0x01] = "ORA";
        opcodeNames[0x11] = "ORA";
        opcodeNames[0xAA] = "TAX";
        opcodeNames[0x8A] = "TXA";
        opcodeNames[0xCA] = "DEX";
        opcodeNames[0xE8] = "INX";
        opcodeNames[0xA8] = "TAY";
        opcodeNames[0x98] = "TYA";
        opcodeNames[0x88] = "DEY";
        opcodeNames[0xC8] = "INY";
        opcodeNames[0x2A] = "ROL";
        opcodeNames[0x26] = "ROL";
        opcodeNames[0x36] = "ROL";
        opcodeNames[0x2E] = "ROL";
        opcodeNames[0x3E] = "ROL";
        opcodeNames[0x6A] = "ROR";
        opcodeNames[0x66] = "ROR";
        opcodeNames[0x76] = "ROR";
        opcodeNames[0x6E] = "ROR";
        opcodeNames[0x7E] = "ROR";
        opcodeNames[0x40] = "RTI";
        opcodeNames[0x60] = "RTS";
        opcodeNames[0xE9] = "SBC";
        opcodeNames[0xE5] = "SBC";
        opcodeNames[0xF5] = "SBC";
        opcodeNames[0xED] = "SBC";
        opcodeNames[0xFD] = "SBC";
        opcodeNames[0xF9] = "SBC";
        opcodeNames[0xE1] = "SBC";
        opcodeNames[0xF1] = "SBC";
        opcodeNames[0x85] = "STA";
        opcodeNames[0x95] = "STA";
        opcodeNames[0x8D] = "STA";
        opcodeNames[0x9D] = "STA";
        opcodeNames[0x99] = "STA";
        opcodeNames[0x81] = "STA";
        opcodeNames[0x91] = "STA";
        opcodeNames[0x9A] = "TXS";
        opcodeNames[0xBA] = "TSX";
        opcodeNames[0x48] = "PHA";
        opcodeNames[0x68] = "PLA";
        opcodeNames[0x08] = "PHP";
        opcodeNames[0x28] = "PLP";
        opcodeNames[0x86] = "STX";
        opcodeNames[0x96] = "STX";
        opcodeNames[0x8E] = "STX";
        opcodeNames[0x84] = "STY";
        opcodeNames[0x94] = "STY";
        opcodeNames[0x8C] = "STY";
        
        addressModeNames[ADDR_ZP] = "Zero Page";
        addressModeNames[ADDR_ZP_X] = "Zero Page,X";
        addressModeNames[ADDR_ZP_Y] = "Zero Page,Y";
        addressModeNames[ADDR_ABS] = "Absolute";
        addressModeNames[ADDR_ABS_X] = "Absolute,X";
        addressModeNames[ADDR_ABS_Y] = "Absolute,Y";
        addressModeNames[ADDR_IMPLIED] = "Implied";
        addressModeNames[ADDR_ACC] = "Accumulator";
        addressModeNames[ADDR_IMMEDIATE] = "Immediate";
        addressModeNames[ADDR_INDIR] = "Indirect";
        addressModeNames[ADDR_INDIR_X] = "Indirect,X";
        addressModeNames[ADDR_INDIR_Y] = "Indirect,Y";
        addressModeNames[ADDR_RELATIVE] = "Relative";
    }
    
}

