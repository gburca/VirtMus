/*
 * AnnotCanvas.java
 *
 * Copyright (C) 2006-2007  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

package com.ebixio.annotations;

import com.ebixio.virtmus.shapes.*;
import com.ebixio.jai.ImageDisplay;
import com.ebixio.virtmus.MusicPage;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.beans.*;
import java.io.Serializable;

/**
 * @author gburca
 */
public class AnnotCanvas extends ImageDisplay implements Serializable, MouseListener, MouseMotionListener {
    
    // Dragging states
    public static enum Drag {
        UNKNOWN, DRAGGING, STILL, EXITED;
    }

    private Drag dragState = Drag.UNKNOWN;
    private Point dragStart;
    
    private Paint paint = Color.BLUE;
    private int diam = 10;
    private float alpha = 1.0F;
    private float scale = 1.0F;
    
    private MusicPage musicPage = null;
    private ShapeLine line = null;

    public AnnotCanvas() {
        addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(Color.BLACK);
    }
    
    // <editor-fold defaultstate="collapsed" desc=" Setters and Getters ">
    public void setMusicPage(MusicPage musicPage) {
        this.musicPage = musicPage;
    }
    
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
    
    public void setScale(float scale) {
        this.scale = scale;
    }
    
    public void setDiam(int val) {
        this.diam = val;
    }
    public int getDiam() {
        return diam;
    }
    
    public void setPaint(Paint c) {
        this.paint = c;
    }
    public Paint getPaint() {
        return paint;
    }
    
    public void setOrigin(Point p) {
        super.setOrigin(p.x, p.y);
    }
    public Point getOrigin() {
        return new Point(super.getXOrigin(), super.getYOrigin());
    }
    // </editor-fold>

    public Color getColor() {
        if (paint instanceof Color) {
            return (Color) paint;
        } else {
            return Color.BLACK;
        }
    }
    
    public void clear() {
        if (musicPage != null) {
            musicPage.clearAnnotations();
            repaint();
        }
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;

        AffineTransform newXform, origXform;
        newXform = origXform = g2d.getTransform();
        Point orig = getOrigin();
        newXform.concatenate( AffineTransform.getTranslateInstance(-orig.x, -orig.y) );
        newXform.concatenate( AffineTransform.getScaleInstance(scale, scale) );
        g2d.setTransform( newXform );
        
        if (musicPage != null) {
            musicPage.paintAnnotations(g2d);
        }
        if (line != null) {
            line.paint(g2d);
        }

        g2d.setTransform(origXform);
    }
    
    
    /** This function will convert the coordinates of a mouse click (with (0,0) being
     * the upper-left corner of the canvas component) into coordinates with respect
     * to the raw image file. The image could be scaled and translated.
     */
    private Point getAbsolutePoint(Point point) {
        Point origin = getOrigin();
        Point p = new Point(point);
        p.translate(origin.x, origin.y);
        p.move(Math.round(p.x/scale), Math.round(p.y/scale));
        return p;
    }
    
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point p = e.getPoint();

            //System.out.println("Origin: " + getOrigin() + " pt: " + p + " abs: " + getAbsolutePoint(p) + " scale: " + scale);
            
            musicPage.addAnnotation(new ShapePoint(paint, alpha, Math.round(diam / scale), getAbsolutePoint(e.getPoint())));
            repaint();
        }
    }
    
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            dragState = Drag.DRAGGING;
            dragStart = e.getPoint();
        }
    }
    
    public void mouseReleased(MouseEvent e) {
        dragState = Drag.STILL;
        if (line != null) {
            musicPage.addAnnotation(line);
            line = null;
            repaint();
        }
    }
    
    public void mouseEntered(MouseEvent e) {
        if (dragState == Drag.EXITED) {
            dragState = Drag.DRAGGING;
        }
    }
    
    public void mouseExited(MouseEvent e) {
        if (dragState == Drag.DRAGGING) {
            dragState = Drag.EXITED;
        } else {
            dragState = Drag.STILL;
        }
    }
    
    public void mouseDragged(MouseEvent e) {
        if (dragState == Drag.DRAGGING) {
            if (line == null) {
                line = new ShapeLine(paint, alpha, Math.round(diam/scale), getAbsolutePoint(dragStart));
            } else {
                line.addEnd(getAbsolutePoint(e.getPoint()));
                repaint();
            }
        }        
    }
    
    public void mouseMoved(MouseEvent e) {
    }
}
