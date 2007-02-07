/*
 * MainApp.java
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
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.joda.time.DateTime;
import org.openide.LifecycleManager;
import org.openide.awt.StatusDisplayer;
import org.openide.explorer.ExplorerManager;
import java.util.logging.*;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 *
 * @author gburca
 */
public class MainApp implements ExplorerManager.Provider, ChangeListener {
    
    private static MainApp instance;
    public Vector<PlayList> playLists = new Vector<PlayList>();
    private transient ExplorerManager manager = new ExplorerManager();
    private static Logger logger = Logger.getLogger("com.ebixio.virtmus");
    private static DateTime lastTime = new DateTime();
    private transient Set<ChangeListener> plListeners = new HashSet<ChangeListener>();
    public transient SaveAllAction saveAllAction = null;
    
    // TODO: Obtain this from OpenIDE-Module-Implementation-Version in manifest.mf
    public static final String VERSION = "0.53";
    private static final boolean RELEASED = true;   // Used to disable logging
    
    public static enum Rotation {
        Clockwise_0, Clockwise_90, Clockwise_180, Clockwise_270;
        // <editor-fold defaultstate="collapsed" desc=" Rotation Behaviors ">
        double radians() {
            switch(this) {
                case Clockwise_90: return Math.PI / 2;
                case Clockwise_180: return Math.PI;
                case Clockwise_270: return Math.PI / 2 * 3;
                case Clockwise_0:
                default:
                    return 0;
            }
        }        
        AffineTransform getTransform(Dimension d) {
            switch (this) {
                case Clockwise_90:
                    /**
                     * Writes on surface of dimension d at 90 degrees clockwise
                     * [ 0  -1  width ]
                     * [ 1   0    0   ]
                     * [ 0   0    1   ]
                     * 
                     * x' = width - y
                     * y' = x
                     */
                    return new AffineTransform(0, 1, -1, 0, d.width, 0);
                case Clockwise_180:
                    /**
                     * Writes upside down
                     * [ -1  0  width ]
                     * [  0 -1  height]
                     * [  0  0     1  ]
                     * 
                     * x' = width - x
                     * y' = height - y
                     */
                    return new AffineTransform(-1, 0, 0, -1, d.width, d.height);
                case Clockwise_270:
                    /**
                     * [  0  1    0    ]
                     * [ -1  0  height ]
                     * [  0  0    1    ]
                     * 
                     * x' = y
                     * y' = height - x
                     */
                    return new AffineTransform(0, -1, 1, 0, 0, d.height);
                case Clockwise_0:
                default:
                    return new AffineTransform();
            }
        }
        /** Rotates the dimension (if needed) */
        Dimension getSize(Dimension d) {
            switch(this) {
                case Clockwise_90:
                case Clockwise_270:
                    return new Dimension(d.height, d.width);
                default:
                    return new Dimension(d);
            }
        }
        // </editor-fold>        
    }
    public static enum ScrollDir { Vertical, Horizontal }
    public static Rotation screenRot;
    public static ScrollDir scrollDir;
    
    public static final String OptPlayListDir       = "PlayListDirectory";
    public static final String OptSongDir           = "SongDirectory";
    public static final String OptScreenRot         = "LiveScreenOrientation";
    public static final String OptPageScrollAmount  = "PageScrollPercentage";
    public static final String OptPageScrollDir     = "ScrollDirection";

    /** Creates a new instance of MainApp */
    private MainApp() {
        log("MainApp::MainApp start");

        Preferences pref = NbPreferences.forModule(MainApp.class);

        screenRot = Rotation.valueOf( pref.get(OptScreenRot, Rotation.Clockwise_0.toString()) );
        scrollDir = ScrollDir.valueOf( pref.get(OptPageScrollDir, ScrollDir.Horizontal.toString()) );
        
        pref.addPreferenceChangeListener(new PreferenceChangeListener() {
            public void preferenceChange(PreferenceChangeEvent evt) {
                if (evt.getKey().equals(OptSongDir)) {
                    // TODO: Is this causing problems by not being synchronized?
                    log("Preference SongDir changed");
                    if (MainApp.findInstance().isDirty()) {
                        int returnVal = JOptionPane.showConfirmDialog(null,
                                "You have unsaved changes. Save all changes before loading new song directory?",
                                "Changes exist in currently loaded playlists or songs.", JOptionPane.YES_NO_CANCEL_OPTION);
                        switch (returnVal) {
                            case JOptionPane.YES_OPTION:    saveAll();   break;
                            case JOptionPane.CANCEL_OPTION: return;
                            case JOptionPane.NO_OPTION:
                            default: break;
                        }
                    }
                    playLists.get(1).addAllSongs(new File(evt.getNewValue()), true);
                } else if (evt.getKey().equals(OptPlayListDir)) {
                    Preferences pref = NbPreferences.forModule(MainApp.class);
                    addAllPlayLists(pref);
                }
            }
        });

        addAllPlayLists(pref);
        
        log("MainApp::MainApp finished");
    }
    
    void addAllPlayLists(Preferences pref) {
        log("MainApp::addAllPlayLists thread: " + Thread.currentThread().getName());
        PlayList pl;
        
        if (isDirty()) {
            int returnVal = JOptionPane.showConfirmDialog(null,
                    "You have unsaved changes. Save all changes before loading new playlists?",
                    "Changes exist in currently loaded playlists or songs.", JOptionPane.YES_NO_CANCEL_OPTION);
            switch (returnVal) {
                case JOptionPane.YES_OPTION:    saveAll();   break;
                case JOptionPane.CANCEL_OPTION: return;
                case JOptionPane.NO_OPTION:
                default: break;
            }
        }
        playLists.clear();
        
        pl = new PlayList("Default Play List");
        pl.type = PlayList.Type.Default;
        playLists.add(pl);
        
        pl = new PlayList("All Songs");
        pl.type = PlayList.Type.AllSongs;
        pl.addAllSongs(new File(pref.get(OptSongDir, "")), true);
        playLists.add(pl);

        File dir = new File(pref.get(OptPlayListDir, ""));
        if (dir.exists() && dir.canRead() && dir.isDirectory()) {
            for (File f: dir.listFiles()) {
                if (f.getName().endsWith(".playlist.xml")) {
                    pl = PlayList.deserialize(f);
                    if (pl != null) playLists.add(pl);
                }
            }
        }
        
        this.notifyPLListeners();
    }
    
    public static synchronized MainApp findInstance() {
        if (instance == null) {
            instance = new MainApp();
        }
        return instance;
    }
    
    public void stateChanged(ChangeEvent arg0) {
        saveAllAction.updateEnable();
    };
    
    public boolean isDirty() {
        for (PlayList pl: playLists) {
            if (pl.isDirty()) return true;
            for (Song s: pl.songs) {
                if (s.isDirty()) return true;
                for (MusicPage mp: s.pageOrder) {
                    if (mp.isDirty) return true;
                }
            }
        }
        return false;
    }
    public void saveAll() {
        for (PlayList pl: playLists) pl.saveAll();
        StatusDisplayer.getDefault().setStatusText("Save All finished.");
    }

    public void setExplorerManager(ExplorerManager manager) {
        this.manager = manager;
    }
    public ExplorerManager getExplorerManager() {
        return manager;
    }
    
    public static void log(String msg) {
        log(msg, Level.INFO, false);
    }
    public static void log(String msg, Level lev) {
        log(msg, lev, false);
    }
    public static void log(String msg, Level lev, boolean printStackDump) {
        if (RELEASED) return;
        logger.log(lev, getElapsedTime() + " - " + msg);
        if (printStackDump) {
            logger.log(lev, getStackTrace() + "\n");
        }
    }
    public static String getElapsedTime() {
        StringBuilder res = new StringBuilder();
        DateTime thisTime = new DateTime();
        long elapsed = thisTime.getMillis() - lastTime.getMillis();
        
        res.append("Last time " + lastTime.toTimeOfDay().toString() +
                " now " + thisTime.toTimeOfDay().toString() +
                " Elapsed " + (new Long(elapsed)).toString() + "ms");
        
        lastTime = thisTime;
        return res.toString();
    }
    
    public static String getStackTrace() {
        StringBuilder res = new StringBuilder();
        StackTraceElement[] ste = (new Throwable()).getStackTrace();
        for (StackTraceElement e: ste) {
            res.append("Class: " + e.getClassName());
            res.append(" Method: " + e.getMethodName());
            res.append(" Line: " + e.getLineNumber() + "\n");
        }
        return res.toString();
    }

    public boolean addPlayList() {
        PlayList pl = PlayList.open();
        if (pl != null) {
            playLists.add(pl);
            notifyPLListeners();
            return true;
        }
        return false;
    }
    
    public void addPLChangeListener(ChangeListener listener) {
        plListeners.add(listener);
    }
    public void removePLChangeListener(ChangeListener listener) {
        plListeners.remove(listener);
    }
    public void notifyPLListeners() {
        ChangeEvent ev = new ChangeEvent(this);
        ChangeListener[] cls = (ChangeListener[]) plListeners.toArray(new ChangeListener[0]);
        for (ChangeListener cl: cls) cl.stateChanged(ev);
    }

    
    /**
     * Default implementation of the LifecycleManager interface that knows
     * how to save all modified data and to exit safely.
     * 
     * We add this to the default lookup by creating a META-INF/services file.
     * 
     * We make sure this is the first one that is found by adding the "#position=10"
     * option in the META-INF/services file. See:
     * http://www.netbeans.org/project/www/download/dev/javadoc/org-openide-util/org/openide/util/doc-files/api.html
     *
     * @author gburca
     */
    public static final class VirtMusLifecycleManager extends LifecycleManager {
        /** Default constructor for lookup. */
        public VirtMusLifecycleManager() {}
        public void saveAll() {
            MainApp.findInstance().saveAll();
        }
        public void exit() {
            if (MainApp.findInstance().isDirty()) {
                int returnVal = JOptionPane.showConfirmDialog(null,
                        "You have unsaved changes. Return to the application to save the changes?",
                        "Unsaved changes exist.", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (returnVal == JOptionPane.YES_OPTION) return; //saveAll();
            }

            // Now we defer to the default org.netbeans.core.NbTopManager$NbLifecycleManager
            //Collection c = Lookup.getDefault().lookup(new Lookup.Template(LifecycleManager.class)).allInstances();
            Collection c = Lookup.getDefault().lookupAll(LifecycleManager.class);
            for (Iterator i = c.iterator(); i.hasNext(); ) {
                LifecycleManager lm = (LifecycleManager) i.next();
                if (lm != this) {
                    lm.exit();
                }
            }
            
            // This line should never execute, unless we couldn't find the default manager above
            System.exit(0);
        }
    }
}
