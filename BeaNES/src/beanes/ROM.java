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

import beanes.mappers.*;
import java.io.*;


public class ROM {
    
    public static final int HORIZONTAL_MIRRORING = 0;
    public static final int VERTICAL_MIRRORING = 1;
    public static final int FOURSCREEN_MIRRORING = 2;
    public static final int SINGLESCREEN_MIRRORING = 3;
    public static final int SINGLESCREEN_MIRRORING2 = 4;
    public static final int SINGLESCREEN_MIRRORING3 = 5;
    public static final int SINGLESCREEN_MIRRORING4 = 6;
    public static final int CHRROM_MIRRORING = 7;
    
    private static final String[] mirrorDescriptions = {
        "Vertical Mirroring",
        "Horizontal Mirroring",
        "Fourscreen Mirroring",
        "Singlescreen Mirroring",
        "Singlescreen Mirroring2",
        "Singlescreen Mirroring3",
        "Singlescreen Mirroring4",
        "CHRROM Mirroring" };
    
    
    private File romFile;
    private BeaNES nes;
    private short rom[][];
    private short vrom[][];
    
    public int numPRGBanks = 0;
    public int numCHRBanks = 0;
    private int mapperType = 0;
    private int mirrorMode = 0;
    private boolean fourscreenMode = false;
    
    
    private boolean trainer = false;
    private MemoryMapper memoryMapper;
    private String[] mapperNames = new String[92];
    
    
    
    public ROM(BeaNES nes, File romFile) {
        this.nes = nes;
        this.romFile = romFile;
        
        for(int i=0;i<92;i++)
            mapperNames[i] = "Unknown Mapper";
        
        mapperNames[0] = "Mapper 0";
        mapperNames[1] = "Nintendo MMC1";
        mapperNames[2] = "UNROM";
        mapperNames[3] = "CNROM";
        mapperNames[4] = "Nintendo MMC3";
        mapperNames[5] = "Nintendo MMC5";
        mapperNames[6] = "FFE F4xxx";
        mapperNames[7] = "AOROM";
        mapperNames[8] = "FFE F3xxx";
        mapperNames[9] = "Nintendo MMC2";
        mapperNames[10] = "Nintendo MMC4";
        mapperNames[11] = "ColorDreams Chip";
        mapperNames[12] = "FFE F6xxx";
        mapperNames[15] = "100-in-1 switch";
        mapperNames[16] = "Bandai chip";
        mapperNames[17] = "FFE F8xxx";
        mapperNames[18] = "Jaleco SS8806 chip";
        mapperNames[19] = "Namcot 106 chip";
        mapperNames[20] = "Nintendo DiskSystem";
        mapperNames[21] = "Konami VRC4a";
        mapperNames[22] = "Konami VRC2a";
        mapperNames[23] = "Konami VRC2a";
        mapperNames[24] = "Konami VRC6";
        mapperNames[25] = "Konami VRC4b";
        mapperNames[32] = "Irem G-101 chip";
        mapperNames[33] = "Taito TC0190/TC0350";
        mapperNames[34] = "32kB ROM switch";
        mapperNames[64] = "Tengen RAMBO-1 chip";
        mapperNames[65] = "Irem H-3001 chip";
        mapperNames[66] = "GNROM switch";
        mapperNames[67] = "SunSoft3 chip";
        mapperNames[68] = "SunSoft4 chip";
        mapperNames[69] = "SunSoft5 FME-7 chip";
        mapperNames[71] = "Camerica chip";
        mapperNames[78] = "Irem 74HC161/32-based";
        mapperNames[91] = "Pirate HK-SF3 chip";
    }
    
    public void load() {
        // load file into memory
        // fetch header information
        System.out.println("Loading ROM file " + romFile.getName());
        int i = 0;
        int read = 0;
        
        
        
        try {
            
            FileInputStream in = new FileInputStream(romFile);
            
            short[] romData = new short[(int)romFile.length()+1];
            
            while(read > -1) {
                read = in.read();
                
                if(read == -1)
                    break;
                
                romData[i] = (short)read;
                i++;
            }
            System.out.println("ROM Size " + (romFile.length()) + "bytes");
            
            
            // check to see if it is an iNES mapper format
            if(romData[0] == 0x4e && romData[1] == 0x45 && romData[2] == 0x53 && romData[3] == 0x1a) {
                
                
                numPRGBanks = romData[4];
                
                numCHRBanks = romData[5]*2;
                
                trainer = (romData[6]&4)!= 0;
                
                mapperType = (romData[6]>>4)|(romData[7]&0xF0);
                
                mirrorMode = ((romData[6]&1) != 0)?1:0;
                
                fourscreenMode = (romData[6]&8)!=0;
                
                
                int offset = 16;
                rom = new short[numPRGBanks][0x4000];
                vrom = new short[numCHRBanks][0x1000];
                
                System.out.println("iNES formatted ROM");
                System.out.println("Mapper " + mapperType + ": " + getMapperName());
                System.out.println("Mirror Mode " + mirrorMode + ": " + mirrorDescriptions[mirrorMode]);
                System.out.println("Four Screen Mode " + (fourscreenMode?"true":false));
                
                System.out.println("Loading PRG ROM Banks");
                
                for (i = 0; i < numPRGBanks; i++) {
                    for(int j = 0; j < 0x4000; j++) {
                        if(offset+j >= romData.length)
                            break;
                        
                        rom[i][j] = romData[offset+j];
                    }
                    
                    offset += 0x4000;
                }
                
                
                System.out.println("Loading CHR ROM Banks");
                
                for (i = 0; i < numCHRBanks; i++) {
                    for(int j = 0; j < 0x1000; j++) {
                        if(offset+j >= romData.length)
                            break;
                        
                        vrom[i][j] = romData[offset+j];
                    }
                    
                    offset+= 0x1000;
                    
                    for(int k = 0; k < 0x100; k++) {
                        short[] data = new short[16];
                        
                        for(int n = 0; n < 16; n++) {
                            data[n] = vrom[i][k*16 + n];
                        }
                        
                    }
                    
                }
                
                switch(mapperType) {
                    case 2: memoryMapper = new UNIROMMapper(nes); break;
                    case 3: memoryMapper = new CNROMMapper(nes); break;
                    case 7: memoryMapper = new AOROMMapper(nes); break;
                    case 66: memoryMapper = new GNROMMapper(nes); break;
                    default: memoryMapper = new NESMapper(nes); break;
                }

                memoryMapper.loadROM(this);
                
            } else {
                System.out.println("Not an iNES formatted ROM");
            }
            
            System.out.println("Loaded ROM (" + numPRGBanks + " PRG Banks and " + numCHRBanks + " CHR banks)");
            
        } catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public short[] getROMBank(int bank) {
        return rom[bank];
    }
    
    
    public short[] getVROMBank(int bank) {
        return vrom[bank];
    }
    
    
    public short read(int bank, int address) {
        try {
            return rom[bank][address];
        } catch(ArrayIndexOutOfBoundsException e) {
            System.out.println("ERRROR: Reading ROM bank " + bank + ":" + Integer.toHexString(address));
        }
        
        return -1;
    }
    
    public void write(int bank, int address, short value) {
        rom[bank][address] = (short)value;
    }
    
    public MemoryMapper getMapper() {
        return memoryMapper;
    }
    
    public int getMirroringMode() {
        return mirrorMode;
    }
    
    public String getMapperName() {
        if(mapperType > mapperNames.length || mapperType < 0)
            return "Unknown Mapper";
        else return mapperNames[mapperType];
    }
}
