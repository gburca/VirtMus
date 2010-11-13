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
import com.ebixio.virtmus.imgsrc.GenericImg;
import com.ebixio.virtmus.imgsrc.ImgSrc;
import com.ebixio.virtmus.imgsrc.PdfImg;
import com.ebixio.virtmus.imgsrc.PdfRender;
import com.ebixio.virtmus.shapes.VmShape;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.swing.event.ChangeListener;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author gburca
 */
@XStreamAlias("page")
public abstract class MusicPage {
    public MainApp.Rotation rotation = MainApp.Rotation.Clockwise_0;
    private String name = null;
    public static int thumbW = 130, thumbH = 200;
    public volatile ImgSrc imgSrc;
    

    private static transient PriorityBlockingQueue<JobRequest> renderQ = new PriorityBlockingQueue<JobRequest>();
    private static transient Map<JobRequester, BufferedImage> renderResults = 
            Collections.synchronizedMap( new HashMap<JobRequester, BufferedImage>() );
    private static transient RenderThread renderThread = null;
    
    private transient DraggableThumbnail thumbnail;
    public transient Song song;
    public transient boolean isDirty = false;
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
     * @param sourceFile
     * @param opt
     */
    public MusicPage(Song song, File sourceFile, Object opt) {
        this.song = song;
        if (sourceFile.getName().toLowerCase().endsWith(".pdf")) {
            int page = (Integer)opt;
            imgSrc = new PdfImg(sourceFile, page);
            //imgSrc = new PdfRender(sourceFile, page);
        } else {
            imgSrc = new GenericImg(sourceFile);
        }
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

    public void setDirty(boolean isDirty) {
        this.thumbnail = null;

        if (this.isDirty == isDirty) return;
        
        //song.fire("pageSetDirty", this.isDirty, isDirty);

        this.isDirty = isDirty;
        // If this change is not done by a NodeAction, we need to enable the actions here.
        SystemAction.get(SaveAllAction.class).setEnabled(true);
        SystemAction.get(SongSaveAction.class).setEnabled(true);
    }
    
    public boolean isDirty() {
        return this.isDirty;
    }
    
//    /** When reading old files in (that don't have a rotation value) we need to
//     * initialize rotation */
//    private Object readResolve() {
//        if (rotation == null) { rotation = MainApp.Rotation.Clockwise_0; }
//        return this;
//    }
    
    public DraggableThumbnail getThumbnail() {
        if (thumbnail == null) {
            thumbnail = new DraggableThumbnail(thumbW, thumbH, getName());
            thumbnail.setPage(this);
            thumbnail.setPreferredSize(new Dimension(thumbW, thumbH));
            thumbnail.setMinimumSize(new Dimension(thumbW, thumbH));
        }
        return thumbnail;
    }

    public File getSourceFile() {
        return imgSrc.getSourceFile();
    }

    public void setSourceFile(File file) {
        imgSrc.setSourceFile(file);
    }

    public void setName(String name) {
        if (name.equals(this.name)) return;
        this.name = name;
        if (this.thumbnail != null) this.thumbnail.setName(name);
        setDirty(true);
        song.notifyListeners();
    }
    public String getName() {
        if (name != null && name.length() > 0) return name;
        if (imgSrc != null) {
            return imgSrc.getName();
        }
        return "No name";
    }
    
    /**
     * The index of this page in the song's page order.
     * @return The 0-based index of this page in the song's page order.
     */
    public int getPageNumber() {
        return song.pageOrder.indexOf(this);
    }
    
    
    protected BufferedImage getImage(Dimension containerSize, MainApp.Rotation rotation, boolean fillSize) {
        return imgSrc.getImage(containerSize, rotation, fillSize, this);
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

        // TODO: Allow multiple threads
        if (renderThread == null || !renderThread.isAlive()) {
            renderThread = new RenderThread();
            renderThread.setName("MusicPage render");
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
                this.setName("Rendering " + j.page.getName());
                
                int maxPriority = this.getThreadGroup().getMaxPriority();
                float relativePriority = 1.0F - (j.priority / JobRequest.MAX_PRIORITY);
                relativePriority = maxPriority * relativePriority;
                relativePriority = Math.max(Math.round(relativePriority), Thread.MIN_PRIORITY);
                this.setPriority((int)relativePriority);
                
                renderResults.put(j.requester, j.page.getImage(j.dim, j.rotation, j.fillSize));
                j.requester.renderingComplete(j.page, j);
            }
            this.setName("MusicPage render");
        }
    }

    
}
