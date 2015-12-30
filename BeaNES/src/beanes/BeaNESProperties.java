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

import java.util.*;
import java.io.*;


public class BeaNESProperties extends Properties {
    
    public static String PROPERTY_ROMPATH = "romPath";
    public static String PROPERTY_FULLSCREEN_RESOLUTION_MODE = "fullscreenResolutionMode";
    public static String PROPERTY_SHOW_FPS = "showFPS";
    public static String PROPERTY_THROTTLE = "throttle";
    public static String PROPERTY_CONTROLLER1 = "controller1";
    public static String PROPERTY_CONTROLLER2 = "controller2";
    
    public BeaNESProperties() {
        findRootPath();
        load();
        setProperty("propertiesFile", getProperty("rootPath") +"/properties.conf");
    }
    
    /**
     * Saves properties to preferences.conf (path from where all class files are stored).
     */
    public void store() {
        try {
            String path = getProperty("rootPath") + "/properties.conf";
            FileOutputStream out = new FileOutputStream(path);
            
            store(out, "BeaNES Settings");
            
            out.close();
        } catch(FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        }catch(IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    /**
     * Loads properties from preferences.conf (path from where all class files are stored).
     */
    public void load() {
        try {
            FileInputStream in = new FileInputStream(getProperty("rootPath") + "/properties.conf");
            load(in);
            in.close();
            
        }catch(FileNotFoundException e) {
            // OK for this to occur (e.g. if you are running for the first time)
        }catch(IOException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public boolean propertiesFileExist() {
        File f = new File(getProperty("rootPath") + "/properties.conf");
        
        return f.exists();
    }
    
    /**
     * Gets path where all class files are stored.
     */
    private void findRootPath() {
        String classPath = System.getProperty("java.class.path");
        String path = "";
        
        String pathSeparator = System.getProperty("path.separator");
        StringTokenizer tokenizer = new StringTokenizer(classPath, pathSeparator);
        
        if(tokenizer.hasMoreElements())
            path = (String)(tokenizer.nextElement());
        else
            path = classPath;
        
        File f = new File(path);
        
        try {
            
            setProperty("rootPath", f.getParent());
        } catch(NullPointerException e) {
            System.out.println("occurs because executing in same path" + path);
        }
    }
    
}

