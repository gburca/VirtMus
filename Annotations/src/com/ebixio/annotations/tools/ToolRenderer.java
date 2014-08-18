/*
 * Copyright (C) 2006-2014  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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
package com.ebixio.annotations.tools;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class ToolRenderer extends Canvas implements ListCellRenderer {

    DrawingTool t;

    public ToolRenderer() {
        setSize(48, 48);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.setBackground(Color.yellow);
        g2d.setColor(Color.red);
        Shape s = null;
        if (t instanceof ToolRect) {
            s = new Rectangle(40, 20);
        } else if (t.getClass() == ToolLine.class) {
            GeneralPath p = new GeneralPath();
            p.moveTo(40, 8);
            p.lineTo(8, 40);
            s = p;
        } else if (t.getClass() == ToolFreehand.class) {
            GeneralPath p = new GeneralPath();
            p.moveTo(40, 8);
            p.lineTo(30, 30);
            p.lineTo(20, 15);
            p.lineTo(8, 40);
            s = p;
        }

        if (s != null) {
            g2d.draw(s);
        }
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        t = (DrawingTool)value;

        return this;
    }
}
