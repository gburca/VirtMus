/*
 * LiveWindowJOGL.java
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

import com.ebixio.util.Log;
import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Vector;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.GL;
import javax.media.opengl.GL.*;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.WindowConstants;
import org.jdesktop.animation.timing.*;


/**
 *
 * @author gburca
 */
public class LiveWindowJOGL extends javax.swing.JFrame
        implements GLEventListener, Renderer.JobRequester {
    
    private static final GLU glu = new GLU();
    final com.sun.opengl.util.Animator animator;

    private TextRenderer textRenderer;
    private String fpsText;
    private int fpsWidth;
    private long startTime;
    private int frameCount;
    private DecimalFormat format = new DecimalFormat("####.00");
    
    Rectangle displaySize = new Rectangle(Utils.getScreenSize());   // Physical display size
    Dimension windowSize = new Dimension(0, 0);                     // OpenGL window size
    Dimension windowSizeR = new Dimension(0, 0);                    // OpenGL window size once rotated as needed
    final double cameraDist = 1000d;                                // Camera distance from pages
    private Song song = null;
    boolean waitingForImage = false;
    boolean fullyPainted = false;
    private int page = 0;
    
    final int maxPrevCache = 3;
    final int maxNextCache = 4;
    Hashtable<Integer, TexturePage> pageCache = new Hashtable<Integer, TexturePage>(3);
    Vector<Integer> toBeRendered = new Vector<Integer>(3);
    
    private Animator pageShiftAnim;
    private float pageShift = 0;
    private float pageShiftAmount = 0;
    
    /** Creates a new instance of LiveWindowJOGL */
    public LiveWindowJOGL() {
        GLCapabilities caps = new GLCapabilities();
        Log.log("JOGL caps: " + caps.toString());

        GLCanvas canvas = new GLCanvas(caps);
        canvas.addGLEventListener(this);

        this.add(canvas, BorderLayout.CENTER);
        setCursor(Utils.getInvisibleCursor());
        setResizable(false);
        setUndecorated(true);
        setSize(displaySize.getSize());
        //this.setSize(displaySize.width - 200, displaySize.height - 300);
        this.setLocationRelativeTo(null);
        this.setLocation(0, 0);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        KeyAdapter ka = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                formKeyPressed(evt);
            }
        };
        addKeyListener(ka);
        canvas.addKeyListener(ka);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    KeyEvent evt = new KeyEvent(null, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_PAGE_DOWN, KeyEvent.CHAR_UNDEFINED);
                    formKeyPressed(evt);
                } else {
                    KeyEvent evt = new KeyEvent(null, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_PAGE_UP, KeyEvent.CHAR_UNDEFINED);
                    formKeyPressed(evt);
                }
            }
        });

        initTimers();

        animator = new com.sun.opengl.util.Animator(canvas);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Run this on another thread than the AWT event queue to
                // make sure the call to Animator.stop() completes before
                // exiting
                new Thread(new Runnable() {
                    public void run() {
                        animator.stop();
                    }
                }).start();
            }
        });
        animator.start();
    }
    
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LiveWindowJOGL().setVisible(true);
            }
        });
    }

    // <editor-fold desc=" GLEventListener interface ">
    /** One time call when OpenGL is initialized */
    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glShadeModel(GL.GL_SMOOTH);
        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        // Set erase color
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        // Set drawing color
        gl.glColor3f(1.0f, 1.0f, 1.0f);

        initTextRenderer();
        windowSize = new Dimension(drawable.getWidth(), drawable.getHeight());
        windowSizeR = MainApp.screenRot.getSize(windowSize);
        
        //IntBuffer i = IntBuffer.wrap(new int[1]);
        //gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, i);
        
        showPage(page);
    }

    /** Requests component to draw itself */
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        
        // gluLookAt (from, to, up-direction)
        //glu.gluLookAt(0d, 0d, cameraDist,   0d, 0d, 0d,   0d, 1d, 0d);
        glu.gluLookAt(pageShift, 0, cameraDist, pageShift, 0, 0, 0, 1d, 0);
        
        if (MainApp.scrollDir == MainApp.ScrollDir.Vertical) {
            renderPageVertical(drawable);
        } else {
            renderPageHorizontal(drawable);
        }
        
        if (gl.glGetError() != GL.GL_NO_ERROR) {
            Log.log("OpenGL error: " + gl.glGetError());
        }
        
        displayFPSText(drawable);
            
        gl.glFlush();
    }

    /** Signals us that the component's location or size has been changed.
     * Also called after init(). */
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL gl = drawable.getGL();

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        boolean ortho = false;
        if (ortho) {
            //glu.gluOrtho2D(-width, width, -height, height);
            gl.glOrtho(0, width, -height/2, height/2, 500d, 1200d);
        } else {
            double aspectRatio = (double)width / (double)height;
            double angle = Math.atan((double)height / 2d / cameraDist) * 2;
            angle *= 180d / Math.PI;    // Convert radians to degrees
            glu.gluPerspective(angle, aspectRatio, 500.0, 1200.0);
        }
        
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /** Signals us that the display mode or device has changed (color depth, etc...).
     * Also, that the window has been dragged from one monitor (a "device") to another. */
    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" FPS Text ">
    private void initTextRenderer() {
        // Create the text renderer
        Font font = new Font("SansSerif", Font.BOLD, 24);
        textRenderer = new TextRenderer(font, true, false);
    }

    private void displayFPSText(GLAutoDrawable drawable) {
        if (++frameCount == 100) {
            long endTime = System.currentTimeMillis();
            float fps = 100.0f / (float) (endTime - startTime) * 1000;
            frameCount = 0;
            startTime = System.currentTimeMillis();

            fpsText = "FPS: " + format.format(fps);
            if (fpsWidth == 0) {
                // Place it at a fixed offset wrt the upper right corner
                fpsWidth = (int)
                    textRenderer.getBounds("FPS: 10000.00").getWidth();
            }
        }

        if (fpsWidth == 0) {
            return;
        }

        // Calculate text location and color
        int x = drawable.getWidth() - fpsWidth - 5;
        int y = drawable.getHeight() - 30;
        float c = 0.55f;

        // Render the text
        textRenderer.beginRendering(drawable.getWidth(), drawable.getHeight());
        textRenderer.setColor(c, c, c, c);
        textRenderer.draw(fpsText, x, y);
        textRenderer.endRendering();
    }
    // </editor-fold>

    private void renderPageVertical(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glPushMatrix();
        if (MainApp.screenRot != MainApp.Rotation.Clockwise_0) {
            gl.glRotatef(-MainApp.screenRot.degrees(), 0, 0, 1);
        }
        
        TexturePage tp1, tp2, tp3;
        
        synchronized(pageCache) { tp1 = pageCache.get(page); }
        if (tp1 != null && tp1.getTexture() != null) {
            //gl.glTranslatef(-tp1.dim.width / 2, (windowSizeR.height - tp1.dim.height) / 2 + (pageShift * tp1.dim.height), 0);
            gl.glTranslatef(-tp1.dim.width / 2, (windowSizeR.height - tp1.dim.height) / 2, 0);
            renderPage(drawable, tp1);
            
            synchronized(pageCache) { tp2 = pageCache.get(page + 1); }
            if (tp2 != null && tp2.getTexture() != null) {
                gl.glTranslatef(0, -tp2.dim.height, 0);
                renderPage(drawable, tp2);
                
                synchronized(pageCache) { tp3 = pageCache.get(page + 2); }
                if (tp3 != null && tp3.getTexture() != null) {
                    gl.glTranslatef(0, -tp3.dim.height, 0);
                    renderPage(drawable, tp3);
                }
            }
        }
        
        gl.glPopMatrix();
        
    }
    private void renderPageHorizontal(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        gl.glPushMatrix();
        if (MainApp.screenRot != MainApp.Rotation.Clockwise_0) {
            gl.glRotatef(-MainApp.screenRot.degrees(), 0, 0, 1);
        }
        
        TexturePage tp1, tp2, tp3, tp4;
        
        synchronized(pageCache) { tp1 = pageCache.get(page); }
        if (tp1 != null && tp1.getTexture() != null) {
            //gl.glTranslatef(-windowSizeR.width / 2 - (pageShift * tp1.dim.width), 0, 0);
            gl.glTranslatef(-windowSizeR.width / 2, 0, 0);
            renderPage(drawable, tp1);
            
            synchronized(pageCache) { tp2 = pageCache.get(page + 1); }
            if (tp2 != null && tp2.getTexture() != null) {
                gl.glTranslatef(tp1.dim.width, 0, 0);
                renderPage(drawable, tp2);
                
                synchronized(pageCache) { tp3 = pageCache.get(page + 2); }
                if (tp3 != null && tp3.getTexture() != null) {
                    gl.glTranslatef(tp2.dim.width, 0, 0);
                    renderPage(drawable, tp3);
                    
                    synchronized(pageCache) { tp4 = pageCache.get(page + 3); }
                    if (tp4 != null && tp4.getTexture() != null) {
                        gl.glTranslatef(tp3.dim.width, 0, 0);
                        renderPage(drawable, tp4);
                    }
                }
            }
        }
        
        gl.glPopMatrix();
        
    }
    
    /** All pages are placed on the XY plane with Z=0.
     * What moves, the model (music pages) or the view/camera (the laptop display)?
     * We can place the first page with its lower-left corner at 0,0 and string the rest of the
     * pages to the right. If the user decides to start off with page 7, we'll have to figure
     * out the width of pages 1-6 so we know at what X-coordinate to start page 7 at.
     * Alternatively, we can keep the view fixed at 0,0 and move the model so that the current
     * page shows up at the left of the screen. This way we don't need to know the width of all
     * preceeding pages. We always place the current page at the same X,Y.
     * */
    private float renderPage(GLAutoDrawable drawable, TexturePage tp) {
        GL gl = drawable.getGL();
        Texture texture = tp.getTexture();
        if (texture == null) return 0;

        TextureCoords tc = texture.getImageTexCoords();
        float tx1 = tc.left();
        float ty1 = tc.top();
        float tx2 = tc.right();
        float ty2 = tc.bottom();
        
        float w = tp.dim.width;
        float h2 = (float)tp.dim.height / 2;
        
        texture.enable();
        texture.bind();
        gl.glBegin(GL.GL_QUADS);
        gl.glColor4f(1f, 1f, 1f, 1f);
        gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(0,  h2, 0f);
        gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(w,  h2, 0f);
        gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(w, -h2, 0f);
        gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(0, -h2, 0f);
        gl.glEnd();
        texture.disable();
        
        return w;
    }
    
    protected void initTimers() {
        //pageShiftAnim = PropertySetter.createAnimator(1000, this, "pageShift", 0f, 1f);
        pageShiftAnim = new Animator(500, new TimingTargetAdapter() {
            @Override
            public void timingEvent(float fraction) { setPageShift(pageShiftAmount * fraction); }
            @Override
            public void end() {
                setPageShift(pageShiftAmount);
                page++;
                setPageShift(0);
               java.awt.EventQueue.invokeLater(new Runnable() {
                   public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                        }
                       showPage(page);
                   }
               });
           }
        });
    }
    public float getPageShift() { return pageShift; }
    public void setPageShift(float shift) { this.pageShift = shift; Log.log("Shift: " + shift);}
    
    // <editor-fold defaultstate="collapsed" desc=" Rendering Cache ">
    private void renderNext() {
        if (song == null || waitingForImage) return;
        if (toBeRendered.size() > 0) {
            int newPage = toBeRendered.remove(0);
            if (newPage < 0 || newPage >= song.pageOrder.size()) return;
            Renderer.JobRequest request = new Renderer.JobRequest(
                    this, newPage, Math.abs(page - newPage),
                    MainApp.screenRot.getSize(windowSize));
            
            Renderer.requestRendering(request, song.pageOrder.get(newPage));
            this.waitingForImage = true;
        }
    }

    @Override
    public void renderingComplete(MusicPage mp, Renderer.JobRequest request) {
            waitingForImage = false;
            if (song == null) return;
            
            if (request.pageNr >= 0 && request.requester == this) {
                synchronized(pageCache) {
                    pageCache.put(request.pageNr, new TexturePage(Renderer.getRenderedImage(this)));
                }
                //if (request.pageNr == page || !fullyPainted) repaint();
            }
            cleanCache();
            renderNext();
    }
    
    private void repopulateCache(int page) {
        if (song == null) return;
        int range = Math.max(maxPrevCache, maxNextCache);
        toBeRendered.clear();
        
        synchronized(pageCache) {
        if (!pageCache.containsKey(page)) toBeRendered.add(page);
        
        // Render closest pages first, then next removed, etc...
        for (int i = 1; i <= range; i++) {
            if (i <= maxNextCache && !pageCache.containsKey(page + i)) {
                if (page + i < song.pageOrder.size()) toBeRendered.add(page + i);
            }
            if (i <= maxPrevCache && !pageCache.containsKey(page - i)) {
                if (page - i >= 0) toBeRendered.add(page - i);
            }
        }
        }
        
        renderNext();
    }

    private void cleanCache() {
        cleanCache(page);
    }
    private void cleanCache(int currentPage) {
        int lastPage = 0;
        
        synchronized(pageCache) {
        for (int i = 0; i < currentPage - maxPrevCache; i++) {
            if (pageCache.containsKey(i)) {
                pageCache.remove(i);
                Log.log("LiveWindow: removed page " + i);
            }
        }
        for (int i: pageCache.keySet()) {
            if (i > lastPage) lastPage = i;
        }
        for (int i = currentPage + maxNextCache + 1; i < lastPage; i++) {
            if (pageCache.containsKey(i)) {
                pageCache.remove(i);
                Log.log("LiveWindow: removed page " + i);
            }
        }
        }
    }
    // </editor-fold>
    
    protected void formKeyPressed(java.awt.event.KeyEvent evt) {
        switch(evt.getKeyCode()) {
        case KeyEvent.VK_ESCAPE:
            animator.stop();
            this.dispose();
            break;
        case KeyEvent.VK_1:
            showPage(page = 0);
            break;
        case KeyEvent.VK_PAGE_UP:
            showPage(--page);
            break;
        case KeyEvent.VK_PAGE_DOWN:
        case KeyEvent.VK_SPACE:
            TexturePage tp;
            synchronized(pageCache) { tp = pageCache.get(page); }
            if (tp != null) {
                pageShiftAmount = tp.dim.width;
            }
            this.pageShiftAnim.start();
            //showPage(++page);
            break;
        default:
            break;
        }
    }
    
    private void showPage(int page) {
        cleanCache(page);
        repopulateCache(page);
        //setPageShift(0);
        //this.repaint();
    }

    // <editor-fold defaultstate="collapsed" desc=" Set song/playlist ">
    public void setSong(Song song) {
        if (song == null || song.pageOrder.size() == 0) return;
        setSong(song, song.pageOrder.get(0));
    }
    public void setSong(Song song, MusicPage startingPage) {
        this.song = song;
        page = song.pageOrder.indexOf(startingPage);
        //showPage(page);
    }
    public void setPlayList(PlayList playList) {
        Song s = new Song();
        synchronized (playList.songs) {
            for (Song plSong: playList.songs) {
                synchronized(plSong.pageOrder) {
                    for (MusicPage mp: plSong.pageOrder) s.pageOrder.add(mp);
                }
            }
        }
        setSong(s);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" TexturePage class ">
    private class TexturePage {
        protected Texture texture = null;
        public Dimension dim;
        protected BufferedImage img;
        protected boolean converted = false;
        
        public TexturePage(BufferedImage img) {
            this.img = img;
            this.dim = new Dimension(img.getWidth(), img.getHeight());
        }
        
        /** This can only be called from a thread that has an OpenGL context or it will throw an exception. */
        public Texture getTexture() {
            if (!converted) {
                converted = true;
                texture = TextureIO.newTexture(img, true);
                img = null; // no longer needed

                if (texture != null) {
                    texture.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
                    texture.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
                }
            }
            return texture;
        }
    }
    // </editor-fold>
}
