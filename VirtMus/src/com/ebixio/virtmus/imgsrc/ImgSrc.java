/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus.imgsrc;

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.xml.FileConverter;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.ref.WeakReference;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;

/**
 *
 * @author GBURCA
 */
public abstract class ImgSrc {
    transient protected Dimension dimension = null;
    @XStreamConverter(FileConverter.class)
    public File sourceFile;

    public ImgSrc(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public abstract Dimension getDimension();
    public abstract BufferedImage getImage(Dimension containerSize, MainApp.Rotation rotation, boolean fillSize, MusicPage page);
    public abstract PlanarImage getFullImg();

    public String getName() {
        if (sourceFile != null) {
            return this.sourceFile.getName().replaceFirst("\\..*", "");
        }
        return "No name";
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    protected BufferedImage errText(BufferedImage img, Graphics2D g, String msg, Rectangle destSize) {
        int strW = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (int)(destSize.getWidth()/2 - strW/2), (int)(destSize.getHeight()/2));
        return img;
    }
}
