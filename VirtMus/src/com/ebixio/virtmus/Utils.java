/*
 * Utils.java
 *
 * Copyright (C) 2006-2007  Gabriel Burca (gburca dash virtmus at ebixio dot com)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.ebixio.virtmus;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.Vector;

/**
 *
 * @author gburca
 */
public class Utils {
    
    /** Creates a new instance of Utils */
    public Utils() {
    }

    static Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
    static Dimension getScreenSize(int screen) {
        return getScreenSizes()[screen];
    }

    static Dimension[] getScreenSizes() {
        Vector<Dimension> sizes = new Vector<Dimension>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        
        for (int i = 0; i < gs.length; i++) {
            DisplayMode dm = gs[i].getDisplayMode();
            sizes.add(new Dimension(dm.getWidth(), dm.getHeight()));
        }
        
        return (Dimension[]) sizes.toArray();
    }

    static int getNumberOfScreens() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            GraphicsDevice[] gs = ge.getScreenDevices();
            return gs.length;
        } catch (HeadlessException e) {
            // Thrown if there are no screen devices
            return 0;
        }
    }
    
    static double scaleProportional(Rectangle container, Rectangle item) {
        double scaleX = (double)container.width / (double)item.width;
        double scaleY = (double)container.height / (double)item.height;
        return Math.min(scaleX, scaleY);
    }
    
    static Rectangle shrinkToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        if (scale > 1) {
            return item;
        } else {
            return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
        }
    }
    
    static Rectangle stretchToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        if (scale < 1) {
            return item;
        } else {
            return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
        }
    }
    
    static Rectangle scaleToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
    }
    
    static Point centerItem(Rectangle container, Rectangle item) {
        int x = (container.width / 2) - (item.width / 2);
        int y = (container.height / 2) - (item.height / 2);
        return new Point(x, y);
    }
}
