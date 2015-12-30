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

import beanes.sound.*;
import javax.sound.sampled.*;
import java.io.*;

/*
 * Mix every time an audio register is written/read or the APU's 250 Hz timer ticks
 * Decide how many samples to mix
 *
 *random notes
 *------------
 * The nes CPU frequency divided by the timer period (19687500/11)/timerperiod
 * E.g.
 *A value of 253 load timers with (253+1)*2 = 508 resulting in a playback  (19687500/11)/(508) = 3523.2Hz,
 *which divided by the 8-step sequence is 440Hz (A note)
 *
Square 1/2 (range 54.6Hz to 12.4KHz)
$4000/4 ddle nnnn   duty cycle type, length counter clock/env decay disable , envelop decay disable, volume/envelope decay rate
$4001/5 eppp nsss   enable sweep, period, negative, shift
$4002/6 pppp pppp   period low
$4003/7 llll lppp   length index, period high
 
Triangle (range 27.3 Hz to 55.9 KHz)
$4008   clll llll   control, linear counter load
$400A   pppp pppp   period low
$400B   llll lppp   length index, period high
 
Noise (19.3 Hz to 447 KHz)
$400C   --le nnnn   loop env/disable length, env disable, vol/env period
$400E   s--- pppp   short mode, period index
$400F   llll l---   length index
 
 
 
Common
$4015   ---d nt21   length ctr enable: DMC, noise, triangle, pulse 2, 1
$4017   fd-- ----   5-frame cycle, disable frame interrupt
 
Status (read)
$4015   if-d nt21   DMC IRQ, frame IRQ, length counter statuses
 
 */


public class PAPU implements Runnable {
    public static double PAPU_FREQUENCY = 1789772.5;
    
    private BeaNES nes;
    private Thread thread;
    private SquareChannel square1;
    private SquareChannel square2;
    private NoiseChannel noise;
    private TriangleChannel triangle;
    private DMChannel dm;
    
    private Mixer mixer;
    private SourceDataLine dataLine;
    private AudioFormat audioFormat;
    
    private int refreshRate = 10;
    private int cycleRate; // cycles/sample
    private int sampleRate = 44100; // samples/second
    
    private int bufferIndex;
    private int bufferSize = 2048;
    private byte[] sampleBuffer;
    
    private int timeCounter = 0;
    
    public static final int[] lengthLookupTable = new int[]{
        0x0A, 0xFE,
        0x14, 0x02,
        0x28, 0x04,
        0x50, 0x06,
        0xA0, 0x08,
        0x3C, 0x0A,
        0x0E, 0x0C,
        0x1A, 0x0E,
        0x0C, 0x10,
        0x18, 0x12,
        0x30, 0x14,
        0x60, 0x16,
        0xC0, 0x18,
        0x48, 0x1A,
        0x10, 0x1C,
        0x20, 0x1E
    };
    
    public PAPU(BeaNES nes) {
        
        this.nes = nes;
        square1 = new SquareChannel(this);
        square2 = new SquareChannel(this);
        noise = new NoiseChannel(this);
        triangle = new TriangleChannel(this);
        dm = new DMChannel(this);
                
        cycleRate = (int)((PAPU_FREQUENCY * 0x10000) / ((float)sampleRate));
                
        sampleBuffer = new byte[bufferSize]; // 4 for steroo 2 for mono
        bufferIndex = 0;
        
        mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[1]);
        audioFormat = new AudioFormat(sampleRate, 16, 1, true, false); // 1 for mono 2 for stereo
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, 44100);
        
        try {
            dataLine = (SourceDataLine)AudioSystem.getLine(info);
            dataLine.open(audioFormat);
            dataLine.start();
            
        } catch(Exception e) {
            System.out.println("could not open audio data line");
        }
        
        thread = new Thread(this);
        thread.start();
    }
    
    public synchronized void run() {
        // do audio looping here
        
        int sleepTime = 1000/refreshRate;
        long currTime = 0;
        int waitTime = 0;
        
        // wait for 1 second
        currTime = System.currentTimeMillis()/1000;
        while(currTime == System.currentTimeMillis()/1000) { waitTime++; }

        //            test();
        //            dataLine.write(sampleBuffer, 0, bufferSize);

        while(true) {
            currTime = System.currentTimeMillis()/1000;
            
            square1.process();
            sampleBuffer[bufferIndex] = square1.sample;
            bufferIndex++;
            if(bufferIndex >= sampleBuffer.length)  {
                bufferIndex = 0;
                dataLine.write(sampleBuffer, 0, bufferSize);
            }

                  
            // each sample taken represents 1/44100 seconds (22675.737 ns)
        }
    }
    
    
    public void test() {
        double frequency = 880;
        for(int i = 0; i < sampleBuffer.length; i++) {
            timeCounter++;
            sampleBuffer[i] = (byte)(Math.sin(Math.PI*timeCounter*frequency/sampleRate)*127);

        }
        
    }
    

    
    public void write(int address, short value) {
        switch(address) {
            case 0x4000:
            case 0x4001:
            case 0x4002:
            case 0x4003:
                square1.write(address, value);
                break;
                
            case 0x4004:
            case 0x4005:
            case 0x4006:
            case 0x4007:
                square2.write(address, value);
                break;
                
            case 0x4008:
            case 0x4009:
            case 0x400A:
            case 0x400B:
                triangle.write(address, value);
                
                break;
            case 0x400C:
            case 0x400D:
            case 0x400E:
            case 0x400F:
                noise.write(address, value);
                
                break;
            case 0x4010:
            case 0x4011:
            case 0x4012:
            case 0x4013:
                dm.write(address, value);
                
                break;
                
            case 0x4015:
                // channel enable
                // initialize hardware if needed
                dm.write(address, value);
                break;
                
            case 0x4017:
                // frame counter control
                break;
        }
    }
    
}
