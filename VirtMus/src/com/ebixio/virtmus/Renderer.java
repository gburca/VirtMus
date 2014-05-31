/*
 * MusicPage.java
 *
 * Copyright (C) 2006-2011  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

import com.ebixio.util.Log;
import com.ebixio.util.NamedThreadFactory;
import com.ebixio.virtmus.options.Options;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 *
 * A MusicPage renderer.
 */
public class Renderer {
    private static transient PriorityBlockingQueue<JobRequest> renderQ = new PriorityBlockingQueue<JobRequest>();
    private static transient Map<JobRequester, BufferedImage> renderResults =
            Collections.synchronizedMap( new HashMap<JobRequester, BufferedImage>() );
    private static transient RenderThread renderThread = null;
    // Heap space must be increased proportional to the thread pool size
    private static transient ExecutorService execSvc;
    static {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        execSvc = Executors.newFixedThreadPool(numProcessors, new NamedThreadFactory("PageRenderers"));
    }

//    private static MusicPageRenderer instance;
//    public synchronized static MusicPageRenderer single() {
//        if (instance == null) {
//            instance = new MusicPageRenderer();
//        }
//        return instance;
//    }

    public static BufferedImage getRenderedImage(JobRequester requester) {
        return renderResults.remove(requester);
    }

    public boolean requestRendering1(JobRequest request, MusicPage page) {
        // TODO: Consider switching to org.openide.util.RequestProcessor
        request.page = page;
        cancelRendering(request.requester);
        Renderer.renderQ.add(request);

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

    public static boolean requestRendering(JobRequest request, MusicPage page) {
        request.page = page;
        execSvc.execute(new RenderRunnable(request));
        return true;
    }
    
    public interface JobRequester {
        public void renderingComplete(MusicPage mp, JobRequest jr);
    }
    
    public static class JobRequest implements Comparable {
        public static final int MAX_PRIORITY = 10;
        /** This is needed because all job requests get dumped into a static
         * render queue and when retrieved from the queue we need to know which
         * page should be asked to provide the image. */
        public MusicPage page;
        public int pageNr;
        public JobRequester requester;
        public Integer priority;
        public Dimension dim;
        public Options.Rotation rotation = Options.Rotation.Clockwise_0;
        public boolean fillSize = false;

        /**
         * Creates a new job request to render an image.
         * @param requester Entity to be notified when the image is ready
         * @param pageNr The page number to render
         * @param priority The priority with which to render (0 .. MAX_PRIORITY). 0 = fastest.
         * @param dim The dimension to render at
         */
        public JobRequest(JobRequester requester, int pageNr, int priority, Dimension dim) {
            this.requester = requester;
            this.pageNr = pageNr;
            this.priority = priority < 0 ? 0 : Math.min(priority, MAX_PRIORITY);
            this.dim = dim;
        }

        @Override
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
                relativePriority = Math.min(relativePriority, Thread.MAX_PRIORITY);
                this.setPriority((int)relativePriority);

                renderResults.put(j.requester, j.page.getImage(j.dim, j.rotation, j.fillSize));
                j.requester.renderingComplete(j.page, j);
            }
            this.setName("MusicPage render");
        }
    }

    private static class RenderRunnable extends Thread {
        JobRequest j;
        public RenderRunnable(JobRequest jr) {
            j = jr;
        }

        @Override
        public void run() {
            BufferedImage img = null;

            for (int i= 0; i < 3; i++) {
                try {
                    img = j.page.getImage(j.dim, j.rotation, j.fillSize);
                    break;
                } catch (Throwable e) {
                    if (!(e instanceof OutOfMemoryError)) {
                        Log.log(e);
                        break;
                    }
                    Log.log("Caught OOM !!!!! " + j.pageNr + " in file " + j.page.imgSrc.sourceFile);
                    try {
                        sleep(250);
                        // Wait a little for some memory to hopefully free up.
                        // Gives other threads a chance to finish and release memory.
                    } catch (InterruptedException ex) { }
                }
            }

            renderResults.put(j.requester, img);
            j.requester.renderingComplete(j.page, j);
        }


    }
}
