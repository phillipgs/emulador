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
import java.awt.image.*;
import java.io.*;


public class PPU {
    BeaNES nes;
    
    public static int PPU_MEMORY_SIZE = 0x8000;
    public static int SPRITE_MEMORY_SIZE = 0x10000;
    
    public int HORIZONTAL_MIRRORING = 0;
    public int VERTICAL_MIRRORING = 1;
    
    public short[] ppuMemory;
    public short[] spriteMemory;
    
    private int[] vramMirror;
    private int[] ntMirror;
    private boolean[] solidBGLine;
    private boolean[] solidSPLine;
    private int[] raster;
    
    private boolean firstWrite = true;
    private int mirroringMode = -1;
    private short controlRegister1;
    private short controlRegister2;
    private short statusRegister;
    private short sramAddress;
    private short ppuLatch;
    
    private int loopyX;
    private int loopyT;
    private int loopyV;
    private int scanlineCycles;
    private int scanline;
    private int vblankWait;
    
    
    private int[] rgbPalette = {
        0x808080, 0x003DA6, 0x0012B0, 0x440096, 0xA1005E, 0xC70028, 0xBA0600, 0x8C1700,
        0x5C2F00, 0x104500, 0x054A00, 0x00472E, 0x004166, 0x000000, 0x050505, 0x050505,
        0xC7C7C7, 0x0077FF, 0x2155FF, 0x8237FA, 0xEB2FB5, 0xFF2950, 0xFF2000, 0xD63200,
        0xC46200, 0x358000, 0x058F00, 0x008A55, 0x0099CC, 0x212121, 0x090909, 0x090909,
        0xFFFFFF, 0x0FD7FF, 0x69A2FF, 0xD480FF, 0xFF45F3, 0xFF618B, 0xFF8833, 0xFF9C12,
        0xFABC20, 0x9FE30E, 0x2BF035, 0x0CF0A4, 0x05FBFF, 0x5E5E5E, 0x0D0D0D, 0x0D0D0D,
        0xFFFFFF, 0xA6FCFF, 0xB3ECFF, 0xDAABEB, 0xFFA8F9, 0xFFABB3, 0xFFD2B0, 0xFFEFA6,
        0xFFF79C, 0xD7E895, 0xA6EDAF, 0xA2F2DA, 0x99FFFC, 0xDDDDDD, 0x111111, 0x111111
    };
    
    
    public PPU(BeaNES nes) {
        this.nes = nes;
        ppuMemory = new short[PPU_MEMORY_SIZE];
        spriteMemory = new short[SPRITE_MEMORY_SIZE];
    }
    
    public void hardReset() {
        
        solidBGLine = new boolean[256];
        solidSPLine = new boolean[256];
        firstWrite = true;
        setMirroringMode(nes.getROM().getMirroringMode());
    }
    
    public void setMirroringMode(int mode) {
        System.out.println("Setting mirroring mode " + mode);
        if(mirroringMode == mode) return;
        mirroringMode = mode;
        
        vramMirror = new int[0x8000];
        ntMirror = new int[4];
        
        // writeup mirroring
        writeMirror(0, 0, vramMirror.length);
        
        writeMirror(0x3f20, 0x3f00, 0x20);    // color palette mirror
        writeMirror(0x3f40, 0x3f00, 0x20);    // color palette mirror
        writeMirror(0x3f80, 0x3f00, 0x20);    // color palette mirror
        writeMirror(0x3fc0, 0x3f00, 0x20);    // color palette mirror
        
        writeMirror(0x3000, 0x2000, 0xf00);   // name and attribute table mirrors
        writeMirror(0x4000, 0, 0x4000);       // mirror of first half
        
        switch(mode) {
            case ROM.VERTICAL_MIRRORING :
                writeMirror(0x2800, 0x2000, 0x400);
                writeMirror(0x2c00, 0x2400, 0x400);
                ntMirror[0] = 0;
                ntMirror[1] = 1;
                ntMirror[2] = 0;
                ntMirror[3] = 1;
                break;
                
            case ROM.HORIZONTAL_MIRRORING:
                writeMirror(0x2400, 0x2000, 0x400);
                writeMirror(0x2c00, 0x2800, 0x400);
                ntMirror[0] = 0;
                ntMirror[1] = 0;
                ntMirror[2] = 1;
                ntMirror[3] = 1;
                break;
                
            case ROM.SINGLESCREEN_MIRRORING:
                writeMirror(0x2400, 0x2000, 0x400);
                writeMirror(0x2800, 0x2000, 0x400);
                writeMirror(0x2c00, 0x2000, 0x400);
                ntMirror[0] = 0;
                ntMirror[1] = 0;
                ntMirror[2] = 0;
                ntMirror[3] = 0;
                break;
                
            case ROM.SINGLESCREEN_MIRRORING2:
                writeMirror(0x2400, 0x2400, 0x400);
                writeMirror(0x2800, 0x2400, 0x400);
                writeMirror(0x2c00, 0x2400, 0x400);
                ntMirror[0] = 1;
                ntMirror[1] = 1;
                ntMirror[2] = 1;
                ntMirror[3] = 1;
                break;
                
            default:
                ntMirror[0] = 0;
                ntMirror[1] = 1;
                ntMirror[2] = 2;
                ntMirror[3] = 3;
                break;
        }
    }
    
    
    /*
     * NTSC
     * 262 scanlines per frame
     * visible area 256x224
     *
     * 240 scanlines are on screen (top 8 and bottom 8 are chopped off)
     * Takes 3 scanlines worth of cpu cycles to enter VBLANK
     * 20 scanlines exist before next frame can be
     *
     */
    public int runCycles(int cycles) {
        int cyclesRan = cycles;
        
        while(cycles > 0) {
            
            scanlineCycles++;
            
            if(scanlineCycles==341) {
                scanlineCycles = 0;
                
                
                // wait 20 dummy scanlines
                if(vblankWait < 19) {
                    vblankWait++;
                }
                
                else {
                    
                    // handle if start of frame (dummy scanline)
                    if(scanline == 0) {
                        // copy temp address to actual
                        if((controlRegister2 & (0x08 | 0x10)) != 0) {
                            loopyV = loopyT;
                        }
                        // reset sprite0hit and vblank status flags
                        statusRegister &= 0x7F;
                        statusRegister &= 0xBF;
                    }
                    
                    
                    // render scanline if bg visibility or sp visibility is set
                    if (scanline < 240 && (controlRegister2 & (0x08 | 0x10)) != 0) {
                        renderScanline();
                    }
                    
                    // postprocessing for end of frame
                    if(scanline == 243) {
                        statusRegister |= 0x80;         // signal vblank
                        vblankWait = 0;                             // reset wait counter
                        scanline = -1;
                        
                        // request IRQ if allowed
                        if(((controlRegister1 >> 7)&1) != 0)
                            nes.getCPU().requestIRQ(nes.getCPU().IRQ_NMI);
                        nes.getVideoOutput().renderImage(raster);   // render the image
                        nes.getClock().signalVBlank();

                    }
                    
                    scanline++;
                }
                
            }
            
            cycles--;
        }
        
        return cyclesRan++;
    }
    
    
    
    
    public void renderScanline() {
        // pre scanline processing
        loopyV &= 0xFBE0;               //v:---- -0-- ---0 0000   (clear bits 0,1,2,3,4,10 = X scroll)
        loopyV |= loopyT & 0x41F;       //v:---- -X-- ---X XXXX   (set X scroll)
        
        // render scanline stuff here if possible
        if((controlRegister2&0x08) != 0) renderBackground();
        if((controlRegister2&0x10) != 0) renderSprites();
        
        
        // postscanline processing
        
        // check if bits 12,13,14 are all set (subtile Y offset = 7)
        if ((loopyV & 0x7000) == 0x7000) {
            loopyV &= 0x8FFF;                   //v:-000 ---- ---- ---- (clear bit 12,13,14 = Y tile offset)
            
            
            // check if bits 5,6,7,8,9,10 are set, but bit 11 not (name table line = 29)
            if ((loopyV & 0x03E0) == 0x03A0) {
                loopyV ^= 0x0800;               // switch name tables
                loopyV &= 0xFC1F;               // v: ---- --00 000- ---- (set name table line = 0)
            }
            
            // this is for the "wierd instance" because the nametable normally never reaches 31
            // unless it is overridden, e.g. manually set the nametable to 31
            else {
                
                // check if bits 5,6,7,8,9,10,11 are all set (name table line = 31)
                if ((loopyV & 0x03E0) == 0x03E0) {
                    loopyV &= 0xFC1F;           // v:---- --00 000- ---- (set name table line = 0)
                }
                
                else {
                    loopyV += 0x20;             // increment name table line number +=0010 0000
                }
            }
        }
        
        
        // else increment next subtile y offset
        else {
            loopyV += 0x1000;   // += 0001 0000 0000 0000
        }
        
        
    }
    
    
    public void renderBackground() {
        
        int indexX = loopyV & 0x1F;             // get x scroll
        int indexY = (loopyV & 0x3E0) >> 5;     // get y scroll
        
        
        int ntAddr = 0x2000 + (loopyV&0xFFF);   // get nametable address
        int atAddr = 0x2000 + (loopyV&0xC00)+ 0x3C0 + ((indexY&0xFFFC)<<1) + (indexX>>2); // get attribute address
        
        int attribute = 0;
        int colorAddr = 0;
        int color = 0;
        int patternAddr = 0;
        int patternMSB = 0;
        int patternLSB = 0;
        int pattern = 0;
        int col = -loopyX;
        int point;
        
        // determine attribute (msb of color pattern)
        if((indexY&2) == 0) {
            if((indexX&2) == 0)
                attribute = (ppuMemory[vramMirror[atAddr]]&3) <<2;
            else
                attribute = (ppuMemory[vramMirror[atAddr]]&0xC);
        } else {
            if((indexX&2) == 0)
                attribute = (ppuMemory[vramMirror[atAddr]]&0x30) >> 2;
            else
                attribute = (ppuMemory[vramMirror[atAddr]]&0xC0) >> 4;
        }
        for(int i = 0; i < solidBGLine.length; i++) solidBGLine[i] = false;
        
        for(int i = 0; i < 33; i++) {
            // determine pattern (lsb of color patterns)
            patternAddr = (((controlRegister1>>4)&1)*0x1000) + (ppuMemory[vramMirror[ntAddr]]<<4)+((loopyV&0x7000)>>12);
            patternLSB = ppuMemory[vramMirror[patternAddr]];
            patternMSB = ppuMemory[vramMirror[patternAddr+8]];
            
            // iterate through each pixel on line (from left to right)
            for(int j = 7; j >= 0; j--) {
                // merge the pattern (lbs of color pattern)
                pattern = (((patternMSB>>j)<<1)&2) | ((patternLSB>>j)&1);
                
                // get the address of the color, handles if transparent
                colorAddr = (pattern == 0)?0x3F10: 0x3F00 + (attribute | pattern);
                
                // get the color from color address
                color = rgbPalette[ppuMemory[vramMirror[colorAddr]]];
                
                // get the point/pixel location for image rasterization
                point = scanline*256 + col;
                
                // only handle if in range
                if(col >= 0 && col < 256) {
                    
                    // transparent if pattern = 0 (2 lsb of color = 0)
                    solidBGLine[col] = (pattern != 0);
                    
                    // draw to video if in range and bg visibility set
                    if(point < raster.length  && point < 240*256 && (controlRegister2&0x08) != 0)
                        raster[point] = color;
                }
                
                col++;
            }
            
            
            indexX++;
            ntAddr++;
            
            if ((indexX & 0x0001) == 0) {
                if ((indexX & 0x0003) == 0) {
                    if ((indexX & 0x001F) == 0) {
                        ntAddr ^= 0x0400;
                        atAddr ^= 0x0400;
                        ntAddr -= 0x0020;
                        atAddr -= 0x0008;
                        indexX -= 0x0020;
                    }
                    
                    atAddr++;
                }
                
                
                if ((indexY & 0x0002) == 0)
                    if ((indexX & 0x0002) == 0)
                        attribute = (ppuMemory[vramMirror[atAddr]] & 0x03) << 2;
                    else
                        attribute = (ppuMemory[vramMirror[atAddr]] & 0x0C);
                
                else
                    if ((indexX & 0x0002) == 0)
                        attribute = (ppuMemory[vramMirror[atAddr]] & 0x30) >> 2;
                    else
                        attribute = (ppuMemory[vramMirror[atAddr]] & 0xC0) >> 4;
                
                
            } // Dual-Tile Boundary Crossed
            
        }
        
        
        
    }
    
    
    public void renderSprites() {
        int x;
        int y;
        int patternIndex;
        int pattern;
        int patternAddr;
        int patternMSB;
        int patternLSB;
        int color;
        int colorH;
        int colorAddr;
        
        int attributes;
        boolean vflip;
        boolean hflip;
        int line;
        int col;
        boolean bgPriority;
        int numDetected = 0;
        int point;
        int pointX;
        int height = ((controlRegister1 & 0x20) != 0)?16:8;
        
        statusRegister &= 0xDF;      // gets flaged only if more than 8 sprites on a scanline
        
        for(int i = 0; i < solidSPLine.length; i++) solidSPLine[i] = false;
        for(int i = 0; i < 64; i++) {
            y = spriteMemory[i*4]+1;                    // location where sprite is to be placed (Y)
            patternIndex = spriteMemory[i*4+1];         // pattern index
            attributes = spriteMemory[i*4+2];           // attributes for sprite
            x = spriteMemory[i*4+3];                    // location where sprite is to be placed (X)
            
            // determine if flipping is neeed
            vflip = (attributes & 0x80) != 0;
            hflip = (attributes & 0x40) != 0;
            
            // determine if sprite has priority of background
            bgPriority = (attributes & 0x20) != 0;  // true=bg is in foreground
            
            colorH = (attributes << 2)&0xC;         // msb of color
            
            line = (scanline - y);                  // number of scanlines from current scanline
            
            // don't render if it is below current scanline, not within the height range
            if(line < 0 || line >= height) continue;
            line = (vflip)?((height-1)-line):line;  // flip if needed
            
            
            if(++numDetected > 8) statusRegister |= 0x20;
            
            // 8x8 tiles
            if(height == 8) {
                patternAddr = ((controlRegister1 >> 3)&1)*0x1000 + patternIndex*0x10+line;
                patternLSB = ppuMemory[vramMirror[patternAddr]];
                patternMSB = ppuMemory[vramMirror[patternAddr+8]];
            }
            
            // 8x16 tiles
            else {
                patternAddr = patternIndex << 4;
                
                if((patternIndex&1) == 1) {
                    patternAddr += 0x1000;
                    if(line <= 7) patternAddr -= 16;
                } else {
                    if(line > 7) patternAddr += 16;
                }
                
                
                patternAddr +=line&7;
                
                patternLSB = ppuMemory[vramMirror[patternAddr]];
                patternMSB = ppuMemory[vramMirror[patternAddr+8]];
            }
            
            
            // go through each pixel for scanline in sprite
            for(int j = 7; j >= 0; j--) {
                // get current point on scanline and raster point
                pointX = x + 7 - j;
                point = scanline*256 + pointX;
                col = (hflip)?7-j:j;            // handle flipping
                
                if(pointX >= 256) continue;              // row on scanline overflow
                if(solidSPLine[pointX]) continue;       // lower priority sprite overlapping
                if(point >= raster.length && point < 240*256) continue;    // large point
                
                // determine color
                pattern = (((patternMSB>>col)&1)<<1) | ((patternLSB>>col)&1);
                colorAddr = (colorH | pattern);
                color = rgbPalette[ppuMemory[vramMirror[0x3F10 + colorAddr]]];
                
                // check for sprite hit
                if(i == 0 && solidBGLine[pointX] && pattern != 0 && (controlRegister2&0x08) != 0) {
                    
                    statusRegister |= 0x40;
                }
                
                // don't display under the following conditions
                
                
                if((controlRegister2&0x10) == 0) continue;
                if(bgPriority && solidBGLine[pointX]) continue;
                if(pattern == 0) continue;
                
                raster[point] = color;
                
            }
            
            
            
        }
        
    }
    
    
    public void setRaster(int[] raster) {
        this.raster = raster;
    }
    
    
    public short externalRead(int address) {
        short value = 0;
        
        switch(address) {
            // ppu control register 1
            case 0x2000:
                value = controlRegister1;
                break;
                
                
                // ppu control register 2
            case 0x2001:
                value = controlRegister2;
                break;
                
                
                // ppu status register
            case 0x2002:
                firstWrite = true;
                value = statusRegister;
                statusRegister &= 0x7F; // clear bit 7 after read
                break;
                
                
                // sprite ram i/o register
            case 0x2004:
                value = spriteMemory[sramAddress];
                sramAddress++;
                sramAddress &= 0xFF;
                break;
                
                
                // vram i/o register
            case 0x2007:
                value = ppuLatch;
                ppuLatch = readVRAM();
                break;
                
                
        }
        
        return value;
        
    }
    
    
    public void externalWrite(int address, short value) {
        switch(address) {
            
            // ppu control register 1
            case 0x2000:
                controlRegister1 = value;
                loopyT &= 0xF3FF;           // t:---- 00-- ---- ---- (clear bits 10,11= temporary refresh address)
                loopyT |= (value&3)<<10;    // t:---- XX-- ---- ----
                break;
                
                
                // ppu control register 2
            case 0x2001:
                controlRegister2 = value;
                break;
                
                
                // ppu status register
            case 0x2002:
                // cannot be written to
                break;
                
                
                // sprite ram adress register
            case 0x2003:
                sramAddress = value;
                break;
                
                
                // sprite ram i/o register
            case 0x2004:
                spriteMemory[sramAddress] = value;
                sramAddress++;
                sramAddress &= 0xFF;
                break;
                
                
                // vram address register 1
            case 0x2005:
                // horizontal scroll
                if(firstWrite) {
                    loopyT &= 0xFFE0;           // t:---- ---- ---0 0000 (clear bits 0,1,2,3,4)
                    loopyT |= (value&0xF8)>>3;  // t:---- ---- ---X XXXX (last 3 bits of value is for fine scroll)
                    loopyX = value&7;           // fine scroll is 3 bits
                }
                // vertical scroll
                else {
                    loopyT &= 0xFC1F;           // t:---- --00 000- ---- (clear bits 5,6,7,8,9)
                    loopyT |= (value&0xF8)<<2;  // t:---- --XX XXX- ---- ((first 3 bits are put to end of loopyt)
                    loopyT &=0x8FFF;            // t:-000 ---- ---- ---- (clear bits 12,13,14 = y tile scroll)
                    loopyT |= (value&7)<<12;    // t:-XXX ---- ---- ----
                }
                firstWrite = !firstWrite;
                break;
                
                
                // vram address register 2
            case 0x2006:
                if(firstWrite) {
                    loopyT &= 0xFF;             // t:0000 0000 ---- ---- (clear bits 8,9,10,11,12,13,14,15)
                    loopyT |= (value&0x3F)<<8;  // t:0XXX XXXX 0000 0000
                } else {
                    loopyT &= 0xFF00;           // t:---- ---- 0000 0000 (clear bits 0,1,2,3,4,5,6,7)
                    loopyT |= value;            // t:0000 0000 XXXX XXXX
                    loopyV = loopyT;            // copy temp to real address
                }
                
                firstWrite = !firstWrite;
                break;
                
                // vram i/o register
            case 0x2007:
                writeVRAM(value);
                break;
                
                // sprite dma
            case 0x4014:
                spriteMemory[sramAddress] = value;
                int baseAddress = value * 0x100;
                short data;
                
                for(int i = sramAddress; i <= 0xFF; i++) {
                    data = nes.getMapper().read(baseAddress + i);
                    spriteMemory[i] = data;
                }
                break;
        }
    }
    
    
    public void writeVRAM(short value) {
        int address = vramMirror[loopyV];
        
        ppuMemory[address] = value;
        loopyV += (((controlRegister1 >> 2)&1) == 0)?1:32;
    }
    
    
    public short readVRAM() {
        short value = ppuMemory[vramMirror[loopyV]];
        
        loopyV += (((controlRegister1 >> 2)&1) == 0)?1:32;
        
        return value;
    }
    
    
    public void writeMirror(int src, int dest, int length) {
        for(int i=0; i < length; i++)
            vramMirror[src+i] = dest + i;
    }
}
