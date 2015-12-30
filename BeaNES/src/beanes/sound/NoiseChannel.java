package beanes.sound;

import beanes.*;

public class NoiseChannel extends SoundChannel {
    
    public NoiseChannel(PAPU papu) {
        super(papu);
    }
    
    public void write(int address, short value) {
    }
    
}
