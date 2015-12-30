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
import java.awt.image.*;


public class VideoOutput extends JComponent {
    
    BeaNES nes;
    
    private int width = 256;
    private int height = 256;
    
    private BufferedImage img;
    private DataBufferInt dbi;
    private VolatileImage vimg;
    
    private Font font = new Font("sansserif", Font.PLAIN, 10);
    
    private int fpsCounter = 0;
    private long prevFrameTime = 0;
    private boolean showFPS = true;
    private String fps = "FPS: -";
    private int[] raster;
    
    
    public VideoOutput(BeaNES nes) {
        super();
        this.nes = nes;
        
        setPreferredSize(new Dimension(width,height));
    }
    
    
    public boolean isShowFPS() {
        return showFPS;
    }
    
    public void setShowFPS(boolean value) {
        showFPS = value;
    }
    
    public void initializeImage() {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        dbi = (DataBufferInt)(img.getRaster().getDataBuffer());
        raster = dbi.getData();
        
        nes.getPPU().setRaster(raster);
    }
    
    
    public void renderImage(int[] image) {
        raster = image;
        
        
        if(showFPS && --fpsCounter<=0){
            long ct = System.nanoTime()/1000;
            
            long frameT = (ct-prevFrameTime)/45;
            if(frameT == 0){
                fps = "FPS: -";
            }else{
                fps = "FPS: "+(1000000/frameT);
            }
            fpsCounter=45;
            prevFrameTime = ct;
            
        }
        
        invalidate();
        
        Graphics g = getGraphics();
        if(g != null) {
            update(g);
            g.dispose();
        }
    }
    
    public void update(Graphics g) {
        paint(g);
    }
    
    public void paint(Graphics g) {
        
        
        createBackBuffer();
        
        do {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            
            
            int valCode = vimg.validate(gc);
            
            
            if(valCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                createBackBuffer();
            }
            
            
            Graphics2D offscreenGraphics = (Graphics2D)vimg.getGraphics();
            
            
            offscreenGraphics.drawImage(img, 0, 0, getWidth(), getHeight(), null);
            offscreenGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            offscreenGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            offscreenGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            if(g != null && vimg != null) {
                g.drawImage(vimg, 0, 0, this);
                g.setFont(font);
                g.setColor(Color.WHITE);
                if(showFPS) g.drawString(fps, 0, 10);
            }
        } while(vimg.contentsLost());
        
    }
    
    public void createBackBuffer() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        
        if(getWidth() != 0 && getHeight() != 0) {
            vimg = gc.createCompatibleVolatileImage(getWidth(), getHeight());
        }
        else
            vimg = gc.createCompatibleVolatileImage(width, height);
    }
    
    
    
}
