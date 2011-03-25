/*
 * LiveWindow.java
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.java.swingfx.waitwithstyle.PerformanceInfiniteProgressPanel;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTargetAdapter;
import org.jdesktop.animation.timing.interpolation.SplineInterpolator;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author  gburca
 */
public class LiveWindow extends javax.swing.JFrame implements Renderer.JobRequester {

    Rectangle displaySize = new Rectangle(Utils.getScreenSize());
    PerformanceInfiniteProgressPanel glasspane = new PerformanceInfiniteProgressPanel();
    /** Used for double buffering the image rendering */
    BufferStrategy bufferStrategy;
    private Song song = null;

    /** Which page is the first one on the screen */
    private int page = 0;

    /** How far the pages are advanced from the top-left of the first page.
     * A value of 1.3 means 70% of the second page is showing on screen. */
    private float pageShift = 0.0F;

    /** By how much to advance the page at a time (in %). This value comes from
     * the user preference dialog box. */
    private float pageIncrement;

    private boolean pageShiftNeeded;
    private AffineTransform xform = MainApp.screenRot.getTransform(displaySize.getSize());
    private boolean fullyPainted = false;

    /** Maximum number of "previous" pages to keep cached. For example, if the user is
    playing page 8, a value of "3" means we want to keep cached pages 5,6,7 so that
    if the user presses "PgUp" we can display them quickly. */
    final int maxPrevCache = 2;

    /** Maximum number of "next" pages to keep cached. For example, if the user is
    playing page 8, a value of "4" means we want to keep cached pages 9,10,11,12 so that
    if the user presses "PgDn" we can display them quickly. This number should be >=
    number of pages visible on the screen at a time. */
    final int maxNextCache = 5;

    /**
     * TODO: Make this configurable.
     */
    final private boolean renderSequentially = true;

    final Map<Integer, BufferedImage> pageCache = Collections.synchronizedMap(new HashMap<Integer, BufferedImage>(3));
    final ArrayList<Integer> renderFailed = new ArrayList<Integer>();
    ArrayList<Integer> toBeRendered = new ArrayList<Integer>(3);
    boolean waitingForImage = false;
    final int separatorSize = 3;
    final Color separatorColor = Color.RED;
    final Color pageShiftColor = Color.BLUE;
    Animator anim = null;
    Graphics2D graph2D;

    /** Creates new form LiveWindow */
    public LiveWindow(GraphicsConfiguration gConfig) {
        super(gConfig);
        initComponents();

        this.setSize(displaySize.width, displaySize.height);
//        glasspane.setBackground(Color.RED);
//        glasspane.setForeground(Color.GREEN);
//        glasspane.setSize(displaySize.getSize());
//        glasspane.setDoubleBuffered(true);
//        glasspane.setText(NbBundle.getMessage(LiveWindow.class, "LW_Loading"));
        this.setGlassPane(glasspane);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    showNextSection();
                } else {
                    showPrevSection();
                }
            }
        });

        pageIncrement = Float.parseFloat(NbPreferences.forModule(MainApp.class).get(MainApp.OptPageScrollAmount, "100.0")) / 100;
        if (pageIncrement == 0) {
            pageShiftNeeded = false;
        } else {
            pageShiftNeeded = true;
        }

        this.setCursor(Utils.getInvisibleCursor());

        //glasspane.setVisible(true);   // Causes strange behavior.
    }

    @Override
    public void setVisible(boolean vis) {
        super.setVisible(vis);

        // Double-buffering (can only do this after component is added to container)
        this.createBufferStrategy(2);
        this.bufferStrategy = this.getBufferStrategy();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(new java.awt.Color(102, 255, 51));
        setForeground(new java.awt.Color(255, 102, 0));
        setResizable(false);
        setUndecorated(true);
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 420, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.gc();
            this.dispose();
        } else {
            switch (evt.getKeyCode()) {
                case KeyEvent.VK_F5:
                // TODO: This does not work. A new window is created instead...
                case KeyEvent.VK_1:
                    showFirstPage();
                    break;
                case KeyEvent.VK_PAGE_UP:
                    //showPrevPage();
                    showPrevSection();
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                case KeyEvent.VK_SPACE:
                    //showNextPage();
                    showNextSection();
                    break;
                default:
                    showNextSection();
                    break;
            }
        }
    }//GEN-LAST:event_formKeyPressed

    public void setLiveSong(Song song) {
        if (song.pageOrder.isEmpty()) {
            return;
        }
        setLiveSong(song, song.pageOrder.get(0));
    }

    public void setLiveSong(Song song, MusicPage startingPage) {
        this.song = song;
        page = song.pageOrder.indexOf(startingPage);
        pageShift = page;
        synchronized (pageCache) {
            this.pageCache.clear();
        }
        this.toBeRendered.clear();
        Renderer.cancelRendering(this);

        showPage(page);
    }

    public void setPlayList(PlayList playList) {
        Song s = new Song();
        synchronized (playList.songs) {
            for (Song plSong : playList.songs) {
                synchronized (plSong.pageOrder) {
                    for (MusicPage mp : plSong.pageOrder) {
                        s.pageOrder.add(mp);
                    }
                }
            }
        }
        setLiveSong(s);
    }

    // <editor-fold defaultstate="collapsed" desc="show routines">
    public void showNextPage() {
        if (song == null || song.pageOrder == null) {
            return;
        }
        if (page < song.pageOrder.size() - 1) {
            startShift(pageShift, page + 1);
        }
    }

    public void showPrevPage() {
        if (song == null || song.pageOrder == null) {
            return;
        }
        if (page > 0) {
            startShift(pageShift, page - 1);
        } else if (page == 0 && pageShift > 0) {
            // If there's no "previous page" at least go to the top of the current page
            startShift(pageShift, page);
        }
    }

    public void showFirstPage() {
        if (song == null || song.pageOrder == null) {
            return;
        }
        page = 0;
        pageShift = 0;
        showPage(page);
    }

    public void showNextSection() {
        if (song == null || song.pageOrder == null) {
            return;
        }
        if (!pageShiftNeeded) {
            showNextPage();
        } else {
            float newShift = pageShift + pageIncrement;
            if (newShift > (song.pageOrder.size() - 1)) {
                newShift = song.pageOrder.size() - 1;
            }
            startShift(pageShift, newShift);
        }
    }

    public void showPrevSection() {
        if (song == null || song.pageOrder == null) {
            return;
        }
        if (!pageShiftNeeded) {
            showPrevPage();
        } else {
            float newShift = pageShift - pageIncrement;
            if (newShift < 0) {
                newShift = 0;
            }
            startShift(pageShift, newShift);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="paint routines">
    @Override
    public void paint(Graphics gOld) {
        paintDblBuf();
    }

    /**
     * Does the double buffering painting
     */
     synchronized protected void paintDblBuf() {
        do {
            do {
                graph2D = (Graphics2D)bufferStrategy.getDrawGraphics();
                paintDispatch();
                graph2D.dispose();
            } while (bufferStrategy.contentsRestored());
            bufferStrategy.show();
        } while (bufferStrategy.contentsLost());
    }

    /**
     * Dispatches the paint request to the appropriate paint function.
     */
    protected void paintDispatch() {
        if (pageShiftNeeded) {
            if (MainApp.scrollDir == MainApp.ScrollDir.Vertical) {
                paintShiftedVertical(graph2D);
            } else {
                paintShiftedHorizontal(graph2D);
            }
        } else {
            paintRegular(graph2D);
        }
    }

    /**
     * Paints during animation, using a double buffering scheme.
     */
    protected void animPaint() {
        //Log.log("animPaint " + pageShift);
        graph2D = (Graphics2D)bufferStrategy.getDrawGraphics();
        paintDispatch();
        graph2D.dispose();
        bufferStrategy.show();
    }

    /**
     * Used to paint when no page shift is needed. Paints only one page.
     * @param g
     */
    public void paintRegular(Graphics2D g) {
        BufferedImage img = pageCache.get(page);
        AffineTransform origXform = g.getTransform();
        g.setTransform(xform);
        Dimension d = MainApp.screenRot.getSize(displaySize.getSize());

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, d.width, d.height);

        if (img != null) {
            Point p = Utils.centerItem(new Rectangle(d), new Rectangle(img.getWidth(), img.getHeight()));
            g.drawImage(img, p.x, p.y, img.getWidth(), img.getHeight(), this);
            glasspane.setVisible(false);
        } else if (renderFailed.contains(page)) {
            g.setColor(Color.WHITE);
            String msg = NbBundle.getMessage(LiveWindow.class, "LW_LoadingFailed");
            int msgW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, d.width / 2 - msgW / 2, d.height / 2);
        } else {
            // Image is not yet ready to be displayed. Tell the user we're working on it...
            //glasspane.setVisible(true);
            g.setColor(Color.WHITE);
            String msg = NbBundle.getMessage(LiveWindow.class, "LW_Loading");
            int msgW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, d.width / 2 - msgW / 2, d.height / 2);
        }

        g.setTransform(origXform);
    }

    /** This function paints page 2 below page 1 and so on. It is typically used when the
    display is in portrait mode. */
    public void paintShiftedVertical(Graphics2D g) {
        int heightPainted = 0;
        BufferedImage img1 = pageCache.get(page);
        AffineTransform origXform = g.getTransform();
        g.setTransform(xform);

        Dimension d = MainApp.screenRot.getSize(displaySize.getSize());
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, d.width, d.height);

        if (img1 != null) {
            glasspane.setVisible(false);
            float page1Shift = pageShift - page;

            int img1y = Math.round(img1.getHeight() * page1Shift);
            g.drawImage(img1, 0, -img1y, img1.getWidth(), img1.getHeight(), Color.BLACK, this);
            heightPainted += img1.getHeight() - img1y;
            g.setColor(separatorColor);
            g.fillRect(0, heightPainted - separatorSize, d.width, separatorSize);

            BufferedImage img2 = pageCache.get(page + 1);
            if (img2 != null) {
                int img2y = img1.getHeight() - img1y;
                g.drawImage(img2, 0, img2y, img2.getWidth(), img2.getHeight(), this);
                heightPainted += img2.getHeight();
                g.fillRect(0, heightPainted - separatorSize, d.width, separatorSize);

                BufferedImage img3 = pageCache.get(page + 2);
                if (img3 != null) {
                    int img3y = img1.getHeight() - img1y + img2.getHeight();
                    g.drawImage(img3, 0, img3y, img3.getWidth(), img3.getHeight(), this);
                    heightPainted += img3.getHeight();
                    g.fillRect(0, heightPainted - separatorSize, d.width, separatorSize);
                }
            }
            // This shows where the next page shift will take us
            g.setColor(pageShiftColor);
            g.fillRect(0, (int) (img1.getHeight() * pageIncrement), 15, 3);
            g.fillRect(d.width - 15, (int) (img1.getHeight() * pageIncrement), 15, 3);
        } else if (renderFailed.contains(page)) {
            g.setColor(Color.WHITE);
            String msg = NbBundle.getMessage(LiveWindow.class, "LW_LoadingFailed");
            int msgW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, d.width / 2 - msgW / 2, d.height / 2);
        } else {
            //glasspane.setVisible(true);
            g.setColor(Color.WHITE);
            String msg = NbBundle.getMessage(LiveWindow.class, "LW_Loading");
            int msgW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, d.width / 2 - msgW / 2, d.height / 2);
        }

        fullyPainted = heightPainted > d.height ? true : false;

        g.setTransform(origXform);
    }

    public void paintShiftedHorizontal(Graphics2D g) {
        int widthPainted = 0;
        BufferedImage img1 = pageCache.get(page);
        AffineTransform origXform = g.getTransform();
        g.setTransform(xform);

        Dimension d = MainApp.screenRot.getSize(displaySize.getSize());
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, d.width, d.height);

        if (img1 != null) {
            glasspane.setVisible(false);

            float page1Shift = pageShift - page;
            int img1x = Math.round(img1.getWidth() * page1Shift);
            //g.drawImage(img1, -img1x, 0, img1.getWidth(), img1.getHeight(), Color.BLACK, this);
            g.drawImage(img1, -img1x, 0, img1.getWidth(), img1.getHeight(), this);

            widthPainted += img1.getWidth() - img1x;
            g.setColor(separatorColor);
            g.fillRect(widthPainted - separatorSize, 0, separatorSize, d.height);

            BufferedImage img2 = pageCache.get(page + 1);
            if (img2 != null) {
                int img2x = img1.getWidth() - img1x;
                g.drawImage(img2, img2x, 0, img2.getWidth(), img2.getHeight(), this);
                widthPainted += img2.getWidth();
                g.fillRect(widthPainted - separatorSize, 0, separatorSize, d.height);

                BufferedImage img3 = pageCache.get(page + 2);
                if (img3 != null) {
                    int img3x = img1.getWidth() - img1x + img2.getWidth();
                    g.drawImage(img3, img3x, 0, img3.getWidth(), img3.getHeight(), this);
                    widthPainted += img3.getWidth();
                    g.fillRect(widthPainted - separatorSize, 0, separatorSize, d.height);
                }
            }
            // This shows where the next page shift will take us
            g.setColor(pageShiftColor);
            g.fillRect((int) (img1.getWidth() * pageIncrement), 0, 3, 15);
            g.fillRect((int) (img1.getWidth() * pageIncrement), d.height - 15, 3, 15);
        } else if (renderFailed.contains(page)) {
            g.setColor(Color.WHITE);
            String msg = NbBundle.getMessage(LiveWindow.class, "LW_LoadingFailed");
            int msgW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, d.width / 2 - msgW / 2, d.height / 2);
        } else {
            //glasspane.setVisible(true);
            g.setColor(Color.WHITE);
            String msg = NbBundle.getMessage(LiveWindow.class, "LW_Loading");
            int msgW = g.getFontMetrics().stringWidth(msg);
            g.drawString(msg, d.width / 2 - msgW / 2, d.height / 2);
        }

        fullyPainted = widthPainted > d.width ? true : false;
        g.setTransform(origXform);
    }

    // </editor-fold>

    private void cleanCache() {
        cleanCache(page);
    }

    /**
     * We can't keep too many images in the cache or we'll run out of memory.
     * Given the currentPage being displayed, we discard any page images that
     * are beyond maxPrevCache or maxNextCache.
     */
    private void cleanCache(int currentPage) {
        HashSet<Integer> toKeep = new HashSet<Integer>(getPagesToCache(currentPage));
        synchronized (pageCache) {
            // Must create new set. keySet() gives us a reference to the set and
            // we'll get access exceptions for modifying while iterating.
            Set<Integer> cache = new HashSet<Integer>(pageCache.keySet());
            for (int i : cache) {
                if (! toKeep.contains(i)) pageCache.remove(i);
            }
        }
    }


    /**
     * Requests rendering of pages missing from the getPagesToCache list.
     */
    private void repopulateCache(int page) {
        if (song == null) {
            return;
        }
        toBeRendered.clear();
        synchronized (pageCache) {
            for (Integer i: getPagesToCache(page)) {
                if (!pageCache.containsKey(i)) toBeRendered.add(i);
            }
        }

        if (renderSequentially) {
            renderNext();
        } else {
            renderAll();
        }
    }

    /**
     * Computes list of pages we want rendered/cached (page-maxPrevCache through
     * page+maxNextCache) and arranges them in order based on how close they are
     * to the given page.
     *
     * @param page 0-based page number
     * @return A list of pages to be rendered/cached
     */
    private ArrayList<Integer> getPagesToCache(int page) {
        ArrayList<Integer> list = new ArrayList<Integer>(maxNextCache + maxPrevCache + 1);
        int range = Math.max(maxPrevCache, maxNextCache);
        int visible = 3;    // How many pages are visible at once on the screen

        // Render pages visible on the screen first
        for (int i= 0; i < visible; i++) {
            int p = page + i;
            if (p < song.pageOrder.size() && i <= maxNextCache)
                list.add(page + i);
        }

        // Render closest pages first, then next removed, etc...
        for (int i = 1; i <= range; i++) {
            int next = page + visible-1 + i;
            int prev = page - i;

            if (next - page <= maxNextCache) {
                // When we reach the end of the song, start rendering the beginning
                if (next >= song.pageOrder.size()) {
                    next = next % song.pageOrder.size();
                }
                if (!list.contains(next)) list.add(next);
            }

            if (prev >= 0 && page - prev <= maxPrevCache) {
                if (!list.contains(prev)) list.add(prev);
            }
        }

        return list;
    }

    /**
     * A callback that is used by the image renderer (MusicPageRemderer) to notify us
     * that the requested page has been rendered.
     * @param mp The page that has been rendered (which is also the page performing the rendering)
     * @param jr The job request that was used to request the rendering.
     */
    @Override
    public void renderingComplete(MusicPage mp, Renderer.JobRequest jr) {
        waitingForImage = false;
        if (song == null) {
            return;
        }

        if (jr.pageNr >= 0 && jr.requester == this) {
            BufferedImage img = Renderer.getRenderedImage(this);
            if (img != null) {
                synchronized (pageCache) {
                    pageCache.put(jr.pageNr, img);
                }
            } else {
                synchronized (renderFailed) {
                    renderFailed.add(jr.pageNr);
                }
            }
            if (jr.pageNr == page || !fullyPainted) {
                try {
                    paintDblBuf();
                } catch(Exception e) { }
            }
        }
        cleanCache();
        if (renderSequentially) renderNext();
    }

    private void renderAll() {
        if (song == null) {
            return;
        }

        Renderer.cancelRendering(this);
        int priority = 0;

        for (int newPage : toBeRendered) {
            if (newPage < 0 || newPage >= song.pageOrder.size()) {
                continue;
            }
            renderFailed.remove(new Integer(newPage));
            Renderer.JobRequest request = new Renderer.JobRequest(this, newPage, priority++, MainApp.screenRot.getSize(displaySize.getSize()));
            Renderer.requestRendering(request, song.pageOrder.get(newPage));
            this.waitingForImage = true;
        }
        toBeRendered.clear();
    }

    private void renderNext() {
        if (song == null || waitingForImage) {
            return;
        }
        if (toBeRendered.size() > 0) {
            int newPage = toBeRendered.remove(0);
            if (newPage < 0 || newPage >= song.pageOrder.size()) {
                return;
            }
            renderFailed.remove(new Integer(newPage));
            Renderer.JobRequest request = new Renderer.JobRequest(this, newPage, Math.abs(page - newPage), MainApp.screenRot.getSize(displaySize.getSize()));
            Renderer.requestRendering(request, song.pageOrder.get(newPage));
            this.waitingForImage = true;
        }
    }

    private void showPage(int page) {
        cleanCache(page);
        repopulateCache(page);
        paintDblBuf();
    }

    /**
     * The page transition animation
     * @param from
     * @param to
     */
    public void startShift(final double from, final double to) {
        final SplineInterpolator si = new SplineInterpolator(0.0f, 0.8f, 1.0f, 0.8f);
        if (anim != null && anim.isRunning()) {
            anim.stop();
        }

        anim = new Animator(300, new TimingTargetAdapter() {

            @Override
            public void begin() {
                setIgnoreRepaint(true);
            }

            @Override
            public void timingEvent(float fraction) {
                pageShift = (float) from + (float) (to - from) * si.interpolate(fraction);
                page = (int) Math.floor((double) pageShift);
                animPaint();
            }

            @Override
            public void end() {
                pageShift = (float) (from + (to - from));
                page = (int) Math.floor((double) pageShift);
                setIgnoreRepaint(false);
                showPage(page);
            }
        });
        anim.start();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
