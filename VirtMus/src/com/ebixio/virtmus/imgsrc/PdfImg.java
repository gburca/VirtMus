/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus.imgsrc;

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.Utils;
import com.ebixio.virtmus.VirtMusKernel;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Vector;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PRectangle;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.openide.util.Exceptions;

/**
 *
 * @author GBURCA
 */
@XStreamAlias("pdfImg")
public class PdfImg extends ImgSrc {
    public int pageNum;
    transient float pageScale = -1;
    transient Rectangle2D.Float pdfCropBox = null;
    transient Rectangle2D.Float pdfMediaBox = null;
    // By how much we would need to rotate the image so it's straight
    transient int pdfRotation = 0;
    transient File tmpImgFile = null;

    public PdfImg(File sourceFile, int pageNum) {
        super(sourceFile);
        this.pageNum = pageNum;
    }

    public int getPageNum() {
        return pageNum;
    }

    private float getPageScale() {
        if (dimension == null) getDimension();
        return pageScale;
    }

    private Rectangle.Float scale(PRectangle pRect, double scale) {
        return new Rectangle.Float(
            (float)(pRect.x * scale), (float)(pRect.y * scale),
            (float)(pRect.width * scale), (float)(pRect.height * scale));
    }

    // Convert from PDF to Java
    // PDF y-axis increases moving up. Java y-axis increases going down
    // pdf.y = height - img.y; img.y = height - pdf.y;
    // @param rect x,y is the top-left corner (positive height == rectangle extends down)
    // @param h The distance b/w the pdf y-intercept and the Java y-intercept
    private Rectangle.Float rect2Java(Rectangle.Float rect, float h) {
        return new Rectangle.Float(rect.x, h - rect.y, rect.width, rect.height);
    }

    @Override
    public Dimension getDimension() {
        if (dimension == null) {
            Document doc = getDocument();
            Page page = doc.getPageTree().getPage(pageNum, this);

            float rot = page.getTotalRotation(0);
            // When rot == 270, the "i" in the image has its dot to the left (o-)
            // We need to rotate the image 90 degrees clockwise so its upright
            pdfRotation = (Math.round((360 + 360-rot) / 90F) % 4) * 90;

            // All getPageBoundary numbers are in some default resolution (72dpi?)
            // The crop cropBox is what Acrobat shows on the screen
            PRectangle cropBox = page.getPageBoundary(Page.BOUNDARY_CROPBOX);

            /* This is the size of the embedded image at some default resolution
             * (typically 72dpi), so if we scan a 5x10" at 300dpi we would get
             * a mediaBox size of 5*72x10*72, not of 5*300x10*300.
             */
            PRectangle mediaBox = page.getPageBoundary(Page.BOUNDARY_MEDIABOX);

            // This is the page size after cropping was applied
            //PDimension pageDim = page.getSize(0);

            doc.getPageTree().releasePage(page, this);

            Image img = getPageImage(doc);
            if (img != null) {
                // TODO: How do we get the size synchronously?
                int imgW = img.getWidth(null);
                //int imgH = img.getHeight(null);

                // Multiply getPageBoundary() values by this factor to get true sizes
                double pdfScale = imgW / mediaBox.getWidth();
                pdfCropBox = scale(cropBox, pdfScale);
                pdfMediaBox = scale(mediaBox, pdfScale);
                pdfCropBox = rect2Java(pdfCropBox, pdfMediaBox.height);
                pdfMediaBox = rect2Java(pdfMediaBox, pdfMediaBox.height);

                if (pdfRotation == 0 || pdfRotation == 180) {
                    dimension = new Dimension(Math.round(pdfCropBox.width), Math.round(pdfCropBox.height));
                } else {
                    dimension = new Dimension(Math.round(pdfCropBox.height), Math.round(pdfCropBox.width));
                }

                pageScale = -1;
            } else {
                dimension = getDimensionBasedOnDisplay(doc);
            }

            doc.dispose();
        }
        return dimension;
    }

    // If the PDF page is a vector drawing, we try to make the smallest page
    // dimension the same as the largest screen dimension
    public Dimension getDimensionBasedOnDisplay(Document doc) {
            Dimension[] dims = Utils.getScreenSizes();
            int biggest = -1, idx = -1;
            for (int i = 0; i < dims.length; i++) {
                if (dims[i].width * dims[i].height > biggest) {
                    biggest = dims[i].width * dims[i].height;
                    idx = i;
                }
            }
            Dimension dim = dims[idx];
            int max = Math.max(dim.width, dim.height);

            Dimension pdim = doc.getPageDimension(pageNum, 0).toDimension();

            int min = Math.min(pdim.width, pdim.height);
            pageScale = (max * 1F) / min;

            return new Dimension((int)(pdim.width * pageScale), (int)(pdim.height * pageScale));
    }

    // Used by Thumbs and Live display
    @Override
    public BufferedImage getImage(Dimension containerSize, MainApp.Rotation rotation, boolean fillSize, MusicPage page) {
        RenderedOp srcImg, destImg = null;
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

    private Document getDocument() {
        Document document = new Document();
        try {
            document.setFile(sourceFile.getAbsolutePath());
        } catch (PDFException ex) {
            System.out.println("Error parsing PDF document " + ex);
        } catch (PDFSecurityException ex) {
            System.out.println("Error encryption not supported " + ex);
        } catch (FileNotFoundException ex) {
            System.out.println("Error file not found " + ex);
        } catch (IOException ex) {
            System.out.println("Error handling PDF document " + ex);
        }

        return document;
    }

    @Override
    public String getName() {
        return super.getName() + " p" + (pageNum + 1);
    }

    private BufferedImage getFullBufferedImage(float scale) {
        Document doc = getDocument();
        BufferedImage srcImgFull = (BufferedImage)doc.getPageImage(pageNum,
                GraphicsRenderingHints.SCREEN, Page.BOUNDARY_CROPBOX, 0, scale);
        MainApp.log("ImgSz2: " + srcImgFull.getWidth() + "x" + srcImgFull.getHeight());
        doc.dispose();
        return srcImgFull;
    }

    private RenderedOp getFullRenderedOp() {
        Document doc = getDocument();
        Image img = getPageImage(doc);
        doc.dispose();
        ParameterBlock pb;

        if (img != null) {
            getDimension(); // Just to make sure the crop boxes are computed
            //MainApp.log("ImgSz1: " + img.getWidth(null) + "x" + img.getHeight(null));
            RenderedOp r = null;

            pb = new ParameterBlock();
            pb.addSource(img);
            pb.add(pdfCropBox.x);
            pb.add(pdfCropBox.y);
            pb.add(pdfCropBox.width);
            pb.add(pdfCropBox.height);
            r = JAI.create("crop", pb);

            // Cropping leaves the origin at topLeftX/Y we need to translate to 0,0
            pb = new ParameterBlock();
            pb.addSource(r);
            pb.add(-pdfCropBox.x);
            pb.add(-pdfCropBox.y);
            r = JAI.create("translate", pb);

            if (pdfRotation != 0) {
                pb = new ParameterBlock();
                pb.addSource(r);
                pb.add(0F);
                pb.add(0F);
                pb.add((float)Math.toRadians(pdfRotation));
                pb.add(new InterpolationBilinear());
                r = JAI.create("rotate", pb);

                Point.Float transl;
                switch (pdfRotation) {
                    case 90:
                        transl = new Point.Float(pdfCropBox.height, 0F); break;
                    case 180:
                        transl = new Point.Float(pdfCropBox.width, -pdfCropBox.height); break;
                    case 270:
                        transl = new Point.Float(0, -pdfCropBox.width); break;
                    case 0:
                    default:
                        transl = new Point.Float(0F, 0F); break;
                }

                pb = new ParameterBlock();
                pb.addSource(r);
                pb.add(transl.x);
                pb.add(transl.y);
                r = JAI.create("translate", pb);
            }

            return r;
        } else {
            pb = new ParameterBlock();
            pb.add(getFullBufferedImage(getPageScale()));
            return JAI.create("AWTImage", pb);
        }
    }

    @SuppressWarnings(value={"unchecked"})
    private Image getPageImage(Document doc) {
        Vector<StringBuffer> txt = doc.getPageText(pageNum);
        if (txt.size() > 0) {
            return null;
        } else {
            Vector<Image> imgs = doc.getPageImages(pageNum);
            if (imgs.size() == 1) {
                // Single image and no text == image covers whole page
                return imgs.get(0);
            } else {
                return null;
            }
        }
    }

    @Override
    public PlanarImage getFullImg() {
        if (getPageScale() > 0) {
            return PlanarImage.wrapRenderedImage(getFullBufferedImage(getPageScale()));
        } else {
            // Single image in the whole page
            return PlanarImage.wrapRenderedImage( getFullRenderedOp() );
        }
    }

    @Override
    public File createImageFile() {
        try {
            tmpImgFile = File.createTempFile("VirtMus", ".jpg");
            JAI.create("filestore", getFullImg(), tmpImgFile.getCanonicalPath(), "JPEG");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
        return tmpImgFile;
    }

    @Override
    public void destroyImageFile() {
        try {
            if (tmpImgFile != null) tmpImgFile.delete();
            tmpImgFile = null;
        } catch (Exception e) {
        }
    }
}
