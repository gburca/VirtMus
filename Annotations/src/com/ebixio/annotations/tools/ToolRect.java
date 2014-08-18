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

import com.ebixio.annotations.AnnotCanvas;
import com.ebixio.virtmus.shapes.ShapeRect;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class ToolRect extends DrawingTool {
    private ShapeRect rect = null;

    public ToolRect(AnnotCanvas canvas) {
        super(canvas);
    }

    @Override
    public String getName() {
        return "Rectangle";
    }

    @Override
    public void paint(Graphics2D g2d) {
        if (rect != null) {
            rect.paint(g2d);
        }
    }

    private void updateRect(Point endPt) {
        if (rect == null) {
            rect = new ShapeRect(canvas.paint, canvas.alpha, Math.round(canvas.diam/canvas.scale), dragStart);
        }
        rect.addEnd(endPt);
        canvas.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point p = getAbsolutePoint(e.getPoint());
            if (canvas.imgBounds.contains(p)) {
                dragState = Drag.DRAGGING;
                dragStart = p;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragState == Drag.DRAGGING) {
            Point p = getAbsolutePoint(e.getPoint());
            if (canvas.imgBounds.contains(p)) {
                updateRect(p);
            }
            if (rect != null) {
                canvas.addAnnotation(rect, getName());
                rect = null;
            }
            dragState = Drag.STILL;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = getAbsolutePoint(e.getPoint());
        if (canvas.imgBounds.contains(p)) {
            if (dragState == Drag.DRAGGING) {
                updateRect(p);
            } else {
                dragState = Drag.DRAGGING;
                dragStart = p;
            }
        }
    }

}
