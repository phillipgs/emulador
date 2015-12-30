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

public class NESMapper implements MemoryMapper{
    
    protected BeaNES nes;
    
    public NESMapper(BeaNES nes) {
        this.nes = nes;
    }
    
    /**
     * Reads a value from memory map (memory/IO)
     */
    public short read(int address) {
        short value = 0;
        // RAM
        if(address < 0x2000) {
            value = nes.getCPU().cpuMemory[address&0x7FF];  // 0x7FF for mirroring
        }
        // PPU communication
        else if(address <= 0x2007) {
            value = nes.getPPU().externalRead(address);
        }
        
        
        //pAPU communication
        else if(address <= 0x4015)
            return value;
        
        // joypad 1 and 2
        else if(address <= 0x4016) {
            value = nes.getCPU().readJoypadInput(0);
        } else if(address <= 0x4017) {
            value = 0;
            value = nes.getCPU().readJoypadInput(1);
        } else if(address > 0x4017) {
            value = nes.getCPU().cpuMemory[address];

        } else {
            System.out.println("Illegal PPU adress read " + Integer.toHexString(address));
            System.exit(1);
        }
        
        
        return value;
    }
    
    
    /**
     * Writes to a value on the memory map (memory/IO)
     */
    public void write(int address, short value) {
        // RAM
        if (address < 0x2000) {
            nes.getCPU().cpuMemory[address&0x7FF] = value;
        }
        // PPU communication
        else if(address <= 0x2007) {
            nes.getPPU().externalWrite(address, value);
            
            return;
            
        } else if(address >= 0x4000 && address <= 0x4013) {
            nes.getPAPU().write(address, value);
            return;
        } else if(address == 0x4014) {
            nes.getPPU().externalWrite(address, value);
        } else if(address == 0x4015) {
            nes.getPAPU().write(address, value);
        } else if(address == 0x4016) {
            nes.getCPU().writeJoypadInput(0, value);
        } else if(address == 0x4017) {
            nes.getCPU().writeJoypadInput(1, value);
            
        } else if(address > 0x4017) {
            nes.getCPU().cpuMemory[address] = value;
            
            
        } else {
            System.out.println("Illegal PPU adress write " + Integer.toHexString(address));
            System.exit(1);
        }
    }
    
    public void loadROM(ROM rom) {
        loadROMBank(0, 0x8000);
        
        if(rom.numPRGBanks < 2)
            loadROMBank(0, 0xC000);
        else
            loadROMBank(1, 0xC000);
        
        if(rom.numCHRBanks >= 2) {
            loadVROMBank(0, 0x0000);
            loadVROMBank(1, 0x1000);
        }
    }
    
    public void loadROMBank(int bank, int address) {
        System.arraycopy(nes.getROM().getROMBank(bank), 0, nes.getCPU().cpuMemory, address, 0x4000);
    }
    
    public void loadVROMBank(int bank, int address) {
        System.arraycopy(nes.getROM().getVROMBank(bank), 0, nes.getPPU().ppuMemory, address, 0x1000);
    }
}
