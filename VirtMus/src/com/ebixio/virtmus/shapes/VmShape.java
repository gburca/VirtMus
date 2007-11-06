/*
 * Shape.java
 * 
 * Created on Oct 16, 2007, 6:19:35 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus.shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;

/**
 *
 * @author gburca
 */
public abstract class VmShape {
    protected Paint paint = Color.BLUE;
    protected int lineThickness = 1;
    protected float alpha = 0.75F;

    public VmShape(Paint color, float alpha, int thickness) {
        paint = color;
        this.alpha = alpha;
        lineThickness = thickness;
    }
    
    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
    
    public abstract void paint(Graphics2D g);
    
}
