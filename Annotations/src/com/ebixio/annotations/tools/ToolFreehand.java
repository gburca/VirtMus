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
import com.ebixio.virtmus.shapes.ShapeLine;
import com.ebixio.virtmus.shapes.ShapePoint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class ToolFreehand extends DrawingTool {
    private ShapeLine line = null;

    public ToolFreehand(AnnotCanvas canvas) {
        super(canvas);
    }

    @Override
    public String getName() {
        return "Freehand";
    }

    @Override
    public void paint(Graphics2D g2d) {
        if (line != null) {
            line.paint(g2d);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point p = getAbsolutePoint(e.getPoint());

            if (canvas.musicPage != null) {
                canvas.musicPage.addAnnotation(new ShapePoint(canvas.paint, canvas.alpha, Math.round(canvas.diam / canvas.scale), p));
            }
            canvas.repaint();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            dragState = Drag.DRAGGING;
            dragStart = e.getPoint();

            if (canvas.imgBounds.contains(getAbsolutePoint(dragStart))) {
                line = new ShapeLine(canvas.paint, canvas.alpha, Math.round(canvas.diam/canvas.scale), getAbsolutePoint(dragStart));
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragState = Drag.STILL;
        if (line != null) {
            canvas.addAnnotation(line, getName());
            line = null;
            canvas.repaint();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (dragState == Drag.EXITED) {
            dragState = Drag.DRAGGING;
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (dragState == Drag.DRAGGING) {
            dragState = Drag.EXITED;
        } else {
            dragState = Drag.STILL;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragState == Drag.DRAGGING) {
            if (canvas.musicPage == null) return;

            Point p = getAbsolutePoint(e.getPoint());
            if (line == null) {
                if (!canvas.imgBounds.contains(p)) return;
                line = new ShapeLine(canvas.paint, canvas.alpha, Math.round(canvas.diam/canvas.scale), p);
            } else {
                if (!canvas.imgBounds.contains(p)) {
                    canvas.musicPage.addAnnotation(line);
                    line = null;
                } else {
                    line.addEnd(getAbsolutePoint(e.getPoint()));
                }
                canvas.repaint();
            }
        }
    }
}
