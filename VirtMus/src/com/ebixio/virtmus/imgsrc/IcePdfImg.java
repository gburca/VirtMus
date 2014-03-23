/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus.imgsrc;

import com.ebixio.util.Log;
import com.ebixio.virtmus.Utils;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PRectangle;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.PageTree;
import org.icepdf.core.pobjects.graphics.text.LineText;
import org.icepdf.core.pobjects.graphics.text.PageText;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.openide.util.Exceptions;

/**
 * Renders PDF pages using the original IcePdf library.
 * @author Gabriel Burca
 */
@XStreamAlias("pdfImg")
public class IcePdfImg extends PdfImg {
    transient float pageScale = -1;
    transient Rectangle2D.Float pdfCropBox = null;
    transient Rectangle2D.Float pdfMediaBox = null;
    // By how much we would need to rotate the image so it's straight
    transient int pdfRotation = 0;

    public IcePdfImg(File sourceFile, int pageNum) {
        super(sourceFile, pageNum);
    }

    private float getPageScale() {
        if (dimension == null) getDimension();
        return pageScale;
    }

    private Rectangle.Float scale(PRectangle pRect, double scaleW, double scaleH) {
        return new Rectangle.Float(
            (float)(pRect.x * scaleW), (float)(pRect.y * scaleH),
            (float)(pRect.width * scaleW), (float)(pRect.height * scaleH));
    }

    private Rectangle.Float rectRound(Rectangle.Float r) {
        return new Rectangle.Float(Math.round(r.x), Math.round(r.y), Math.round(r.width), Math.round(r.height));
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
            if (doc == null) return new Dimension(1, 1);
            Page page = doc.getPageTree().getPage(pageNum);

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

            Image img = getPageImage(doc);
            if (img != null) {
                // TODO: How do we get the size synchronously?
                int imgW = img.getWidth(null);
                int imgH = img.getHeight(null);

                // Multiply getPageBoundary() values by this factor to get true sizes
                double pdfScaleW = imgW / mediaBox.getWidth();
                double pdfScaleH = imgH / mediaBox.getHeight();
                //double delta = pdfScaleH - pdfScaleW; // W&H scale in PDF is not always the same
                pdfCropBox = scale(cropBox, pdfScaleW, pdfScaleH);
                pdfMediaBox = scale(mediaBox, pdfScaleW, pdfScaleH);
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
    private Dimension getDimensionBasedOnDisplay(Document doc) {
        if (doc == null) return new Dimension(1, 1);

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


    private Document getDocument() {
        Document document = new Document();
        try {
            document.setFile(sourceFile.getAbsolutePath());
        } catch (PDFException ex) {
            pageErr = "Error parsing PDF document";
            System.out.println(pageErr + " " + ex);
        } catch (PDFSecurityException ex) {
            pageErr = "Error PDF encryption not supported";
            System.out.println(pageErr + " " + ex);
        } catch (FileNotFoundException ex) {
            pageErr = "Error file not found";
            System.out.println(pageErr + " " + ex);
        } catch (IOException ex) {
            pageErr = "Error handling PDF document";
            System.out.println(pageErr + " " + ex);
        }

        if (pageErr != null) document = null;
        return document;
    }

    private BufferedImage getFullBufferedImage(float scale) {
        Document doc = getDocument();
        if (doc == null) {
            return null;
        }
        BufferedImage srcImgFull = (BufferedImage)doc.getPageImage(pageNum,
                GraphicsRenderingHints.SCREEN, Page.BOUNDARY_CROPBOX, 0, scale);
        Log.log("ImgSz2: " + srcImgFull.getWidth() + "x" + srcImgFull.getHeight());
        doc.dispose();
        return srcImgFull;
    }

    @Override
    protected RenderedOp getFullRenderedOp() {
        Document doc = getDocument();
        if (doc == null) {
            return null;
        }

        Image img = getPageImage(doc);
        doc.dispose();
        ParameterBlock pb;

        if (img != null) {
            // A single image covers the whole PDF page.
            Dimension dim = getDimension(); // Just to make sure the crop boxes are computed
            //Log.log("ImgSz1: " + img.getWidth(null) + "x" + img.getHeight(null));

            pb = new ParameterBlock();
            pb.addSource(img);

            // Due to floating point math, pdfCropBox could be slightly larger than the image
            // and that would cause the "crop" to throw an exception. Round the crop.
            Rectangle.Float crop = rectRound(pdfCropBox);
            pb.add(crop.x);
            pb.add(crop.y);
            pb.add(crop.width - 1);
            pb.add(crop.height - 1);
            RenderedOp r = JAI.create("crop", pb);

            // Cropping leaves the origin at the old topLeftX/Y we need to translate
            // the image so the top-left corner is again at 0,0
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
                // Image is now rotated around the 0,0 point, and completely outside
                // the view-port. We need to translate it back into the view-port.

                Point.Float transl;
                switch (pdfRotation) {
                    case 90:
                        transl = new Point.Float(pdfCropBox.height, 0F); break;
                    case 180:
                        transl = new Point.Float(pdfCropBox.width, pdfCropBox.height); break;
                    case 270:
                        transl = new Point.Float(0, pdfCropBox.width); break;
                    case 0:
                    default:
                        transl = new Point.Float(0F, 0F); break;
                }

                pb = new ParameterBlock();
                pb.addSource(r);
                pb.add(transl.x);   // Positive value moves image to the right
                pb.add(transl.y);   // Positive value moves image downwards
                r = JAI.create("translate", pb);
            }

            return r;
        } else {
            // A combination of text and/or images cover the PDF page, so we render
            // the page at a suitable scale/resolution.
            pb = new ParameterBlock();
            pb.add(getFullBufferedImage(getPageScale()));
            return JAI.create("AWTImage", pb);
        }
    }

    /**
     * We want to see if the PDF page consists of a single image (usually the case
     * when the PDF consists of scanned documents), or a combination of images
     * and/or text (usually the case when the PDF was generated by some music
     * notation software).
     *
     * If it's a single image, we extract it in its native resolution,
     * otherwise we need to rasterize the page to a desired target resolution.
     * 
     * @param doc PDF document to use
     * @return An image (in the case of a single image per page) or null otherwise.
     */
    @SuppressWarnings(value={"unchecked"})
    private Image getPageImage(Document doc) {
        ArrayList<LineText> txt;
        try {
            int pages = doc.getNumberOfPages();
            PageTree pageTree = doc.getCatalog().getPageTree();
            Page pg = pageTree.getPage(pageNum);
            if (pg != null) {
                PageText pgTxt = pg.getText();
            }
            // Next line causes VirtMus to crash with some PDF files.
            // ex: Mendelssohn - Wedding March - Pipe Orgam
            // No exception is thrown. The program just terminates.
            PageText pt = doc.getPageText(pageNum);
            txt = pt.getPageLines();
            //txt = doc.getPageText(pageNum).getPageLines();
        } catch (Exception e) {
            return null;
        }
        if (txt == null || !txt.isEmpty()) {
            return null;
        } else {
            ArrayList<Image> imgs = new ArrayList<Image>(doc.getPageImages(pageNum));

            if (imgs.size() == 1) {
                // Single image and no text == image covers whole page
                return imgs.get(0);
            } else {
                return null;
            }
        }
    }

    /**
     *
     * @return true if we think we can properly render the PDF page.
     */
    public boolean foogly() {
        // This class causes VirtMus to crash on some PDF types. See getPageImage()
        // Disabling it until we get an updated library that works.
//        return false;
        boolean result = false;
        Document doc = getDocument();

        if (doc != null) {
            if (getPageImage(doc) != null) {
                result = true;
            }
            doc.dispose();
        }

        return result;
    }

    @Override
    public PlanarImage getFullImg() {
        // We don't know yet if we have a good PDF. Let's check.
        if (getDocument() == null) {
            return PlanarImage.wrapRenderedImage(errText(pageErr, new Rectangle(850, 1100)));
        }

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
}
