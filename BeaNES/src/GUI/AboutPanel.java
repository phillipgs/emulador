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

import javax.swing.*;
import beanes.*;
public class AboutPanel {
    
    public AboutPanel() {
        String message = BeaNES.PROGRAM_STRING + "\n\nCreated by Don Honerbrink and Chris Frericks. \n\nBugs or questions?\nEmail us at beanes-general@lists.sourceforge.net\n or visit http://beanes.sourceforge.net\n\n";
        JOptionPane.showMessageDialog(null, message, "About", JOptionPane.INFORMATION_MESSAGE); 
    }
    
}
