package com.ebixio.jai;
/*
 * $RCSfile: Magnifier.java,v $
 *
 * 
 * Copyright (c) 2005 Sun Microsystems, Inc. All  Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 
 * 
 * - Redistribution of source code must retain the above copyright 
 *   notice, this  list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in 
 *   the documentation and/or other materials provided with the
 *   distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any 
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL 
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF 
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR 
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES. 
 * 
 * You acknowledge that this software is not designed or intended for 
 * use in the design, construction, operation or maintenance of any 
 * nuclear facility. 
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:40:48 $
 * $State: Exp $
 */
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/**
 * An output widget used as a magnifing glass derived from
 * javax.swing.JComponent, and can be used in any context
 * that calls for a * JComponent.
 */

public class Magnifier extends JComponent {

    private PlanarImage image;
    private JComponent parent = null;
    private float magnification = 2.0F;

    public Magnifier() {
        setOpaque(true);
    }

    public void setSource(JComponent parent) {
        this.parent = parent;
        parent.addMouseListener(new MouseClickHandler());
        parent.addMouseMotionListener(new MouseMotionHandler());

        if ( parent instanceof ImageDisplay ) {
            image = (PlanarImage)((ImageDisplay)parent).getImage();
        }
    }

    public void setMagnification(float f) {
        if ( f < 0.005 ) {
            magnification = 0.005F;
        } else {
            magnification = f;
        }

        repaint();
    }

    /**
     * Paint the image onto a Graphics object.  The painting is
     * performed tile-by-tile, and includes a grey region covering the
     * unused portion of image tiles as well as the general
     * background.
     */
    public synchronized void paintComponent(Graphics g) {

        Graphics2D g2D = null;
        if (g instanceof Graphics2D) {
            g2D = (Graphics2D)g;
        } else {
            System.err.println("not a Graphics2D");
            return;
        }

        g2D.setColor(getBackground());
        g2D.fillRect(0, 0, getWidth(), getHeight());

        if ( image != null ) {
            Dimension d = getSize();
            Point p = getLocation();
            Insets insets = parent.getInsets();

            // area to crop (plus a bit)
            int w = (int)((float)d.width  / magnification + .5F) + 1;
            int h = (int)((float)d.height / magnification + .5F) + 1;

            int x = p.x + (d.width  - w)/2 - insets.left;
            int y = p.y + (d.height - h)/2 - insets.top;

            // must clip for cropping
            if ( x < 0 ) x = 0;
            if ( y < 0 ) y = 0;

            if ( (x + w) > image.getWidth() ) {
                w = image.getWidth() - x;
            }

            if ( (y + h) > image.getHeight() ) {
                h = image.getHeight() - y;
            }

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add((float)x);
            pb.add((float)y);
            pb.add((float)w);
            pb.add((float)h);
            RenderedOp tmp = JAI.create("crop", pb, null);

	    Float magf = new Float(magnification);
	    RenderedOp dst = 
		ScaleDescriptor.create(tmp,
				       magf,
				       magf,
				       new Float(-x*magnification),
				       new Float(-y*magnification),
				       Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
				       null);

            ((OpImage)dst.getRendering()).setTileCache(null);

            g2D.drawRenderedImage(dst,
                                  AffineTransform.getTranslateInstance(0, 0));
        }
    }

    // moves the slider box
    class MouseClickHandler extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            int mods = e.getModifiers();
            Point p  = e.getPoint();

            if ( (mods & InputEvent.BUTTON1_MASK) != 0 ) {
                moveit(p.x, p.y);
            }
        }

        public void mouseReleased(MouseEvent e) {
        }
    }

    class MouseMotionHandler extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e) {
            Point p  = e.getPoint();
            int mods = e.getModifiers();

            if ( (mods & InputEvent.BUTTON1_MASK) != 0 ) {
                moveit(p.x, p.y);
            }
        }
    }

    public final void moveit(int px, int py) {
        Insets inset = parent.getInsets();
        Dimension dm = getSize();
        Dimension dp = parent.getSize();

        int pw = dm.width / 2;
        int ph = dm.height / 2;
        int x = px - pw;
        int y = py - ph;

        if ( px < inset.left ) x = -pw + inset.left;
        if ( py < inset.top  ) y = -ph + inset.top;
        if ( px >= (dp.width  - inset.right ) ) x = dp.width  - pw - inset.right;
        if ( py >= (dp.height - inset.bottom) ) y = dp.height - ph - inset.bottom;

        // magnifier origin
        setLocation(x, y);
    }
}
