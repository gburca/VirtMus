/*
 * ShapeLine.java
 * 
 * Created on Oct 16, 2007, 6:44:37 PM
 * 
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
import java.awt.geom.GeneralPath;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class ShapeLine extends VmShape {
    protected GeneralPath path = new GeneralPath();
    
    public ShapeLine(Paint color, float alpha, int thickness, Point start) {
        super(color, alpha, thickness);
        path.moveTo(start.x, start.y);
    }
    
    public void addEnd(Point end) {
        path.lineTo(end.x, end.y);
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
        
        g.draw(path);
        g.setComposite(origComposite);
    }
    
}
