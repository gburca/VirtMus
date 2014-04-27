/*
 * MainApp.java
 *
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

package com.ebixio.virtmus;

import com.ebixio.util.Log;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.awt.StatusDisplayer;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public final class MainApp implements ChangeListener {
    
    private static MainApp instance;
    public final List<PlayList> playLists = Collections.synchronizedList(new ArrayList<PlayList>());
    private static Date lastTime = new Date();
    private final transient Set<ChangeListener> plListeners = new HashSet<>();
    
    public static final String VERSION = "3.20";
    
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
        int degrees() {
            switch(this) {
                case Clockwise_90: return 90;
                case Clockwise_180: return 180;
                case Clockwise_270: return 270;
                case Clockwise_0:
                default:
                    return 0;
            }
        }
        public AffineTransform getTransform(Dimension d) {
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
    public static final String OptUseOpenGL         = "UseOpenGL";    
    public static final String OptSvgEditor         = "SvgEditor";
    public static final String OptInstallId         = "InstallId";
    public static final String OptLogVersion        = "LogVersion";

    public static final Object playListPrefLock = new Object();
    
    /** Creates a new instance of MainApp */
    private MainApp() {
        Log.configUiLog();
        //Log.enableDebugLogs();
        Log.log("MainApp::MainApp start");
        
        System.getProperties().put("org.icepdf.core.scaleImages", "false");
        System.getProperties().put("org.icepdf.core.awtFontLoading", "true");
        
        //System.setProperty("nb.show.statistics.ui", "true");
        //System.getProperties().put("nb.show.statistics.ui", "true");

        Preferences pref = NbPreferences.forModule(MainApp.class);

        screenRot = Rotation.valueOf( pref.get(OptScreenRot, Rotation.Clockwise_0.toString()) );
        scrollDir = ScrollDir.valueOf( pref.get(OptPageScrollDir, ScrollDir.Horizontal.toString()) );
        
        setupListeners(pref);
        
        addAllPlayListsThreaded(pref, false);
        
        Log.log("MainApp::MainApp finished");
    }

    private void setupListeners(Preferences pref) {
        pref.addPreferenceChangeListener(new PreferenceChangeListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                if (evt.getKey().equals(OptSongDir)) {
                    Log.log("Preference SongDir changed");
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
                    synchronized(playListPrefLock) {
                        playLists.get(1).addAllSongs(new File(evt.getNewValue()), true);
                    }
                } else if (evt.getKey().equals(OptPlayListDir)) {
                    Preferences pref = NbPreferences.forModule(MainApp.class);
                    addAllPlayListsThreaded(pref, false);
                }
            }
        });

        
        PropertyChangeListener pcl = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                    Node[] nodes = (Node[]) evt.getNewValue();
                    if (nodes.length == 0) {return;}
                    Lookup l = nodes[0].getLookup();
                    Collection songs = l.lookupResult(Song.class).allInstances();
                    if (!songs.isEmpty()) {
                        Song s = (Song) songs.iterator().next();
                        displayFile("Song: ", s.getSourceFile());
                    } else {
                        // Let's see if we have a playlist
                        Collection playlists = l.lookupResult(PlayList.class).allInstances();
                        if (!playlists.isEmpty()) {
                            PlayList p = (PlayList) playlists.iterator().next();
                            displayFile("PlayList: ", p.getSourceFile());
                        }
                    }
                }
            }
            private void displayFile(String pre, File f) {
                if (f != null) {
                    StatusDisplayer.getDefault().setStatusText(pre + f.getAbsolutePath());
                } else {
                    StatusDisplayer.getDefault().setStatusText(pre + "no file");
                }
            }
        };

        // To update the status bar when songs/playlists are selected
        CommonExplorers.MainExplorerManager.addPropertyChangeListener(pcl);
        CommonExplorers.TagsExplorerManager.addPropertyChangeListener(pcl);
    }
    
    /**
     * Re-reads all the PlayLists and Songs from the disc
     */
    public void refresh() {
        addAllPlayListsThreaded(NbPreferences.forModule(MainApp.class), true);
    }
    
    void addAllPlayListsThreaded(final Preferences pref, final boolean clearSongs) {
        Thread t = new Thread() {
            @Override
            public void run() {
                addAllPlayLists(pref, clearSongs);
            }
        };
        
        t.setName("addAllPlayLists");
        // We want the GUI to be responsive and show the updates instead of being
        // stuck with the splash-screen
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }
    synchronized void addAllPlayLists(Preferences pref, boolean clearSongs) {
        //log("MainApp::addAllPlayLists thread: " + Thread.currentThread().getName());

        setStatusText("Re-loading all PlayLists");

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
        
        synchronized (playListPrefLock) {
            playLists.clear();
            
            // Discard all the songs so they get re-loaded when the playlist is re-created
            if (clearSongs) Song.clearInstantiated();

            pl = new PlayList("Default Play List");
            pl.type = PlayList.Type.Default;
            playLists.add(pl);

            File dir = new File(pref.get(OptPlayListDir, ""));
            if (dir.exists() && dir.canRead() && dir.isDirectory()) {

                FilenameFilter filter = new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".playlist.xml");
                    }
                };

                for (File f : Utils.listFiles(dir, filter, true)) {
                    pl = PlayList.deserialize(f);
                    if (pl != null) {
                        playLists.add(pl);
                        this.notifyPLListeners();
                    }
                }
            }

            pl = new PlayList("All Songs");
            pl.type = PlayList.Type.AllSongs;
            pl.addAllSongs(new File(pref.get(OptSongDir, "")), true);
            playLists.add(pl);
            
            LogRecord rec = new LogRecord(Level.INFO, "VIRTMUS_PLAYLISTS");
            rec.setParameters(new Object[] {playLists.size()});
            Log.uiLog(rec);

            Collections.sort(playLists);
        }
        
        this.notifyPLListeners();
        
        setStatusText("Finished loading all PlayLists");
    }
    
    /** Handle setting the status bar text from non-EDT threads
     * @param msg The status bar text to display. */
    public static void setStatusText(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StatusDisplayer.getDefault().setStatusText(msg);
            }
        });
    }
    
    public static synchronized MainApp findInstance() {
        if (instance == null) {
            instance = new MainApp();
        }
        return instance;
    }
    
    @Override
    public void stateChanged(ChangeEvent arg0) {
        
    };
    
    public boolean isDirty() {
        synchronized (playLists) {
            for (PlayList pl : playLists) {
                if (pl.isDirty()) {
                    Log.log("Dirty PlayList: " + pl.getName());
                    return true;
                }
                synchronized (pl.songs) {
                    for (Song s : pl.songs) {
                        if (s.isDirty()) {
                            Log.log("Dirty Song: " + s.getName());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    public void saveAll() {
        synchronized(playLists) {
            for (PlayList pl: playLists) pl.saveAll();
        }
        StatusDisplayer.getDefault().setStatusText("Save All finished.");
    }
    
    public static String getElapsedTime() {
        StringBuilder res = new StringBuilder();
        Date thisTime = new Date();
        long elapsed = thisTime.getTime() - lastTime.getTime();
        
        res.append("Last time ").append(lastTime.toString());
        res.append(" now ").append(thisTime.toString());
        res.append(" Elapsed ").append((new Long(elapsed)).toString()).append("ms");
        
        lastTime = thisTime;
        return res.toString();
    }
    
    public boolean replacePlayList(PlayList replace, PlayList with) {
        synchronized (playLists) {
            int idx = playLists.lastIndexOf(replace);
            if (idx < 0) {
                return false;
            } else {
                playLists.remove(idx);
                playLists.add(idx, with);
                notifyPLListeners();
                return true;
            }
        }
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
    
    /** Listeners will be notified of additions/deletions to the set of PlayLists. */
    public void addPLChangeListener(ChangeListener listener) {
        if (!plListeners.contains(listener)) plListeners.add(listener);
    }
    public void removePLChangeListener(ChangeListener listener) {
        plListeners.remove(listener);
    }
    public void notifyPLListeners() {
        ChangeEvent ev = new ChangeEvent(this);
        ChangeListener[] cls = plListeners.toArray(new javax.swing.event.ChangeListener[0]);
        for (ChangeListener cl: cls) cl.stateChanged(ev);
    }

}
