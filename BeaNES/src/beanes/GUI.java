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
import java.awt.event.*;
import java.io.*;
import GUI.*;


public class GUI extends JFrame {
    
    private BeaNES nes;
    final JMenuBar menuBar = new JMenuBar();
    private boolean fullScreen = false;
    
    public GUI() {
        nes = new BeaNES(this);
        createMenuBar();
        setTitle(BeaNES.PROGRAM_STRING);
        
        add(nes.getVideoOutput());
        nes.getVideoOutput().initializeImage();
        addKeyListener(nes.getJoypadInput(0));
        addKeyListener(nes.getJoypadInput(1));
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                nes.getProperties().store();
            }
        });        
        
        pack();
        
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == e.VK_ESCAPE) {
                    setFullScreen(!isFullScreen());
                }
            }
        });
        
    }
    
    
    public void start() {
        setVisible(true);
    }
    
    
    
    private void createMenuBar() {
        
        JMenu nesMenu = new JMenu("NES");
        JMenu optionsMenu = new JMenu("Options");
        JMenu helpMenu = new JMenu("Help");
        
        JMenuItem loadROMMenuItem = new JMenuItem("Load ROM");
        JMenuItem hardResetMenuItem = new JMenuItem("Hard Reset");
        JMenuItem videoMenuItem = new JMenuItem("Video Settings");
        JMenuItem controlsMenuItem = new JMenuItem("Controller Settings");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        final JCheckBoxMenuItem fullScreen = new JCheckBoxMenuItem("Full Screen");
        final JCheckBoxMenuItem throttle = new JCheckBoxMenuItem("Throttle CPU");
        final JCheckBoxMenuItem showFPS = new JCheckBoxMenuItem("Show FPS");
        JMenuItem aboutMenuItem = new JMenuItem("About Authors /Contact Authors");
        
        
        // build menu
        nesMenu.add(loadROMMenuItem);
        nesMenu.add(exitMenuItem);
        optionsMenu.add(videoMenuItem);
        optionsMenu.add(controlsMenuItem);
        optionsMenu.add(throttle);
        optionsMenu.add(fullScreen);
        optionsMenu.add(showFPS);
        helpMenu.add(aboutMenuItem);
        menuBar.add(nesMenu);
        menuBar.add(optionsMenu);
        menuBar.add(helpMenu);
        
        throttle.setSelected(nes.getClock().isThrottle());

        // pause system whenever there is a mouse click
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                togglePause();
                
            }
        });
        
        // build action listeners for menu
        videoMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                VideoPanel vp = new VideoPanel(nes);
            }
        });

        // build action listeners for menu
        controlsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ControllersPanel cp = new ControllersPanel(nes);
            }
        });        
        
        loadROMMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadROM();
                
            }
        });
                
        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(1);
            }
        });
                
        throttle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                boolean value = !nes.getClock().isThrottle();
                
                nes.getClock().setThrottle(value);
                throttle.setSelected(value);
                BeaNES.getProperties().setProperty(BeaNESProperties.PROPERTY_THROTTLE, Boolean.toString(throttle.isSelected()));
                
            }
        });
        
        fullScreen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                boolean value = !isFullScreen();
                setFullScreen(value);
                fullScreen.setSelected(value);
                
                nes.getClock().setThrottle(value);
                throttle.setSelected(value);
                
            }
        });
        
        showFPS.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nes.getVideoOutput().setShowFPS(!nes.getVideoOutput().isShowFPS());
                showFPS.setSelected(nes.getVideoOutput().isShowFPS());
                BeaNES.getProperties().setProperty(BeaNESProperties.PROPERTY_SHOW_FPS, Boolean.toString(showFPS.isSelected()));
            }
        });
        aboutMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AboutPanel about = new AboutPanel();
            }
        });
        
        try {
            throttle.setSelected(nes.getVideoOutput().isShowFPS());
            
            boolean selected = Boolean.valueOf(BeaNES.getProperties().getProperty(BeaNESProperties.PROPERTY_THROTTLE));
            nes.getClock().setThrottle(selected);
            throttle.setSelected(selected);
            
        } catch(NumberFormatException e) { }
        
        
        try {
            showFPS.setSelected(nes.getVideoOutput().isShowFPS());
            
            boolean selected = Boolean.valueOf(BeaNES.getProperties().getProperty(BeaNESProperties.PROPERTY_SHOW_FPS));
            nes.getVideoOutput().setShowFPS(selected);
            showFPS.setSelected(selected);
            
        } catch(NumberFormatException e) { }
        
        
        setJMenuBar(menuBar);
    }
    
    
    private void loadROM() {
        String currDir = nes.getProperties().getProperty(BeaNESProperties.PROPERTY_ROMPATH);
        JFileChooser chooser = new JFileChooser(currDir);
        
        chooser.setDialogTitle("Select ROM");
        chooser.showOpenDialog(this);
        
        File romFile = chooser.getSelectedFile();
        
        if(romFile != null) {
            nes.stop();
            
            // save the rom path you were last in
            nes.getProperties().setProperty(BeaNESProperties.PROPERTY_ROMPATH, romFile.getAbsolutePath());
            nes.loadROM(new ROM(nes, romFile));
            
            // we assume you would automatically like to play after you load the rom
            nes.start();
            menuBar.setVisible(nes.getClock().isPaused());
        }
    }
    
    
    public void togglePause() {
        nes.getClock().setPaused(!nes.getClock().isPaused());
        menuBar.setVisible(nes.getClock().isPaused());
    }
    
    
    public BeaNES getNES() {
        return nes;
    }
    
    
    public boolean isFullScreen() {
        return fullScreen;
    }
    
    public void setFullScreen(boolean fullScreen) {
        // get all the graphics information
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        
        // enter fullscreen mode
        if(fullScreen) {
            setVisible(false);
            dispose();
            // hide frame/titlebars
            this.setUndecorated(true);
            this.setDefaultLookAndFeelDecorated(false);
            setVisible(true);
            
            // enter full screen mode
            gd.setFullScreenWindow(this);
            gd.setDisplayMode(nes.getFullScreenDisplayMode());
            
            this.fullScreen = fullScreen;
            
            // unpause game if it is paused
            if(nes.getClock().isPaused()) togglePause();
            
        } 
        // exit fullscreen mode
        else {
            this.fullScreen = false;
            setVisible(false);
            dispose();
            this.setUndecorated(false);
            this.setDefaultLookAndFeelDecorated(true);
            setVisible(true);
            gd.setFullScreenWindow(null);
        }
    }
}
