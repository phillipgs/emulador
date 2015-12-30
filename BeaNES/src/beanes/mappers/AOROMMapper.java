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

package beanes.mappers;

import beanes.*;
import java.util.*;


public class AOROMMapper extends NESMapper {

    int offset;
    int mirroring;
    short[] prgROM;
    
    public AOROMMapper(BeaNES nes) {
        super(nes);

        offset = 0;
        mirroring = -1;
        
        int banks = nes.getROM().numPRGBanks;
        prgROM = new short[banks*0x4000];
        for(int i = 0; i < banks; i++)
            System.arraycopy(nes.getROM().getROMBank(i), 0, prgROM, i*0x4000, 0x4000);
        
    }
    
    public short read(int address) {
        if(address < 0x8000) {
            return super.read(address);
        }
        else {
            return prgROM[address+offset];
        }
    }
    
    public void write(int address, short value) {
        if(address < 0x8000) {
            super.write(address, value);
        }
        else {
            offset = ((value&0xF)-1) << 0xF;
            
            if(mirroring != (value&0x10)) {
                mirroring = value&0x10;
                
                if(mirroring == 0) {
                    nes.getPPU().setMirroringMode(ROM.SINGLESCREEN_MIRRORING);
                } else {
                    nes.getPPU().setMirroringMode(ROM.SINGLESCREEN_MIRRORING2);
                }
            }
        }
    }
    
}

