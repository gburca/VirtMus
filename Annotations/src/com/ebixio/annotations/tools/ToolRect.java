/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
