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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.media.jai.PlanarImage;

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

    /**
     * For derived classes that have raster images as a source, this is the size
     * of the raster image. For derived classes that have vector images as a
     * source (some PDFs) this should probably be computed so that the shortest
     * image side is the same as the shortest display side.
     *
     * @return The full image size (unscaled).
     */
    public abstract Dimension getDimension();
    public abstract BufferedImage getImage(Dimension containerSize, MainApp.Rotation rotation, boolean fillSize, MusicPage page);
    /**
     * Used by Annotation component (not by thumbs, they use getImage)
     * @return The full unscaled image (the size should match what getDimension()
     * returns).
     */
    public abstract PlanarImage getFullImg();

    /**
     * This function will be called when the SVG file is generated for the external
     * SVG editor. Derived classes that contain multiple images per sourceFile will
     * need to extract the particular image that corresponds to this object into a
     * stand-alone image file.
     *
     * @return The path to a single image file that can be used as SVG background.
     */
    public abstract File createImageFile();

    /**
     * This function will be called after the external SVG editor exits. The file
     * created in createImageFile() (if any) can be deleted here.
     */
    public abstract void destroyImageFile();


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

    protected BufferedImage errText(String msg, Rectangle destSize) {
        // Acquiring the current Graphics Device and Graphics Configuration
        GraphicsEnvironment graphEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphDevice = graphEnv.getDefaultScreenDevice();
        GraphicsConfiguration graphicConf = graphDevice.getDefaultConfiguration();
        System.gc();
        BufferedImage result = graphicConf.createCompatibleImage(destSize.width, destSize.height, Transparency.OPAQUE);

        Graphics2D g = result.createGraphics();
        g.setColor(Color.gray);
        g.fillRect(0, 0, destSize.width, destSize.height);
        g.setColor(Color.white);
        g.setFont(new Font("SansSerif", Font.BOLD, 32));

        int strW = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (int)(destSize.getWidth()/2 - strW/2), (int)(destSize.getHeight()/2));
        return result;
    }
}
