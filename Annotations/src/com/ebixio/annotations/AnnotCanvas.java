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

import com.ebixio.jai.ImageDisplay;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.*;
import java.io.Serializable;
import javax.swing.JComponent;

/**
 * @author gburca
 */
public class AnnotCanvas extends ImageDisplay implements Serializable, MouseListener, MouseMotionListener {
    
    public static final String PROP_SAMPLE_PROPERTY = "sampleProperty";    
    private String sampleProperty;
    private PropertyChangeSupport propertySupport;
    
    // Dragging states
    private static final byte STATE_UNKNOWN = 0, STATE_DRAGGING = 1, STATE_STILL = 2;
    private byte dragState = STATE_UNKNOWN;
    private Point dragStart;
    
    public AnnotCanvas() {
        propertySupport = new PropertyChangeSupport(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(Color.BLACK);
    }
    
    public String getSampleProperty() {
        return sampleProperty;
    }
    
    public void setSampleProperty(String value) {
        String oldValue = sampleProperty;
        sampleProperty = value;
        propertySupport.firePropertyChange(PROP_SAMPLE_PROPERTY, oldValue, sampleProperty);
    }
    
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
    private int diam = 10;

    public void setDiam(int val) {
        this.diam = val;
    }
    
    public int getDiam() {
        return diam;
    }
    
    private Paint paint = Color.BLUE;
    public void setPaint(Paint c) {
        this.paint = c;
    }
    
    public Paint getPaint() {
        return paint;
    }
    
    public Color getColor() {
        if (paint instanceof Color) {
            return (Color) paint;
        } else {
            return Color.BLACK;
        }
    }
    
    private BufferedImage backingImage = null;
    public void clear() {
        backingImage = null;
        repaint();
    }
    
    public BufferedImage getBuffImage() {
        if (backingImage == null || backingImage.getWidth() != getWidth() || backingImage.getHeight() != getHeight()) {
            BufferedImage old = backingImage;
            backingImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics g = backingImage.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (old != null) {
                ((Graphics2D) backingImage.getGraphics()).drawRenderedImage(old,
                        AffineTransform.getTranslateInstance(0, 0));
            }
        }
        return backingImage;
    }
    
    public void paint(Graphics g) {
        super.paint(g);
        //Graphics2D g2d = (Graphics2D) g;
        //g2d.drawRenderedImage(getBuffImage(), AffineTransform.getTranslateInstance(0,0));
    }
    
    public void setOrigin(Point p) {
        super.setOrigin(p.x, p.y);
    }
    public Point getOrigin() {
        return new Point(super.getXOrigin(), super.getYOrigin());
    }
    
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point p = e.getPoint();
            int half = diam / 2;
            Graphics g = getBuffImage().getGraphics();
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ((Graphics2D) g).setPaint(getPaint());
            g.fillOval(p.x - half, p.y - half, diam, diam);
            repaint(p.x - half, p.y - half, diam, diam);
        }
    }
    
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) {
            dragState = STATE_DRAGGING;
            dragStart = e.getPoint();
        }
    }
    
    public void mouseReleased(MouseEvent e) {
        dragState = STATE_STILL;
    }
    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
        dragState = STATE_STILL;
    }
    
    public void mouseDragged(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseClicked(e);
        } else if (dragState == STATE_DRAGGING) {
            int dx = e.getX() - dragStart.x;
            int dy = e.getY() - dragStart.y;
            Point p = this.getOrigin();
            p.translate(dx, dy);
            this.setOrigin(p);
            dragStart = e.getPoint();
        }        
    }
    
    public void mouseMoved(MouseEvent e) {
    }
    
    JComponent createBrushSizeView() {
        return new BrushSizeView();
    }
    
    
    private class BrushSizeView extends JComponent {
        
        public boolean isOpaque() {
            return true;
        }
        
        public void paint(Graphics g) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRect(0,0,getWidth(),getHeight());
            Point p = new Point(getWidth() / 2, getHeight() / 2);
            int half = getDiam() / 2;
            int diam = getDiam();
            g.setColor(getColor());
            g.fillOval(p.x - half, p.y - half, diam, diam);
        }
        
        public Dimension getPreferredSize() {
            return new Dimension(32, 32);
        }
        
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
    }

}
