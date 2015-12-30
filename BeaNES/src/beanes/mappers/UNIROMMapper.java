package beanes.mappers;

import beanes.*;
import java.util.*;


public class UNIROMMapper extends NESMapper {
    
    public UNIROMMapper(BeaNES nes) {
        super(nes);
    }
    
    public void loadROM(ROM rom) {
        loadROMBank(0, 0x8000);
        loadROMBank(rom.numPRGBanks-1, 0xC000);
    }
    
    public void write(int address, short value) {
        if(address < 0x8000) {
            super.write(address, value);
        }
        
        else {
            loadROMBank(value, 0x8000);
        }
    }
}
