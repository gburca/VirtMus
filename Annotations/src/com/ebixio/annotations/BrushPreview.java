/*
 * BrushPreview.java
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
package com.ebixio.annotations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import javax.swing.JComponent;

public class BrushPreview extends JComponent {

    private int diam = 10;
    private Color color = Color.BLUE;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        this.repaint();
    }

    public int getDiam() {
        return diam;
    }

    public void setDiam(int diam) {
        this.diam = diam;
        this.repaint();
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public void paint(Graphics g) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        Point p = new Point(getWidth() / 2, getHeight() / 2);
        int half = diam / 2;
        g.setColor(color);
        g.fillOval(p.x - half, p.y - half, diam, diam);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(32, 32);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
}
