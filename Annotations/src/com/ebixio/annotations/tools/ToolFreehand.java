/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * @author GBURCA
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
