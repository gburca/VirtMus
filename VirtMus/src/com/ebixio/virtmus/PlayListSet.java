/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus;

import com.ebixio.util.Log;
import com.ebixio.util.PropertyChangeSupportUnique;
import com.ebixio.util.WeakPropertyChangeListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import org.openide.util.NbPreferences;

/**
 * The set of all the PlayLists that VirtMus knows about.
 * @author gburca
 */
public class PlayListSet implements PreferenceChangeListener {
    private static PlayListSet instance;
    public final List<PlayList> playLists = Collections.synchronizedList(new ArrayList<PlayList>());
    
    /** If this is true, PROP_ALL_PL_LOADED has already been fired. */
    public boolean allPlayListsLoaded = false;
    
    private PropertyChangeSupportUnique propertyChangeSupport;
    public static final String PROP_ALL_PL_LOADED   = "allPlayListsLoaded";
    public static final String PROP_NEW_PL_ADDED    = "newPlayListAdded";
    public static final String PROP_PL_DELETED      = "playListDeleted";
    
    private PlayListSet() {
        
    }
    
    private void init() {
        Preferences pref = NbPreferences.forModule(MainApp.class);
        pref.addPreferenceChangeListener(this);
        propertyChangeSupport = new PropertyChangeSupportUnique(this);
        addAllPlayLists(false);
    }

    /**
     * @return A new PlayListSet instance.
     */
    public static synchronized PlayListSet findInstance() {
        if (instance == null) {
            instance = new PlayListSet();
            instance.init();
        }
        return instance;
    }
    
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
        MainApp.setStatusText("Save All finished.");
    }
        
    public boolean replacePlayList(PlayList replace, PlayList with) {
        synchronized (playLists) {
            int idx = playLists.lastIndexOf(replace);
            if (idx < 0) {
                return false;
            } else {
                playLists.remove(idx);
                playLists.add(idx, with);
                this.fire(PROP_NEW_PL_ADDED, replace, with);
                return true;
            }
        }
    }
    
    /** Adds a new PlayList to the set.
     * @param pl The PlayList to add
     * @return true if it was successfully added (if not null). */
    public boolean addPlayList(PlayList pl) {
        if (pl != null) {
            synchronized(playLists) {
                playLists.add(pl);
            }
            this.fire(PROP_NEW_PL_ADDED, null, pl);
            return true;
        }
        return false;
    }
    
    public boolean deletePlayList(PlayList pl) {
        if (pl.type != PlayList.Type.Normal
                || pl.getSourceFile() == null) return false;
        
        synchronized(playLists) {
            playLists.remove(pl);
        }
        
        try {
            if (pl.getSourceFile().delete()) {
                fire(PROP_PL_DELETED, pl, null);
                return true;
            }
        } catch (Exception e) {
            // Ignore exception
        }
        
        return false;
    }
    
    /**
     * Finds all the PlayLists on disk and loads them.
     * @param clearSongs If true, discards all songs so they get re-loaded when
     * the PlayList is re-created. This would effectively refresh everything.
     */
    public void addAllPlayLists(final boolean clearSongs) {
        MainApp.setStatusText("Re-loading all PlayLists");
        Thread t = new AddPlayLists(NbPreferences.forModule(MainApp.class), clearSongs);
        t.start();
    }
    

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        switch (evt.getKey()) {
            case MainApp.OptSongDir:
                Log.log("Preference SongDir changed");
                if (isDirty()) {
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
                synchronized(playLists) {
                    playLists.get(1).addAllSongs(new File(evt.getNewValue()), true);
                }
                break;
            case MainApp.OptPlayListDir:
                addAllPlayLists(false);
                break;
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc=" Property Change Listener ">
    /** Listeners will be notified of any changes to the set of PlayLists. */
    public void addPropertyChangeListener (PropertyChangeListener pcl) {
        synchronized(propertyChangeSupport) {
            if (pcl instanceof WeakPropertyChangeListener) {
                propertyChangeSupport.addPropertyChangeListener(pcl);
            } else {
                propertyChangeSupport.addPropertyChangeListener(new WeakPropertyChangeListener(pcl, this));
            }
        }
    }
    
    /** Listeners will be notified of changes to the set of PlayLists. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
        synchronized(propertyChangeSupport) {
            if (pcl instanceof WeakPropertyChangeListener) {
                propertyChangeSupport.addPropertyChangeListener(propertyName, pcl);
            } else {
                propertyChangeSupport.addPropertyChangeListener(propertyName, new WeakPropertyChangeListener(pcl, this));
            }
        }
    }
    
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        synchronized(propertyChangeSupport) {
            propertyChangeSupport.removePropertyChangeListener(pcl);
        }
    }
    
    private void fire(String propertyName, Object old, Object nue) {
        synchronized(propertyChangeSupport) {
            propertyChangeSupport.firePropertyChange(propertyName, old, nue);
        }
    }
    // </editor-fold>
    
    
    class AddPlayLists extends Thread {
        Preferences pref;
        boolean clearSongs;
        
        public AddPlayLists(Preferences pref, boolean clearSongs) {
            this.pref = pref;
            this.clearSongs = clearSongs;
            
            setName("addPlayLists");
            setPriority(Thread.MIN_PRIORITY);
        }
        
        @Override
        public void run() {
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

            synchronized (playLists) {
                playLists.clear();

                // Discard all the songs so they get re-loaded when the playlist is re-created
                if (clearSongs) Song.clearInstantiated();

                pl = new PlayList("Default Play List");
                pl.type = PlayList.Type.Default;
                playLists.add(pl);
                fire(PROP_NEW_PL_ADDED, null, pl);

                File dir = new File(pref.get(MainApp.OptPlayListDir, ""));
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
                            fire(PROP_NEW_PL_ADDED, null, pl);
                        }
                    }
                }

                pl = new PlayList("All Songs");
                pl.type = PlayList.Type.AllSongs;
                pl.addAllSongs(new File(pref.get(MainApp.OptSongDir, "")), true);
                playLists.add(pl);
                fire(PROP_NEW_PL_ADDED, null, pl);

                LogRecord rec = new LogRecord(Level.INFO, "VIRTMUS_PLAYLISTS");
                rec.setParameters(new Object[] {playLists.size()});
                Log.uiLog(rec);

                Collections.sort(playLists);
                
                fire(PROP_ALL_PL_LOADED, null, playLists);
                allPlayListsLoaded = true;
                MainApp.setStatusText("Finished loading all PlayLists");
            }
        }
    }    
}
