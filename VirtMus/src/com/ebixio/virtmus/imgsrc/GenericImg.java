/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.imgsrc;

import com.ebixio.util.Log;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.Utils;
import com.ebixio.virtmus.VirtMusKernel;
import com.ebixio.virtmus.options.Options;
import com.sun.media.jai.codec.FileSeekableStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import javax.media.jai.*;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.openide.util.Exceptions;

/**
 *
 * @author Gabriel Burca
 */
public class GenericImg extends ImgSrc {

    public GenericImg(File sourceFile) {
        super(sourceFile);
    }

    @Override
    public ImgType getImgType() {
        String name = sourceFile.getName().toLowerCase();
        if (name.endsWith(".png")) {
            return ImgType.PNG;
        } else if (name.endsWith("jpg") || name.endsWith("jpeg")) {
            return ImgType.JPG;
        } else {
            return ImgType.OTHER;
        }
    }

    void closeStream(FileSeekableStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public Dimension getDimension() {
        if (dimension != null) {
            return dimension;
        } else {
            FileSeekableStream stream = null;
            try {
                stream = new FileSeekableStream(sourceFile.toString());
            } catch (IOException e) {
                Log.log("MusicPage file: " + sourceFile.toString());
                Log.log(e.toString());
                closeStream(stream);
                return new Dimension(1, 1);
            }
            // Create an operator to decode the image file
            RenderedOp srcImg = JAI.create("stream", stream);
            try {
                dimension = srcImg.getBounds().getSize();
                srcImg.dispose();
            } catch (Exception e) {
                Log.log("Bad file format: " + sourceFile.toString());
                closeStream(stream);
                return new Dimension(1, 1);
            }
            closeStream(stream);
            return dimension;
        }
    }

    @Override
    public synchronized BufferedImage getImage(Dimension containerSize, Options.Rotation rotation, boolean fillSize, MusicPage page) {
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

        FileSeekableStream stream;
        try {
            stream = new FileSeekableStream(sourceFile.toString());
        } catch (IOException e) {
            return errText(result, g, "Bad file.", destSize);
        }

        // Create an operator to decode the image file
        srcImg = JAI.create("stream", stream);
        float scale;

        try {
            //this.dimension = srcImg.getBounds().getSize();
            scale = (float)Utils.scaleProportional(destSize, srcImg.getBounds());
        } catch (Exception e) {
            srcImg.dispose();
            closeStream(stream);
            return errText(result, g, "Bad file format.", destSize);
        }

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
            destPt = Utils.centerItem(destSize, srcImg.getBounds());
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
        page.paintAnnotations(g);
        g.setTransform(origXform);

        Dimension dim = srcImg.getBounds().getSize();
        srcImg.dispose();
        closeStream(stream);
        g.dispose();

        if (fillSize) {
            return result;
        } else {
            return result.getSubimage(0, 0, dim.width, dim.height);
        }
    }

    @Override
    public PlanarImage getFullImg() {
        if (sourceFile.exists() && sourceFile.canRead()) {
            return JAI.create("fileload", sourceFile.toString());
        } else {
            return null;
        }
    }

    @Override
    public File createImageFile() {
        return sourceFile;
    }

    @Override
    public void destroyImageFile() {
        // Nothing to do.
    }
}
