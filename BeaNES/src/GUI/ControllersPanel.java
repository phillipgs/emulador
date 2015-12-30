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

package GUI;
import beanes.mappers.NESMapper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import beanes.*;

public class ControllersPanel extends JFrame {
    BeaNES nes;
    
    JButton saveButton = new JButton("Save");
    JButton cancelButton = new JButton("Cancel");
    JTextField[][] inputs = new JTextField[2][8];
    int[][] buttonMap = new int[2][8];
    
    public ControllersPanel(BeaNES nes) {
        this.nes = nes;
        setTitle("Configure Controls");
        setLayout(new BorderLayout());
        
        JTabbedPane controlPanels = new JTabbedPane();
        JPanel buttonPanel = new JPanel();
        JPanel cPanel1 = buildControllerPanel(0);
        JPanel cPanel2 = buildControllerPanel(1);
        
        controlPanels.addTab("Controller 1", cPanel1);
        controlPanels.addTab("Controller 2", cPanel2);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // i know there is a better way to do this. This will be changed later. It is just for testing

                for(int cnum = 0; cnum <= 1; cnum++) {
                    getNES().getJoypadInput(cnum).unbindKeys();
                
                    for(int button = 0; button < buttonMap[cnum].length; button++) {
                        if(buttonMap[cnum][button] != JoypadInput.BUTTON_UNBOUNDED)
                            getNES().getJoypadInput(cnum).bindKey(buttonMap[cnum][button], button);
                            String key = "controls["+cnum+"]["+button+"]";
                            String value = String.valueOf(buttonMap[cnum][button]);
                            getNES().getProperties().setProperty(key, value);
                    }
                }
                
                dispose();
            }
        });
        
        
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        add(controlPanels, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        setSize(400, 460);
        setVisible(true);
    }
    
    private BeaNES getNES() {
        return nes;
    }
    
    
    
    public JPanel buildControllerPanel(final int num) {
        JoypadInput joypad = nes.getJoypadInput(num);
        
        JPanel panel = new JPanel();
        JPanel inputContainer = new JPanel();
        JPanel inputPanel = new JPanel();
        NESControllerPanel cPanel = new NESControllerPanel();
        
        inputPanel.setLayout(new GridLayout(8, 2));
        

        JLabel labelUp = new JLabel("Up");
        JLabel labelDown = new JLabel("Down");
        JLabel labelLeft = new JLabel("Left");
        JLabel labelRight = new JLabel("Right");
        JLabel labelSelect = new JLabel("Select");
        JLabel labelStart = new JLabel("Start");
        JLabel labelB = new JLabel("B");
        JLabel labelA = new JLabel("A");
        
        // initialize all the text fields
        for(int button = 0; button < inputs[num].length; button++) {
            final int fbutton = button;    

            // initialize text input fields for up, down, left, right, b, a, select, and start
            buttonMap[num][button] = joypad.getBoundedKey(button);
            
            String inputText = (joypad.getBoundedKey(button) == JoypadInput.BUTTON_UNBOUNDED)?"":KeyEvent.getKeyText(joypad.getBoundedKey(button));
            inputs[num][button] = new JTextField(10);
            inputs[num][button].setText(inputText);
            inputs[num][button].setEditable(false);
            
            inputs[num][button].addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    inputs[num][fbutton].setText(KeyEvent.getKeyText(e.getKeyCode()));
                    
                    for(int i = 0; i < inputs.length; i++) {
                        for(int j = 0; j < inputs[i].length; j++) {
                            if(buttonMap[i][j] == e.getKeyCode() && !(num == i && fbutton == j)) {
                                buttonMap[i][j] = JoypadInput.BUTTON_UNBOUNDED;
                                inputs[i][j].setText("");
                            }
                        }
                    }
                                
                    buttonMap[num][fbutton] = e.getKeyCode();
                }
            });
            

        }

        
        inputPanel.add(labelUp);
        inputPanel.add(inputs[num][JoypadInput.BUTTON_UP]);
        inputPanel.add(labelDown);
        inputPanel.add(inputs[num][JoypadInput.BUTTON_DOWN]);
        inputPanel.add(labelLeft);
        inputPanel.add(inputs[num][JoypadInput.BUTTON_LEFT]);
        inputPanel.add(labelRight);
        inputPanel.add(inputs[num][JoypadInput.BUTTON_RIGHT]);
        inputPanel.add(labelSelect);
        inputPanel.add(inputs[num][JoypadInput.BUTTON_SELECT]);
        inputPanel.add(labelStart);
        inputPanel.add(inputs[num][JoypadInput.BUTTON_START]);
        inputPanel.add(labelB);
        inputPanel.add(inputs[num][JoypadInput.BUTTON_B]);
        inputPanel.add(labelA);
        inputPanel.add(inputs[num][JoypadInput.BUTTON_A]);
        inputContainer.add(inputPanel);
        
        panel.add(inputContainer, BorderLayout.CENTER);
        panel.add(cPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JTextField getTextInputs(int num, int button) {
        return inputs[num][button];
    }
    
    
    private class NESControllerPanel extends JPanel {
        
        private Image img;
        
        public NESControllerPanel() {
            ClassLoader cl = this.getClass().getClassLoader();
            ImageIcon icon = new ImageIcon(cl.getResource("images/NESController.jpg"));
            img = icon.getImage();
            
            Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setSize(size);
            setLayout(null);
        }
        
        public void paintComponent(Graphics g) {
            g.drawImage(img, 0, 0, null);
        }
        
    }
    
    
    
}
