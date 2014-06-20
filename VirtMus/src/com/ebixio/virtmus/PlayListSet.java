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
package com.ebixio.virtmus;

import com.ebixio.util.Log;
import com.ebixio.util.PropertyChangeSupportUnique;
import com.ebixio.util.WeakPropertyChangeListener;
import com.ebixio.virtmus.options.Options;
import com.ebixio.virtmus.stats.StatsLogger;
import java.beans.PropertyChangeEvent;
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
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class PlayListSet implements PreferenceChangeListener, PropertyChangeListener {
    private static PlayListSet instance;
    public final List<PlayList> playLists = Collections.synchronizedList(new ArrayList<PlayList>());

    /** If this is true, PROP_ALL_PL_LOADED has already been fired. */
    public Boolean allPlayListsLoaded = new Boolean(false);
    private int playListsLoading = 0;

    private PropertyChangeSupportUnique propertyChangeSupport;
    private final Object pcsMutex = new Object();
    public static final String PROP_ALL_PL_LOADED   = "allPlayListsLoaded";
    public static final String PROP_ALL_SONGS_LOADED= "allSongsLoaded";
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
            playLists.add(pl);
            this.fire(PROP_NEW_PL_ADDED, null, pl);
            return true;
        }
        return false;
    }

    public boolean deletePlayList(PlayList pl) {
        if (pl.type != PlayList.Type.Normal
                || pl.getSourceFile() == null) return false;

        playLists.remove(pl);

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
            case Options.OptSongDir:
                Log.log("Preference SongDir changed");

                /* We need the synchronization because we could be in the middle
                of AddPlayLists.run() when the preferences are changed, and
                playLists.get(1) might not exist yet (or might be something
                other than AllSongs). */
                synchronized(AddPlayLists.class) {
                    if (!promptForSave("Save all changes before loading new song directory?")) return;
                    playLists.get(1).addAllSongs(new File(evt.getNewValue()), true);
                }
                break;
            case Options.OptPlayListDir:
                addAllPlayLists(false);
                break;
        }
    }

    /**
     *
     * @param msg
     * @return false if user selected "Cancel"
     */
    private boolean promptForSave(String msg) {
        if (isDirty()) {
            int returnVal = JOptionPane.showConfirmDialog(null,
                    "You have unsaved changes. " + msg,
                    "Changes exist in currently loaded playlists or songs.", JOptionPane.YES_NO_CANCEL_OPTION);
            switch (returnVal) {
                case JOptionPane.YES_OPTION:
                    saveAll();
                    break;
                case JOptionPane.CANCEL_OPTION:
                    return false;
                case JOptionPane.NO_OPTION:
                default:
                    break;
            }
        }
        return true;
    }

    // <editor-fold defaultstate="collapsed" desc=" Property Change Listener ">
    /** Listeners will be notified of any changes to the set of PlayLists.
     * @param pcl */
    public void addPropertyChangeListener (PropertyChangeListener pcl) {
        synchronized(pcsMutex) {
            propertyChangeSupport.addPropertyChangeListener(pcl);
        }
    }

    /** Listeners will be notified of changes to the set of PlayLists.
     * @param propertyName
     * @param pcl */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener pcl) {
        synchronized(pcsMutex) {
            propertyChangeSupport.addPropertyChangeListener(propertyName, pcl);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        synchronized(pcsMutex) {
            propertyChangeSupport.removePropertyChangeListener(pcl);
        }
    }

    private void fire(String propertyName, Object old, Object nue) {
        synchronized(pcsMutex) {
            propertyChangeSupport.firePropertyChange(propertyName, old, nue);
        }
    }
    // </editor-fold>

    /**
     * Monitors PlayList loading. Only after ALL PlayLists have finished loading
     * their songs do we fire "all songs loaded".
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() == null) return;
        switch (evt.getPropertyName()) {
            case PlayList.PROP_LOADED:
                if ((boolean)evt.getNewValue()) {
                    decrPlayListsLoading();
                    if (getPlayListsLoading() == 0) {
                        fire(PROP_ALL_SONGS_LOADED, false, true);
                    }
                }
                break;
        }
    }

    // <editor-fold defaultstate="collapsed" desc=" PlayList loading count ">
    /**
     * Call this every time a PlayList starts loading.
     */
    public synchronized void incrPlayListsLoading() {
        playListsLoading += 1;
    }
    /**
     * Call this every time a PlayList finished loading.
     */
    public synchronized void decrPlayListsLoading() {
        playListsLoading -= 1;
    }
    /**
     * This tell us if there are any PlayLists still loading.
     * @return The number of PlayLists currently being loaded.
     */
    public synchronized int getPlayListsLoading() {
        return playListsLoading;
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

            // Syncrhonized to block re-loads if there is one in progress
            synchronized (AddPlayLists.class) {
                if (!promptForSave("Save all changes before loading new playlists?")) return;

                playLists.clear();

                // Discard all the songs so they get re-loaded when the playlist is re-created
                if (clearSongs) Song.clearInstantiated();

                pl = new PlayList("Default Play List");
                pl.type = PlayList.Type.Default;
                playLists.add(pl);
                fire(PROP_NEW_PL_ADDED, null, pl);

                File dir = new File(pref.get(Options.OptPlayListDir, ""));
                if (dir.exists() && dir.canRead() && dir.isDirectory()) {

                    FilenameFilter filter = new FilenameFilter() {

                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".playlist.xml");
                        }
                    };

                    for (File f : Utils.listFiles(dir, filter, true)) {
                        incrPlayListsLoading();
                        pl = PlayList.deserialize(f, new WeakPropertyChangeListener(PlayListSet.this, pl));
                        if (pl == null) {
                            decrPlayListsLoading();
                        } else {
                            playLists.add(pl);
                            fire(PROP_NEW_PL_ADDED, null, pl);
                        }
                    }
                }

                pl = new PlayList("All Songs");
                pl.type = PlayList.Type.AllSongs;
                incrPlayListsLoading();
                pl.addPropertyChangeListener(PlayList.PROP_LOADED, new WeakPropertyChangeListener(PlayListSet.this, pl));
                pl.addAllSongs(new File(pref.get(Options.OptSongDir, "")), true);
                playLists.add(pl);
                fire(PROP_NEW_PL_ADDED, null, pl);

                LogRecord rec = new LogRecord(Level.INFO, "VIRTMUS_PLAYLISTS");
                rec.setParameters(new Object[] {playLists.size()});
                StatsLogger.log(rec);

                Collections.sort(playLists);

                synchronized(allPlayListsLoaded) {
                    // They're not fully loaded until their songs are also loaded.
                    fire(PROP_ALL_PL_LOADED, null, playLists);
                    allPlayListsLoaded = true;
                }
                MainApp.setStatusText("Finished loading all PlayLists");
            }
        }
    }
}
