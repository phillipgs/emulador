package beanes.sound;

import beanes.*;


public abstract class SoundChannel {
    
    private PAPU papu;
    private boolean enabled;
    
    
    public SoundChannel(PAPU papu) {
        this.papu = papu;
    }
    
    
    public boolean isEnabled() {
        return enabled;
    }
    
    
    public void setEnabled(boolean enable) {
        enabled = enable;
    }

    
    public abstract void write(int address, short value);
    
}
