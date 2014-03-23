/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        //throw new UnsupportedOperationException("Not supported yet.");
        t = (DrawingTool)value;

        return this;
    }
}
