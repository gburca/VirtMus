/*
 * Copyright (C) 2006-2014  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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
package com.ebixio.virtmus.imgsrc;

import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.Utils;
import com.ebixio.virtmus.VirtMusKernel;
import com.ebixio.virtmus.options.Options.Rotation;
import com.ebixio.virtmus.stats.StatsCollector;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.lang.ref.WeakReference;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;

/**
 * A proxy class for PDF image sources. Some PDF pages render better with
 * org.icepdf, others with com.sun.pdfview. This class will try to defer to
 * one of these two implementations.
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
@XStreamAlias("pdfImg")
public class PdfImg extends ImgSrc {
    public int pageNum;
    private PdfImg pdfSrc;
    protected transient String pageErr = null;
    protected transient File tmpImgFile = null;
    
    public PdfImg(File sourceFile, int pageNum) {
        super(sourceFile);
        this.pageNum = pageNum;
    }

    @Override
    public Dimension getDimension() {
        return getPdfSrc().getDimension();
    }

    // Used by Thumbs and Live display
    @Override
    public BufferedImage getImage(Dimension containerSize, Rotation rotation, boolean fillSize, MusicPage page) {
        RenderedOp srcImg, destImg;
        Rectangle destSize;

        // Acquiring the current Graphics Device and Graphics Configuration
        GraphicsEnvironment graphEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphDevice = graphEnv.getDefaultScreenDevice();
        GraphicsConfiguration graphicConf = graphDevice.getDefaultConfiguration();
        System.gc();
        BufferedImage result = graphicConf.createCompatibleImage(containerSize.width, containerSize.height, Transparency.OPAQUE);

        //BufferedImage result = new BufferedImage(containerSize.width, containerSize.height, BufferedImage.TYPE_INT_ARGB_PRE);
        // TYPE_4BYTE_ABGR_PRE (instead of TYPE_INT_ARGB_PRE) REQUIRED for OpenGL
        //BufferedImage result = new BufferedImage(containerSize.width, containerSize.height, BufferedImage.TYPE_4BYTE_ABGR_PRE);

        Graphics2D g = result.createGraphics();

        /** If a BUFFERED_IMAGE hint is not provided, the batik code issues the following warning:
         * "Graphics2D from BufferedImage lacks BUFFERED_IMAGE hint"
         *
         * See: http://mail-archives.apache.org/mod_mbox/xmlgraphics-batik-dev/200603.mbox/%3C20060309110529.1B7C96ACA9@ajax%3E
         * See: org.apache.batik.ext.awt.image.GraphicsUtil getDestination(Graphics2D g2d)
         */
        RenderingHints renderingHints = new RenderingHints(
                RenderingHintsKeyExt.KEY_BUFFERED_IMAGE,
                new WeakReference<BufferedImage>(result));
        g.addRenderingHints(renderingHints);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, result.getWidth(), result.getHeight());
        g.setColor(Color.WHITE);

        RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        RenderingHints interpHints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        AffineTransform origXform = g.getTransform();
        g.setTransform(rotation.getTransform(containerSize.getSize()));

        switch (rotation) {
            case Clockwise_90:
            case Clockwise_270:
                g.setRenderingHints(interpHints);
                // Rotate the container size if the image is rotated sideways
                destSize = new Rectangle(containerSize.height, containerSize.width);
                break;
            default:
                destSize = new Rectangle(containerSize);
                break;
        }

        float scale = (float)Utils.scaleProportional(destSize, new Rectangle(getDimension()));
        if (pageErr != null) {
            return errText(result, g, pageErr, destSize);
        }

        srcImg = getFullRenderedOp();

        /* When using the "scale" operator to reduce the size of an image, the result is very poor,
         * even with bicubic interpolation. SubsampleAverage gives much better results, but can
         * only scale down an image.
         */
        if (scale == 1.0) {
            destImg = srcImg;
        } else if (scale < 1.0 && Math.min(destSize.height, destSize.width) < 600) {
            // For the SubsampleAverage operator, scale must be (0,1]

            /** SubsampleAverage sometimes creates black horizontal lines which
             * look almost like staff lines. This only happens at certain scale
             * factors. We will therefore only use this for icons and thumbnails.
             * Anything larger than that will be scaled below.
             */
            destImg = JAI.create("SubsampleAverage", srcImg, (double)scale, (double)scale, qualityHints);
        } else {
            if (scale < 1.0) {
                // We apply a mild low-pass filter first. See:
                // http://archives.java.sun.com/cgi-bin/wa?A2=ind0311&L=jai-interest&P=15036
                // http://www.leptonica.com/scaling.html
                KernelJAI k;
                k = VirtMusKernel.getKernel(1, 1, 6);
                //k = VirtMusKernel.getKernel(0, 0, 1);   // Identity kernel
                destImg = JAI.create("Convolve", srcImg, k);
                srcImg = destImg;
            } else { // scale > 1.0
                scale = Math.min(scale, 2.0F);   // Don't zoom in more than 2x
            }

            // Create a bicubic interpolation object to be used with the "scale" operator
            Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);

            scale = Math.min(scale, 2.0F);   // Don't zoom in more than 2x

            ParameterBlock params = new ParameterBlock();
            params.addSource(srcImg);
            params.add(scale);   // x scale factor
            params.add(scale);   // y scale factor
            params.add(0.0F);   // x translation
            params.add(0.0F);   // y translation
            params.add(interp); // interpolation method

            destImg = JAI.create("scale", params);
        }

        srcImg = destImg;
        Point destPt;

        if (fillSize) {
            destPt = Utils.centerItem(destSize, Utils.scale(new Rectangle(getDimension()), scale));
        } else {
            destPt = new Point(0, 0);
        }

        AffineTransform newXform = g.getTransform();
        newXform.concatenate(AffineTransform.getTranslateInstance(destPt.x, destPt.y));
        g.setTransform(newXform);
        // Image is already scaled. Draw it before applying the scaling transform.
        g.drawImage(srcImg.getAsBufferedImage(), 0, 0, null);

        // The annotations need to be scaled properly before being drawn.
        newXform.concatenate(AffineTransform.getScaleInstance(scale, scale));

        g.setTransform(newXform);
        if (page != null) page.paintAnnotations(g);
        g.setTransform(origXform);

        Dimension dim = srcImg.getBounds().getSize();
        srcImg.dispose();
        g.dispose();

        if (fillSize) {
            return result;
        } else {
            return result.getSubimage(0, 0, dim.width, dim.height);
        }
    }

    @Override
    public PlanarImage getFullImg() {
        return getPdfSrc().getFullImg();
    }

    @Override
    public File createImageFile() {
        return getPdfSrc().createImageFile();
    }

    @Override
    public void destroyImageFile() {
        try {
            if (tmpImgFile != null) tmpImgFile.delete();
            tmpImgFile = null;
        } catch (Exception e) {
        }
    }

    protected RenderedOp getFullRenderedOp() {
        return getPdfSrc().getFullRenderedOp();
    }

    /**
     * Here we decide what PDF rasterizer to use for this page.
     * @return
     */
    private PdfImg getPdfSrc() {
        if (pdfSrc == null) {
            IcePdfImg icePdfSrc = new IcePdfImg(sourceFile, pageNum);
            if (icePdfSrc.canRender()) {
                pdfSrc = icePdfSrc;
                StatsCollector.findInstance().usingRenderer("org.icepdf");
            } else {
                pdfSrc = new PdfViewImg(sourceFile, pageNum);
                StatsCollector.findInstance().usingRenderer("com.sun.pdfview");
            }
        }
        return pdfSrc;
    }

    /**
     * Used to display the PDF type in the page property sheet.
     * @return Class of the inner PDF rasterizer.
     */
    public Class<?> getInnerClass() {
        return getPdfSrc().getClass();
    }

    /**
     * @return the pageNum
     */
    public int getPageNum() {
        return pageNum;
    }

    /**
     * @param pageNum the PDF page number
     */
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    @Override
    public String getName() {
        return super.getName() + " p" + (pageNum + 1);
    }

    public void setDimension(Dimension dim) {

    }
}
