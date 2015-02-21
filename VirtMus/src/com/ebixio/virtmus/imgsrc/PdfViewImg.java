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

import com.ebixio.util.Log;
import com.ebixio.virtmus.Utils;
import com.sun.pdfview.PDFCmd;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFObject;
import com.sun.pdfview.PDFPage;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
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
        PDFPage pdfPage = getPdfPage(null);
        Dimension dim = getLargestDisplay();
        int max = Math.max(dim.width, dim.height);
        Dimension pdim = pdfPage.getUnstretchedSize(max, max, null);

        int min = Math.min(pdim.width, pdim.height);
        float pageScale = (max * 1F) / min;

        return new Dimension((int)(pdim.width * pageScale), (int)(pdim.height * pageScale));

// TODO: The new version of PDFRenderer doesn't have getPageImages().
//        int rotation = pdfPage.getRotation();
//        List<PDFImage> imgs = pdfPage.getPageImages();
//        if (imgs.size() == 1) {
//            PDFImage img = imgs.get(0);
//            if (rotation == 90 || rotation == 270) {
//                return new Dimension(img.getHeight(), img.getWidth());
//            } else {
//                return new Dimension(img.getWidth(), img.getHeight());
//            }
//        }
    }

    private void debug() {
        final PDFFile pdfFile = getPDFFile();

        try {
            Iterable<String> kIt = new Iterable<String>() {
                @Override
                public Iterator<String> iterator() {
                    try { return pdfFile.getMetadataKeys();
                    } catch (IOException ex) { return null; }
                }
            };
            for (String k : kIt) {
                Log.log("Key: " + k + " = " + pdfFile.getStringMetadata(k));
            }

            PDFObject root = pdfFile.getRoot();
            for (String k : root.getDictionary().keySet()) {
                Log.log("RootDictKey: " + k);
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }


        PDFPage pdfPage = getPdfPage(pdfFile);
        for (PDFCmd cmd : pdfPage.getCommands()) {
            Log.log("CmdClass: " + cmd.getClass().getCanonicalName());
            Log.log("Cmd: " + cmd.toString() + " Details: " + cmd.getDetails());
        }
    }

    /** Find the largest display the page might need to be rendered for. */
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
    }

    @Override
    public PlanarImage getFullImg() {
        RenderedOp op = getFullRenderedOp();
        if (op == null) {
            return null;
        } else {
            return PlanarImage.wrapRenderedImage(op);
        }
    }

    @Override
    protected RenderedOp getFullRenderedOp() {
        Image img = getPageImage();
        if (img == null) {
            return null;
        } else {
            RenderedOp rend = JAI.create("AWTImage", img);
            return rend;
        }
    }

    private Image getPageImage() {
        debug();
        PDFPage pdfPage = getPdfPage(null);
        if (pdfPage == null) return null;

        Dimension dim = getDimension();
        /*
        Note: The crop box is in PDF coordinate space with the y-axis increasing
        going up. PDF origin is at the bottom left of the page. Java origin is
        at the top left of the screen.
        */
        Rectangle2D cbox = pdfPage.getPageBox();
        Rectangle2D bbox = pdfPage.getBBox();
        Image img = pdfPage.getImage(
                dim.width, dim.height,
                cbox,   // clip rect
                null,   // null for the ImageObserver
                true,   // fill background with white
                true);  // block until drawing is done

        return img;
    }
    private PDFPage getPdfPage(PDFFile pdfFile) {
        if (pdfFile == null) {
            pdfFile = getPDFFile();
        }

        if (pdfFile != null) {
            return pdfFile.getPage(getPageNum() + 1, true);
        } else {
            return null;
        }
    }

    private PDFFile getPDFFile() {
        try {
            RandomAccessFile raf = new RandomAccessFile(sourceFile, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            PDFFile pdfFile = new PDFFile(buf);
            return pdfFile;
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public File createImageFile() {
        tmpImgFile = null;
        try {
            tmpImgFile = File.createTempFile("VirtMus", ".jpg");
            ImageIO.write(getFullRenderedOp(), "jpg", tmpImgFile);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return tmpImgFile;
    }
}
