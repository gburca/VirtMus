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

import com.sun.media.jai.codec.FileSeekableStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.concurrent.PriorityBlockingQueue;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import org.openide.nodes.Node;

/**
 *
 * @author gburca
 */
@XStreamAlias("page")
public class MusicPage {
    private File sourceFile;
    public MainApp.Rotation rotation = MainApp.Rotation.Clockwise_0;
    private String name = null;
    public static int thumbW = 130, thumbH = 200;
    

    private static transient PriorityBlockingQueue<JobRequest> renderQ = new PriorityBlockingQueue<JobRequest>();
    private static transient Map<ActionListener, BufferedImage> renderResults = 
            Collections.synchronizedMap( new HashMap<ActionListener, BufferedImage>() );
    private static transient RenderThread renderThread = null;
    
    private transient DraggableThumbnail thumbnail;
    public transient Song song;
    public transient boolean isDirty = false;
    
    /** Creates a new instance of MusicPage */
    public MusicPage(Song song) {
        this.song = song;
    }
    
    public MusicPage(Song song, File sourceFile) {
        this.song = song;
        this.setSourceFile(sourceFile);
    }
    
    /** When reading old files in (that don't have a rotation value) we need to
     * initialize rotation */
    private Object readResolve() {
        if (rotation == null) { rotation = MainApp.Rotation.Clockwise_0; }
        return this;
    }
    
    public MusicPage clone() {
        return this.clone(this.song);
    }
    public MusicPage clone(Song song) {
        MusicPage mp = new MusicPage(song, this.sourceFile);
        mp.setName(this.getName());
        mp.rotation = this.rotation;
        return mp;
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
    
    public String getName() {
        if (name != null && name.length() > 0) return name;
        if (sourceFile != null) return this.sourceFile.getName().replaceFirst("\\..*", "");
        return "No name";
    }

    private BufferedImage getImage(Dimension destSize, MainApp.Rotation rotation, boolean fillSize) {
        RenderedOp srcImg, destImg;
        Rectangle size;

        BufferedImage result = new BufferedImage(destSize.width, destSize.height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = result.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, result.getWidth(), result.getHeight());
        g.setColor(Color.WHITE);
        
        RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        RenderingHints interpHints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        
        AffineTransform origXform = g.getTransform();
        g.setTransform(rotation.getTransform(destSize.getSize()));
        
        switch (rotation) {
            case Clockwise_90:
            case Clockwise_270:
                g.setRenderingHints(interpHints);
                // Rotate the container size if the image is rotated sideways
                size = new Rectangle(destSize.height, destSize.width);
                break;
            default:
                size = new Rectangle(destSize);
                break;
        }

        FileSeekableStream stream = null;
        try {
            stream = new FileSeekableStream(sourceFile.toString());
        } catch (IOException e) {
            String msg = "Bad file.";
            int strW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (int)(size.getWidth()/2 - strW/2), (int)(size.getHeight()/2));
            return result;
        }
        
        // Create an operator to decode the image file
        srcImg = JAI.create("stream", stream);
        float scale = 1;
        
        try {
            scale = (float)Utils.scaleProportional(size, srcImg.getBounds());
        } catch (Exception e) {
            String msg = "Bad file format.";
            int strW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, (int)(size.getWidth()/2 - strW/2), (int)(size.getHeight()/2));            
            return result;
        }
        
        /* When using the "scale" operator to reduce the size of an image, the result is very poor,
         * even with bicubic interpolation. SubsampleAverage gives much better results, but can
         * only scale down an image.
         */
        if (scale < 1.0) {
            // For the SubsampleAverage operator, scale must be (0,1]
            destImg = JAI.create("SubsampleAverage", srcImg, (double)scale, (double)scale, qualityHints);
        } else if (scale > 1.0) {
            // Create a bilinear interpolation object to be used with the "scale" operator
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
        } else { // scale == 1.0
            destImg = srcImg;
        }

        //srcImg = rotate(srcImg, rotation);
        //MainApp.log("ImageSize: scaled: " + srcImg.getBounds() + " rotated: " + destImg.getBounds());
        
        srcImg = destImg;
        if (fillSize) {
            Point p = Utils.centerItem(size, srcImg.getBounds());
            
            g.drawImage(srcImg.getAsBufferedImage(), p.x, p.y, srcImg.getWidth(), srcImg.getHeight(), null);
            g.setTransform(origXform);
            
            return result;
        } else {
            return rotate(srcImg, rotation).getAsBufferedImage();
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

    public BufferedImage getRenderedImage(ActionListener requester) {
        return renderResults.remove(requester);
    }
    
    public boolean requestRendering(ActionListener requester, int priority, Dimension dim, Properties props) {
        cancelRendering(requester);
        this.renderQ.add(new JobRequest(this, requester, priority, dim, props));
        
        if (renderThread == null || !renderThread.isAlive()) {
            renderThread = new RenderThread();
            renderThread.start();
        }
        
        return true;
    }
    public static void cancelRendering(ActionListener requester) {
        // Remove all previous jobs requested by this same requester
        for (JobRequest j: renderQ.toArray(new JobRequest[0])) {
            if (j.requester == requester) renderQ.remove(j);
        }        
    }
    
    private class JobRequest implements Comparable {
        public static final int MAX_PRIORITY = 10;
        public MusicPage page;
        public ActionListener requester;
        public Integer priority;
        public Dimension dim;
        public Properties props;
        
        public JobRequest() {}
        
        public JobRequest(MusicPage page, ActionListener requester, int priority, Dimension dim, Properties props) {
            this.page = page;
            this.requester = requester;
            this.priority = Math.min(priority, MAX_PRIORITY);
            this.dim = dim;
            this.props = props;
        }

        public int compareTo(Object other) {
            return this.priority.compareTo( ((JobRequest)other).priority);
        }
    }
    
    private class RenderThread extends Thread {
        public void run() {
            while (!renderQ.isEmpty()) {
                JobRequest j = renderQ.poll();
                MainApp.Rotation rotation = MainApp.Rotation.Clockwise_0;
                boolean fillSize = true;
                if (j.props != null) {
                    rotation = MainApp.Rotation.valueOf(j.props.getProperty("rotation", MainApp.Rotation.Clockwise_0.toString()));
                    fillSize = Boolean.parseBoolean(j.props.getProperty("fillSize", "true"));
                }
                
                int maxPriority = this.getThreadGroup().getMaxPriority();
                float relativePriority = 1.0F - (j.priority / JobRequest.MAX_PRIORITY);
                relativePriority = maxPriority * relativePriority;
                relativePriority = Math.max(Math.round(relativePriority), this.MIN_PRIORITY);
                this.setPriority((int)relativePriority);
                
                renderResults.put(j.requester, j.page.getImage(j.dim, rotation, fillSize));
                ByteArrayOutputStream serializedProps = new ByteArrayOutputStream();
                if (j.props != null) {
                    try {
                        j.props.storeToXML(serializedProps, "RenderedImage");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                j.requester.actionPerformed(new ActionEvent(j.page, ActionEvent.ACTION_PERFORMED, serializedProps.toString()));
            }
        }
    }

    public void setName(String name) {
        this.name = name;
        if (this.thumbnail != null) this.thumbnail.setName(name);
        this.isDirty = true;
    }
}
