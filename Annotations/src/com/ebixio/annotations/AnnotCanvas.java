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

import com.ebixio.annotations.tools.ToolRect;
import com.ebixio.annotations.tools.DrawingTool;
import com.ebixio.jai.ImageDisplay;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.shapes.VmShape;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.io.Serializable;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import org.openide.awt.UndoRedo;

/**
 * @author gburca
 */
public class AnnotCanvas extends ImageDisplay implements Serializable,
        MouseListener, MouseMotionListener {
    
    public Paint paint = Color.BLUE;
    public int diam = 10;
    public float alpha = 1.0F;
    public float scale = 1.0F;
    
    public MusicPage musicPage = null;

    public Rectangle imgBounds;

    public DrawingTool tool = new ToolRect(this);
    public UndoRedo.Manager undoManager = new UndoRedo.Manager();


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
        tool.paint(g2d);

        g2d.setTransform(origXform);
    }

    /**
     * Simple annotation undo class. Only undoes annotations that have not
     * been turned to SVG yet.
     */
    class AnnotUndo extends AbstractUndoableEdit {
        private String name;
        public AnnotUndo(String name) {
            this.name = name;
        }

        @Override
        public String getPresentationName() {
            return name;
        }

        @Override
        public void undo() {
            super.undo();
            if (musicPage != null) {
                musicPage.popAnnotation();
                repaint();  // canvas.repaint()
            }
        }
    }

    public void addAnnotation(VmShape s, String name) {
        if (musicPage != null) {
            musicPage.addAnnotation(s);
            UndoableEdit edit = new AnnotUndo(name);
            UndoableEditEvent ue = new UndoableEditEvent(this, edit);
            undoManager.undoableEditHappened(ue);
        }
    }
    
    
    /** This function will convert the coordinates of a mouse click (with (0,0) being
     * the upper-left corner of the canvas component) into coordinates with respect
     * to the raw image file. The image could be scaled and translated.
     * @param point Coordinates wrt upper-left corner of canvas
     * @return Coordinates wrt raw image file
     */
    public Point getAbsolutePoint(Point point) {
        Point origin = getOrigin();
        Point p = new Point(point);
        p.translate(origin.x, origin.y);
        p.move(Math.round(p.x/scale), Math.round(p.y/scale));
        return p;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (musicPage != null) tool.mouseClicked(e);
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (musicPage != null) tool.mousePressed(e);
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (musicPage != null) tool.mouseReleased(e);
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        if (musicPage != null) tool.mouseEntered(e);
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        if (musicPage != null) tool.mouseExited(e);
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (musicPage != null) tool.mouseDragged(e);
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        if (musicPage != null) tool.mouseMoved(e);
    }
}
