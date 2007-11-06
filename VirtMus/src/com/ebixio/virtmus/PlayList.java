/*
 * PlayList.java
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

import com.ebixio.virtmus.filefilters.PlayListFilter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.ErrorManager;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 *
 * @author gburca
 */
@XStreamAlias("PlayList")
public class PlayList {
    @XStreamAlias("SongFiles")
    public Vector<File> songFiles = new Vector<File>();
    @XStreamAlias("Name")
    private String name = null;

    // We don't want to save the "Song", with all it's pages, etc... just the song.xml file name
    public transient Vector<Song> songs = new Vector<Song>();
    protected transient boolean isDirty = false;
    private transient File sourceFile = null;
    private transient Set<ChangeListener> listeners = new HashSet<ChangeListener>();
    
    public static enum Type { Normal, AllSongs, Default }
    public transient Type type = Type.Normal;
    
    
    /** Creates a new instance of PlayList */
    public PlayList() {}
    
    public PlayList(String name) {
        this.name = name;
    }
    
    /** This function is executed by the XStream library after an object is
     * deserialized. It needs to initialize the transient fields (which are not
     * serialized/deserialized).
     */
    private Object readResolve() {
        songs = new Vector<Song>();
        isDirty = false;
        listeners = new HashSet<ChangeListener>();
        type = Type.Normal;
        return this;
    }
    
    public void addAllSongs(File dir, boolean removeExisting) {
        if (removeExisting) songs.clear();
        if (!(dir.exists() && dir.isDirectory())) {
            notifyListeners();
            return;
        }

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name != null) ? name.endsWith(".song.xml") : false;
            }
        };
        
        for (File f: Utils.listFiles(dir, filter, true)) {
            if (f.canRead()) {
                Song s = Song.deserialize(f);
                if (s != null) songs.add(s);
            }
        }
        if (this.type != Type.Normal) sortSongsByName();
        notifyListeners();
    }


    public void sortSongsByName() {
//        class Comparer implements Comparator {
//                public int compare(Object song1, Object song2)
//                {
//                        String n1 = ((Song)song1).getName();
//                        String n2 = ((Song)song2).getName();
//                        return n1.compareTo(n2);
//                }
//        }
//        Collections.sort(songs, new Comparer());
        Collections.sort(songs);
    }

    public void saveAll() {
        for (Song s: songs) {
            if (s.isDirty()) s.save();
        }
        if (this.isDirty()) save();
    }
    public boolean save() {
        if (type == Type.Normal) {
            return serialize();
        } else {
            return false;
        }
    }
    public boolean saveAs() {
        final  Frame mainWindow = WindowManager.getDefault().getMainWindow();
        final JFileChooser fc = new JFileChooser();
        String playlistDir = NbPreferences.forModule(MainApp.class).get(MainApp.OptPlayListDir, "");
        File pD = new File(playlistDir);
        if (pD.exists()) {
            fc.setCurrentDirectory(pD);
        }
        fc.addChoosableFileFilter(new PlayListFilter());
        int returnVal = fc.showSaveDialog(mainWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (! file.toString().endsWith(".playlist.xml")) {
                file = new File(file.toString().concat(".playlist.xml"));
            }
            if (file.exists()) {
                returnVal = JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "Overwrite?", JOptionPane.YES_NO_OPTION);
                if (returnVal != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
            this.sourceFile = file;
            return serialize();
        } else {
            return false;
        }
    }
    
    public static PlayList open() {
        final Frame mainWindow = WindowManager.getDefault().getMainWindow();
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        String playlistDir = NbPreferences.forModule(MainApp.class).get(MainApp.OptPlayListDir, "");
        File pD = new File(playlistDir);
        if (pD.exists()) {
            fc.setCurrentDirectory(pD);
        }
        fc.addChoosableFileFilter(new PlayListFilter());
        
        int returnVal = fc.showOpenDialog(mainWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return PlayList.deserialize( fc.getSelectedFile() );
        }
        return null;
    }
    
    public boolean serialize() {
        return serialize(this.sourceFile);
    }
    public boolean serialize(File toFile) {
        if (toFile == null || toFile.isDirectory()) return false;
        
        XStream xs = new XStream();
        Annotations.configureAliases(xs, PlayList.class);
        
        // We need to re-create the songFiles
        songFiles.clear();
        for (Song s: songs) {
            if (s.getSourceFile() != null) songFiles.add(s.getSourceFile());
        }
        
        try {
            xs.toXML(this, new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        
        setDirty(false);
        return true;
    }
    
    static PlayList deserialize(File f) {
        if (f == null || !f.getName().endsWith(".playlist.xml")) return null;

        XStream xs = new XStream();
        Annotations.configureAliases(xs, PlayList.class);

        PlayList pl;
        
        try {
            pl = (PlayList) xs.fromXML(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            ErrorManager.getDefault().notify(ex);
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorManager.getDefault().notify(ex);
            return null;            
        }
        
        for (File sf: pl.songFiles) {
            if (!sf.exists()) {
                sf = Utils.findFileRelative(f, sf);
            }
            Song s = Song.deserialize(sf);
            if (s != null) pl.songs.add(s);
        }
        pl.sourceFile = f;
        return pl;
    }
    
    public void addSong(Song song) {
        songs.add(song);
        setDirty(true);
        if (this.type != Type.Normal) sortSongsByName();
        notifyListeners();
    }

    public boolean removeSong(Song song) {
        boolean result = songs.remove(song);
        setDirty(true);
        notifyListeners();
        return result;
    }
    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getName() {
        if (name != null) return name;
        if (this.sourceFile != null) return this.sourceFile.getName().replaceFirst("\\.playlist\\.xml", "");
        return "No name";
    }

    public void setName(String name) {
        this.name = name;
        setDirty(true);
    }

    public boolean isDirty() {
        if (type == Type.Normal) return isDirty;
        return false;
    }
    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
        MainApp.findInstance().saveAllAction.updateEnable();
    }
    
    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }
    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }
    public void notifyListeners() {
        //MainApp.log("PlayList::notifyListeners thread: " + Thread.currentThread().getName());
        //MainApp.log("PlayList::notifyListeners: " + this.toString() + " " + getName());
        ChangeEvent ev = new ChangeEvent(this);
        ChangeListener[] cls = listeners.toArray(new javax.swing.event.ChangeListener[0]);
        for (ChangeListener cl: cls) {
            cl.stateChanged(ev);
        }
    }

}
