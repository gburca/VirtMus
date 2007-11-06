/*
 * MusicPage.java
 *
 * Copyright (C) 2006-2007  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

package com.ebixio.virtmus;

import com.ebixio.virtmus.actions.SaveAllAction;
import com.ebixio.virtmus.actions.SongSaveAction;
import com.ebixio.virtmus.shapes.VmShape;
import com.sun.media.jai.codec.FileSeekableStream;
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
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.RenderedOp;
import javax.swing.event.ChangeListener;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author gburca
 */
public abstract class MusicPage {
    protected File sourceFile;
    public MainApp.Rotation rotation = MainApp.Rotation.Clockwise_0;
    private String name = null;
    public static int thumbW = 130, thumbH = 200;
    

    private static transient PriorityBlockingQueue<JobRequest> renderQ = new PriorityBlockingQueue<JobRequest>();
    private static transient Map<JobRequester, BufferedImage> renderResults = 
            Collections.synchronizedMap( new HashMap<JobRequester, BufferedImage>() );
    private static transient RenderThread renderThread = null;
    
    private transient DraggableThumbnail thumbnail;
    public transient Song song;
    public transient boolean isDirty = false;
    public transient Dimension dimension = null;
    /** The AnnotTopComponent sets itself as a change listener so that we can 
     * tell it to repaint the annotation canvas when the SVG changes */
    protected transient ChangeListener changeListener = null;

    
    public abstract void clearAnnotations();
    public abstract void addAnnotation(VmShape shape);
    public abstract void paintAnnotations(Graphics2D g2d);
    @Override
    public abstract MusicPage clone();
    public abstract MusicPage clone(Song song);
    public abstract void prepareToSave();
    
    

    /** Creates a new instance of MusicPage 
     * @param song The song that this music page belongs to.
     */
    public MusicPage(Song song) {
        this.song = song;
    }
    
    public MusicPage(Song song, File sourceFile) {
        this.song = song;
        this.setSourceFile(sourceFile);
    }

    /**
     * The transient fields need to be initialized here. No constructor is called when
     * the object is deserialized.
     * @param s The Song that this music page belongs to.
     */
    public void deserialize(Song s) {
        song = s;
    }
    
    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }


    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
        this.thumbnail = null;
        // If this change is not done by a NodeAction, we need to enable the actions here.
        SystemAction.get(SaveAllAction.class).setEnabled(true);
        SystemAction.get(SongSaveAction.class).setEnabled(true);
    }
    
    
    /** When reading old files in (that don't have a rotation value) we need to
     * initialize rotation */
    private Object readResolve() {
        if (rotation == null) { rotation = MainApp.Rotation.Clockwise_0; }
        return this;
    }
    
    public DraggableThumbnail getThumbnail() {
        if (thumbnail == null) {
            thumbnail = new DraggableThumbnail(thumbW, thumbH, getSourceFile(), getName());
            thumbnail.setPage(this);
            thumbnail.setPreferredSize(new Dimension(thumbW, thumbH));
            thumbnail.setMinimumSize(new Dimension(thumbW, thumbH));
        }
        return thumbnail;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }
    
    public void setName(String name) {
        this.name = name;
        if (this.thumbnail != null) this.thumbnail.setName(name);
        this.isDirty = true;
    }
    public String getName() {
        if (name != null && name.length() > 0) return name;
        if (sourceFile != null) return this.sourceFile.getName().replaceFirst("\\..*", "");
        return "No name";
    }
    
    /**
     * The index of this page in the song's page order.
     * @return The 0-based index of this page in the song's page order.
     */
    public int getPageNumber() {
        return song.pageOrder.indexOf(this);
    }
    
    
    private BufferedImage getImage(Dimension containerSize, MainApp.Rotation rotation, boolean fillSize) {
        RenderedOp srcImg, destImg;
        Rectangle destSize;

        BufferedImage result = new BufferedImage(containerSize.width, containerSize.height, BufferedImage.TYPE_INT_ARGB_PRE);
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

        FileSeekableStream stream = null;
        try {
            stream = new FileSeekableStream(sourceFile.toString());
        } catch (IOException e) {
            String msg = "Bad file.";
            int strW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (int)(destSize.getWidth()/2 - strW/2), (int)(destSize.getHeight()/2));
            return result;
        }
        
        // Create an operator to decode the image file
        srcImg = JAI.create("stream", stream);
        float scale = 1;
        
        try {
            this.dimension = srcImg.getBounds().getSize();
            scale = (float)Utils.scaleProportional(destSize, srcImg.getBounds());
        } catch (Exception e) {
            String msg = "Bad file format.";
            int strW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (int)(destSize.getWidth()/2 - strW/2), (int)(destSize.getHeight()/2));            
            return result;
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
        this.paintAnnotations(g);
        g.setTransform(origXform);
        
        if (fillSize) {
            return result;
        } else {
            return result.getSubimage(0, 0, srcImg.getWidth(), srcImg.getHeight());
        }
    }
        
    private RenderedOp rotate(RenderedOp srcImg, MainApp.Rotation rotation) {
        if (rotation == MainApp.Rotation.Clockwise_0) return srcImg;
        
        Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        
        ParameterBlock params = new ParameterBlock();
        params.addSource(srcImg);
        params.add(srcImg.getWidth()/2.0F);
        params.add(srcImg.getHeight()/2.0F);
        params.add(new Float(rotation.radians()));
        params.add(interp);

        return JAI.create("rotate", params);
    }
    
    /**
     * Some sample code to save the page image to an external file. The image size
     * will match the current display size and orientation.
     * 
     * @param file The file to save the image to
     * @param format The file format to use ("png", or "jpg")
     */
    public void saveImg(final File file, final String format) {
        Dimension displaySize = Utils.getScreenSize();
        Dimension rotatedSize = MainApp.screenRot.getSize(displaySize);
        BufferedImage img = this.getImage(rotatedSize, MainApp.Rotation.Clockwise_0, false);
        
        // Let's find out what the most efficient format is...
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage imgToSave = gc.createCompatibleImage(img.getWidth(), img.getHeight(), Transparency.OPAQUE);
        //final BufferedImage imgToSave = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        //final BufferedImage imgToSave = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        
        Graphics2D g2 = imgToSave.createGraphics();
        boolean done = g2.drawImage(img, 0, 0, new ImageObserver() {
            
            public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                if ((infoflags & ImageObserver.ALLBITS) > 0) {
                    try {
                        ImageIO.write(imgToSave, format, file);
                    } catch (Exception ex) {
                        
                    }
                    return false;
                }
                return true;
            }
        });

        if (done) {
            try {
                ImageIO.write(imgToSave, format, file);
            } catch (Exception ex) { }
        }
    }

    public BufferedImage getRenderedImage(JobRequester requester) {
        return renderResults.remove(requester);
    }
    
    public boolean requestRendering(JobRequest request) {
        // TODO: Consider switching to org.openide.util.RequestProcessor
        request.page = this;
        cancelRendering(request.requester);
        MusicPage.renderQ.add(request);
        
        if (renderThread == null || !renderThread.isAlive()) {
            renderThread = new RenderThread();
            renderThread.start();
        }
        
        return true;
    }
    public static void cancelRendering(JobRequester requester) {
        // Remove all previous jobs requested by this same requester
        for (JobRequest j: renderQ.toArray(new JobRequest[0])) {
            if (j.requester == requester) renderQ.remove(j);
        }        
    }
    
    public interface JobRequester {
        public void renderingComplete(MusicPage mp, JobRequest jr);
    }
    
    public static class JobRequest implements Comparable {
        public static final int MAX_PRIORITY = 10;
        /** This is needed because all job requests get dumped into a static
         * render queue and when retrieved from the queue we need to know which
         * page should be asked to do the rendering. */
        public MusicPage page;
        public int pageNr;
        public JobRequester requester;
        public Integer priority;
        public Dimension dim;
        public MainApp.Rotation rotation = MainApp.Rotation.Clockwise_0;
        public boolean fillSize = false;
        
        public JobRequest(JobRequester requester, int pageNr, int priority, Dimension dim) {
            this.requester = requester;
            this.pageNr = pageNr;
            this.priority = priority;
            this.dim = dim;
        }

        public int compareTo(Object other) {
            return this.priority.compareTo( ((JobRequest)other).priority);
        }
    }
    
    /** Using a single thread to handle all the page rendering so that we don't
     * have to worry about making getImage thread safe or synchronized and so that
     * we can prioritize the renderings in one place. */
    private class RenderThread extends Thread {
        @Override
        public void run() {
            while (!renderQ.isEmpty()) {
                JobRequest j = renderQ.poll();
                
                int maxPriority = this.getThreadGroup().getMaxPriority();
                float relativePriority = 1.0F - (j.priority / JobRequest.MAX_PRIORITY);
                relativePriority = maxPriority * relativePriority;
                relativePriority = Math.max(Math.round(relativePriority), Thread.MIN_PRIORITY);
                this.setPriority((int)relativePriority);
                
                renderResults.put(j.requester, j.page.getImage(j.dim, j.rotation, j.fillSize));
                j.requester.renderingComplete(j.page, j);
            }
        }
    }

    
}
