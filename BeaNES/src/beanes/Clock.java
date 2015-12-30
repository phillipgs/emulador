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


public class Clock implements Runnable {
    
    private BeaNES nes;
    private Thread thread;

    private long currVBlankTime;
    private long lastVBlankTime;

    
    private boolean paused = false;
    private boolean throttle = true;
    private boolean running = false;
    
    public Clock(BeaNES nes) {
        this.nes = nes;
    }
    
    public void start() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }
    
    public void stop() {
        running = false;
    }
    
    public boolean isRunning() {
        return (thread == null)?false:true;
    }
    
    public synchronized void run() {

        long cpuNewCycles = 0;
        long ppuNewCycles = 341;
        
        while(running) {
            while(paused) {}
            while(ppuNewCycles <= cpuNewCycles*3) {
                ppuNewCycles += nes.getPPU().runCycles(24);
            }

            while(cpuNewCycles <= ppuNewCycles/3) {
                cpuNewCycles += nes.getCPU().processNextInstruction();
            }

        }
        
        thread = null;
    }
    
    public boolean isThrottle() {
        return throttle;
    }
    
    public void setThrottle(boolean value) {
        throttle = value;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public void setPaused(boolean value) {
        paused = value;
    }
    
    public void signalVBlank() {
        if(throttle) {
        do {
            currVBlankTime = System.nanoTime();
        } while(currVBlankTime - lastVBlankTime < 16666667);
        lastVBlankTime = currVBlankTime;;
        }   
    }
}
