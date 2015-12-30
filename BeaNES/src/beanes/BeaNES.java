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

import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;


public class BeaNES{
    
    public final static String PROGRAM_NAME     = "BeaNES";
    public final static String PROGRAM_VERSION  = "0.0.1";
    public final static String PROGRAM_STRING   = PROGRAM_NAME + "-" + PROGRAM_VERSION;
    
    private Clock clock;
    private VideoOutput video;
    private JoypadInput[] joypads = new JoypadInput[2];
    private DisplayMode fullScreenDisplayMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
    private PPU ppu;
    private PAPU papu;
    private ROM rom;
    private GUI gui;
    private CPU cpu;
    private static BeaNESProperties properties;
    
    public BeaNES(GUI g) {
        gui = g;
        
        //initialize nes hardware
        
        properties  = new BeaNESProperties();
        clock       = new Clock(this);
        cpu         = new CPU(this);
        ppu         = new PPU(this);
        video       = new VideoOutput(this);
        papu        = new PAPU(this);
        
        // initialize joypads
        for(int i = 0 ;i < joypads.length; i++)
            joypads[i] = new JoypadInput(this, i);  
    }

    
    public void start() {
        hardReset();
    }
    
    
    public void stop() {
        clock.stop();
    }
    
    
    public CPU getCPU() {
        return cpu;
    }
    
    
    public PPU getPPU() {
        return ppu;
    }
    
    
    public PAPU getPAPU() {
        return papu;
    }
    
    
    
    public void hardReset() {
        stop();
        cpu.hardReset();
        ppu.hardReset();
        clock.start();
    }
    
    
    public void reset() {
        cpu.reset();
    }
     
    
    public Clock getClock() {
        return clock;
    }


    public VideoOutput getVideoOutput() {
        return video;
    }

    
    public JoypadInput getJoypadInput(int num) {
        return joypads[num];
    }
    
    
    public GUI getGUI() {
        return gui;
    }
    
    
    public void loadROM(ROM rom) {
        this.rom = rom;
        rom.load();
        rom.getMapper().loadROM(rom);
    }
    
    
    public ROM getROM() {
        return rom;
    }

    
    public MemoryMapper getMapper() {
        return rom.getMapper();
    }

    public DisplayMode getFullScreenDisplayMode() {
        return fullScreenDisplayMode;
    }
    
    public void setFullScreenDisplayMode(DisplayMode dm) {
        fullScreenDisplayMode = dm;
    }
    
    
    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.start();
    }
    
    public static BeaNESProperties getProperties() {
        return properties;
    }
}
