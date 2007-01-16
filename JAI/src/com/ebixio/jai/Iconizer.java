package com.ebixio.jai;
/*
 * $RCSfile: Iconizer.java,v $
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
 * $Date: 2005/02/11 04:40:47 $
 * $State: Exp $
 */
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.swing.*;

/**
 * A class to create icons from Planar Images
 */

public class Iconizer implements Icon {

    protected int width  = 64;
    protected int height = 64;
    protected BufferedImage icon = null;

   /**
    * Default constructor
    */
    public Iconizer() {
    }

   /**
     * @param source a PlanarImage to be displayed.
     * @param width is the icon width
     * @param height is the icon height
     */
    public Iconizer(PlanarImage image, int width, int height) {
        this.width  = width;
        this.height = height;

        icon = iconify(image);
    }

    public int getIconWidth() {
        return width;
    }

    public int getIconHeight() {
        return height;
    }

    /**
     * Paint the icon
     */
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {

        Graphics2D g2D = null;
        if (g instanceof Graphics2D) {
            g2D = (Graphics2D)g;
        } else {
            return;
        }

        AffineTransform transform = AffineTransform.getTranslateInstance(0,0);
        g2D.drawRenderedImage(icon, transform);
    }

    private BufferedImage iconify(PlanarImage image) {
        float scale = 1.0F;

        float s1 = (float)width / (float)image.getWidth();
        float s2 = (float)height / (float)image.getHeight();

        if ( s1 > s2 ) {
            scale = s1;
        } else {
            scale = s2;
        }

        InterpolationBilinear interp = new InterpolationBilinear();

	Float scalef = new Float(scale);
	Float zerof = new Float(0.0F);
	PlanarImage temp = (PlanarImage)ScaleDescriptor.create(image, 
							       scalef,
							       scalef,
							       zerof, 
							       zerof,
							       interp,
							       null);

        return temp.getAsBufferedImage();
    }

    public void save(String filename, String format) {
        JAI.create("filestore", icon, filename, format, null);
    }
}
