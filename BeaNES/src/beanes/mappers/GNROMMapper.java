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


public class GNROMMapper extends NESMapper {
    
    public GNROMMapper(BeaNES nes) {
        super(nes);
    }
    
    public void write(int address, short value) {
        if(address < 0x8000) {
            super.write(address, value);
        }
        
        else {
            //int bank = (value>>4)&3;
            int bank1 = (((value>>4)&3)*2) % nes.getROM().numPRGBanks;
            int bank2 = (((value>>4)&3)*2+1) % nes.getROM().numPRGBanks;
            
            loadROMBank(bank1, 0x8000);
            loadROMBank(bank2, 0xC000);
            
            if(nes.getROM().numCHRBanks == 0) return;
            
            int vbank = (value&3)*2 % nes.getROM().numCHRBanks;        
            loadVROMBank(vbank, 0x0000);
            loadVROMBank(vbank+1, 0x1000);
        }
    }
    
}
