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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.*;


public class JoypadInput extends KeyAdapter {
    
    private BeaNES nes;
    
    public static final int BUTTON_A = 0;
    public static final int BUTTON_B = 1;
    public static final int BUTTON_SELECT = 2;
    public static final int BUTTON_START = 3;
    public static final int BUTTON_UP = 4;
    public static final int BUTTON_DOWN = 5;
    public static final int BUTTON_LEFT = 6;
    public static final int BUTTON_RIGHT = 7;
    
    public static final int BUTTON_UNBOUNDED = -1;
    
    private int[] keyboardMap = new int[255];
    private int[] buttonStates = {0,0,0,0,0,0,0,0};
    
    private int num;
    
    public JoypadInput(BeaNES nes, int num) {
        this.num = num;
        this.nes = nes;
        
        unbindKeys();
        if(nes.getProperties().propertiesFileExist()) {
            for(int button = 0; button < 8; button++) {
                String key = "controls["+num+"]["+button+"]";
                String value = nes.getProperties().getProperty(key);
                if(value != null) {
                    int code = Integer.parseInt(value);
                    bindKey(code, button);
                }
            }
        }
        
        // hardcode buttons
        else {
            if(num == 0) {
                bindKey(KeyEvent.VK_UP, BUTTON_UP);
                bindKey(KeyEvent.VK_DOWN, BUTTON_DOWN);
                bindKey(KeyEvent.VK_LEFT, BUTTON_LEFT);
                bindKey(KeyEvent.VK_RIGHT, BUTTON_RIGHT);
                bindKey(KeyEvent.VK_Z, BUTTON_SELECT);
                bindKey(KeyEvent.VK_X, BUTTON_START);
                bindKey(KeyEvent.VK_C, BUTTON_B);
                bindKey(KeyEvent.VK_V, BUTTON_A);
            } else if(num == 1) {
                bindKey(KeyEvent.VK_W, BUTTON_UP);
                bindKey(KeyEvent.VK_S, BUTTON_DOWN);
                bindKey(KeyEvent.VK_A, BUTTON_LEFT);
                bindKey(KeyEvent.VK_D, BUTTON_RIGHT);
                bindKey(KeyEvent.VK_H, BUTTON_SELECT);
                bindKey(KeyEvent.VK_J, BUTTON_START);
                bindKey(KeyEvent.VK_K, BUTTON_B);
                bindKey(KeyEvent.VK_L, BUTTON_A);
            }        
        }
    }
    
    public void bindKey(int key, int button) {
        if(key >= 0 && key < keyboardMap.length)
            keyboardMap[key] = button;
    }
    
    public void unbindButton(int button) {
        for(int i = 0; i < keyboardMap.length; i++)
            if(keyboardMap[i] == button)
                keyboardMap[i] = BUTTON_UNBOUNDED;
    }
    
    public void unbindKey(int key) {
        keyboardMap[key] = BUTTON_UNBOUNDED;
    }
    
    public void unbindKeys() {
        for(int i = 0; i < keyboardMap.length; i++)
            unbindKey(i);
    }
    
    public int getBoundedButton(int key) {
        return keyboardMap[key];
    }
    
    public int getBoundedKey(int button) {
        for(int i = 0; i < keyboardMap.length; i++)
            if(keyboardMap[i] == button)
                return i;
        
        return BUTTON_UNBOUNDED;
    }
    
    
    public int getButtonState(int button) {
        if(button < buttonStates.length && button >= 0)
            return buttonStates[button];
        
        return 0;
    }
    
    
    public synchronized void keyPressed(KeyEvent e) {
        int button = keyboardMap[e.getKeyCode()];
        
        if(button < buttonStates.length && button >= 0)
            buttonStates[button] = 1;
        
    }
    
    
    public synchronized void keyReleased(KeyEvent e) {
        int button = keyboardMap[e.getKeyCode()];
        
        if(button < buttonStates.length && button >= 0)
            buttonStates[button] = 0;
        
    }
    
}
