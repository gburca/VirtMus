/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus.shapes;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;

/**
 *
 * @author GBURCA
 */
public class ShapeRect extends VmShape {
    Point start, end;
    
    public ShapeRect(Paint color, float alpha, int thickness, Point start) {
        super(color, alpha, thickness);
        this.start = start;
    }
    
    public void addEnd(Point end) {
        this.end = end;
    }
    
    @Override
    public void paint(Graphics2D g) {
        Composite origComposite = g.getComposite();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(paint);
        g.setStroke(new BasicStroke(this.lineThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (this.alpha < 1.0) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        Point p1 = new Point(Math.min(start.x, end.x), Math.min(start.y, end.y));
        Point p2 = new Point(Math.max(start.x, end.x), Math.max(start.y, end.y));
        g.fillRoundRect(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y, lineThickness, lineThickness);
        //g.drawRoundRect(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y, lineThickness, lineThickness);

        g.setComposite(origComposite);

    }
}
