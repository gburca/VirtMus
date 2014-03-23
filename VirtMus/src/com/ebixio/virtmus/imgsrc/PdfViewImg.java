/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus.imgsrc;

import com.ebixio.util.Log;
import com.ebixio.virtmus.Utils;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import org.openide.util.Exceptions;

/**
 * A class that uses com.sun.pdfview to rasterize PDFs.
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
@XStreamAlias("pdfImg")
public class PdfViewImg extends PdfImg {

    public PdfViewImg(File sourceFile, int pageNum) {
        super(sourceFile, pageNum);
    }

    @Override
    public Dimension getDimension() {
        PDFPage pdfPage = getPdfPage();
        Dimension dim = getLargestDisplay();
        int max = Math.max(dim.width, dim.height);
        dim = pdfPage.getUnstretchedSize(max, max, null);
        int rotation = pdfPage.getRotation();

        List<PDFImage> imgs = pdfPage.getPageImages();
        if (imgs.size() == 1) {
            PDFImage img = imgs.get(0);
            if (rotation == 90 || rotation == 270) {
                return new Dimension(img.getHeight(), img.getWidth());
            } else {
                return new Dimension(img.getWidth(), img.getHeight());
            }
        }

        return dim;
    }
//    public Dimension getDimension1() {
//        PDFPage pdfPage = getPdfPage();
//        List<PDFCmd> cmds = pdfPage.getCommands();
//        for (PDFCmd c : cmds) {
//            Log.log(c.toString() + " : " + c.getDetails());
//        }
//        return new Dimension((int)pdfPage.getBBox().getWidth(), (int)pdfPage.getBBox().getHeight());
//    }

    // If the PDF page is a vector drawing, we try to make the smallest page
    // dimension the same as the largest screen dimension
    private Dimension getLargestDisplay() {
        Dimension[] dims = Utils.getScreenSizes();
        int biggest = -1, idx = -1;
        for (int i = 0; i < dims.length; i++) {
            if (dims[i].width * dims[i].height > biggest) {
                biggest = dims[i].width * dims[i].height;
                idx = i;
            }
        }
        Dimension dim = dims[idx];
        return dim;
//        int max = Math.max(dim.width, dim.height);
//
//        Dimension pdim = doc.getPageDimension(pageNum, 0).toDimension();
//
//        int min = Math.min(pdim.width, pdim.height);
//        pageScale = (max * 1F) / min;
//
//        return new Dimension((int)(pdim.width * pageScale), (int)(pdim.height * pageScale));
    }


    @Override
    public PlanarImage getFullImg() {
        return PlanarImage.wrapRenderedImage(getFullRenderedOp());
    }

    @Override
    protected RenderedOp getFullRenderedOp() {
        RenderedOp rend = JAI.create("AWTImage", getPageImage());
        return rend;
    }

    private Image getPageImage() {
        PDFPage pdfPage = getPdfPage();
        //get the width and height for the doc at the default zoom
        Rectangle2D bbox = pdfPage.getBBox();

        Rectangle rect1 = new Rectangle((int)bbox.getX(), (int)bbox.getY(),
                (int)bbox.getWidth(), (int)bbox.getHeight());

        Dimension dim = getDimension();
        Rectangle rect = new Rectangle(0, 0, dim.width, dim.height);
        int rotation = pdfPage.getRotation();
        if (rotation == 90 || rotation == 270) {
            rect1 = new Rectangle(0, 0, rect1.height, rect1.width);
            // TODO: Should we flip X and Y?
            bbox = new Rectangle2D.Double(bbox.getX(), bbox.getY(), bbox.getHeight(), bbox.getWidth());
        }
        Log.log("ImgSizes: pg:" + pageNum + "  rot:" + rotation + "   " + rect1.getSize().toString() + "   " + rect.getSize().toString());

        Image img = pdfPage.getImage(
                (int)rect.width, (int)rect.height,
                bbox,   // clip rect
                null,   // null for the ImageObserver
                true,   // fill background with white
                true);  // block until drawing is done

        return img;
    }

    private PDFPage getPdfPage() {
        try {
            RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdfFile = new PDFFile(buf);
            PDFPage pdfPage = pdfFile.getPage(getPageNum() + 1, true);
            return pdfPage;
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public File createImageFile() {
        try {
            tmpImgFile = File.createTempFile("VirtMus", ".jpg");
            ImageIO.write(getFullRenderedOp(), "jpg", tmpImgFile);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return tmpImgFile;
    }
}
