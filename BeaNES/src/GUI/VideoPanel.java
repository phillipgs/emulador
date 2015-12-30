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

import beanes.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;


public class VideoPanel extends JFrame {
    
    private BeaNES nes;
    private Vector<String> dmCols = new Vector<String>();
    private Vector<Vector> dmRows = new Vector<Vector>();
    private JTable dmTable;
    private JScrollPane dmScroll;
    private JButton saveButton = new JButton("save");
    private DisplayMode[] dms;
    
    
    public VideoPanel(BeaNES nes) {
        super();
        this.nes = nes;
        // build the table column headers
        dmCols.add("Width");
        dmCols.add("Height");
        dmCols.add("Bit Depth");
        dmCols.add("Refresh Rate");
        
        
        // fetch and build a table of all the graphics device screen modes
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        
        GraphicsDevice[] devices = ge.getScreenDevices();
        
        dms = devices[0].getDisplayModes();
        for(int k = 0; k < dms.length; k++) {
            
            Vector<Integer> row = new Vector<Integer>();
            
            row.add(dms[k].getWidth());
            row.add(dms[k].getHeight());
            row.add(dms[k].getBitDepth());
            row.add(dms[k].getRefreshRate());
            
            dmRows.add(row);
        }

        // fetch from preferences to determine which mode to use on default
        int selectedRow = 0;
        
        try {
            selectedRow = Integer.valueOf(BeaNES.getProperties().getProperty(BeaNESProperties.PROPERTY_FULLSCREEN_RESOLUTION_MODE));
        } catch(NumberFormatException e) {}
      
        
        // create the able
        dmTable = new JTable(dmRows, dmCols);
        dmScroll = new JScrollPane(dmTable);
        
        // select the default/preferred display mode
        dmTable.setRowSelectionInterval(selectedRow, selectedRow);
        
        // update/save the selected display mode
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getNES().setFullScreenDisplayMode(dms[getDMTable().getSelectedRow()]);
                
                BeaNES.getProperties().setProperty(BeaNESProperties.PROPERTY_FULLSCREEN_RESOLUTION_MODE, 
                        Integer.toString(getDMTable().getSelectedRow()));
                setVisible(false);
            }
        });
        
        
        // build GUI
        add(dmScroll);
        
        setLayout(new BorderLayout());
        
        JPanel southPanel = new JPanel();
        
        southPanel.add(saveButton);
        add(southPanel, BorderLayout.SOUTH);
        
        add(dmScroll, BorderLayout.CENTER);
        
        setSize(new Dimension(500, 300));
        setMinimumSize(new Dimension(500, 300));
        
        setVisible(true);
    }
    
    
    private BeaNES getNES() {
        return nes;
    }
    
    
    private JTable getDMTable() {
        return dmTable;
    }
}
