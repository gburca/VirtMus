/*
 * Copyright (C) 2006-2010  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

import com.ebixio.annotations.AnnotCanvas;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 *
 * @author GBURCA
 */
public abstract class DrawingTool implements MouseListener, MouseMotionListener {
//    protected Paint paint = Color.BLUE;
//    protected int lineThickness = 1;
//    protected float alpha = 0.75F;
    public AnnotCanvas canvas;

        // Dragging states
    public static enum Drag {
        UNKNOWN, DRAGGING, STILL, EXITED;
    }
    protected Drag dragState = Drag.UNKNOWN;
    protected Point dragStart;

    public DrawingTool(AnnotCanvas canvas) {
        this.canvas = canvas;
    }

    public void paint(Graphics2D g2d) { }

    public Point getAbsolutePoint(Point point) {
        return canvas.getAbsolutePoint(point);
    }
    public abstract String getName();

    @Override
    public String toString() {
        return getName();
    }

    public void mouseClicked(MouseEvent e) { }

    public void mousePressed(MouseEvent e) { }

    public void mouseReleased(MouseEvent e) { }

    public void mouseEntered(MouseEvent e) { }

    public void mouseExited(MouseEvent e) { }

    public void mouseDragged(MouseEvent e) { }

    public void mouseMoved(MouseEvent e) { }

//    public Paint getPaint() {
//        return paint;
//    }
//
//    public void setPaint(Paint paint) {
//        this.paint = paint;
//    }
//
//    public int getLineThickness() {
//        return lineThickness;
//    }
//
//    public void setLineThickness(int lineThickness) {
//        this.lineThickness = lineThickness;
//    }
//
//    public float getAlpha() {
//        return alpha;
//    }
//
//    public void setAlpha(float alpha) {
//        this.alpha = alpha;
//    }
}
