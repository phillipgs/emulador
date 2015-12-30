package beanes.sound;

import beanes.*;
public class SquareChannel extends SoundChannel {
    
    private int dutyMode;
    private boolean lengthCounterEnable;
    private boolean envDisable;
    private int envDecayRate;
    private int lengthCounterHalt;
    private int envelope;
    
    private boolean sweepEnable;
    private int sweepCounter;
    private int sweepMode;
    private int sweepShiftCount;
    
    private int pulseTimer;
    private int lengthCounter;
    private byte volume = 120;
    
    public byte sample;
    
    
    public final static int[] DUTY_LOOKUP = new int[] {
        0, 1, 0, 0, 0, 0, 0, 0,
        0, 1, 1, 0, 0, 0, 0, 0,
        0, 1, 1, 1, 1, 0, 0, 0,
        1, 0, 0, 1, 1, 1, 1, 1,
    };
    
    public SquareChannel(PAPU papu) {
        super(papu);
    }
    
    public void write(int address, short value) {
        switch(address) {
            case 0x4000:
            case 0x4004:
                dutyMode = (value >> 6)&3;
                lengthCounterEnable = (value&0x20) == 0;
                envDisable = (value&0x10) != 0;
                envDecayRate = value&0xF;
                
                break;
                
            case 0x4001:
            case 0x4005:
                sweepEnable = (value&0x80) != 0;
                sweepCounter = (value>>4)&7;
                sweepMode = (value>>3)&1;
                sweepShiftCount = value&7;
                break;
                
            case 0x4002:
            case 0x4006:
                pulseTimer &= 0x7000;
                pulseTimer |= value;
                break;
                
            case 0x4003:
            case 0x4007:
                pulseTimer &= 0xFF;
                pulseTimer |= (value&7)<<8;
                lengthCounter = (value >> 3)&0x1F;
                break;
                
        }
        
        process();
        
    }
    
    public void clockLengthCounter() {
        if(lengthCounterEnable && lengthCounter > 0) {
            lengthCounter--;
            
            if(lengthCounter == 0) 
                process();
        }
    }
    
    
    public void process() {
        int output = 0;
        
        if(lengthCounter > 0 && pulseTimer > 7)
            if(sweepMode == 0 && (pulseTimer + (pulseTimer>>sweepShiftCount)) > 4095)
                sample = 0;
            else
                sample = (byte)(volume*DUTY_LOOKUP[(dutyMode<<3)]);
        else
            sample = 0;
        
        pulseTimer = pulseTimer - (pulseTimer >> sweepShiftCount) - 1; // -1 for square 1,  -0 for square2
    }
    
    
    
    
}
