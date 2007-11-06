/*
 * Song.java
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

import com.ebixio.virtmus.filefilters.SongFilter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
@XStreamAlias("song")
public class Song implements Comparable<Song> {
    @XStreamAlias("pages")
    public Vector<MusicPage> pageOrder = new Vector<MusicPage>();
    public String name = null;
    
    // transients are not initialized when the object is deserialized !!!
    private transient File sourceFile = null;
    private transient boolean isDirty = false;
    private transient List<PropertyChangeListener> propListeners = Collections.synchronizedList(new LinkedList<PropertyChangeListener>());
    private transient List<ChangeListener> pageListeners = Collections.synchronizedList(new LinkedList<ChangeListener>());
    /* We should instantiate each song only once.
     * That way when a page is added/removed from it the change will be reflected in all playlists containing the song. */
    //private transient static HashMap<String, Song> instantiated = Collections.synchronizedMap(new HashMap<String, Song>());
    private transient static HashMap<String, Song> instantiated = new HashMap<String, Song>();

    public Song() {}
    
    /** Creates a new instance of Song from a file, or a directory of files*/
    public Song(File f) {
        addPage(f);
    }
    
    /** Constructors are not called (and transients are not initialized)
     * when the object is deserialized !!! */
    private Object readResolve() {
        propListeners = Collections.synchronizedList(new LinkedList<PropertyChangeListener>());
        pageListeners = Collections.synchronizedList(new LinkedList<ChangeListener>());
        isDirty = false;
        return this;
    }
    
    public boolean isDirty() {
        for (MusicPage mp: pageOrder) {
            if (mp.isDirty) return true;
        }
        return isDirty;
    }
    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
        MainApp.findInstance().saveAllAction.updateEnable();
    }
    
    public boolean addPage() {
        final Frame mainWindow = WindowManager.getDefault().getMainWindow();
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        
        //String songDir = NbPreferences.forModule(MainApp.class).get(MainApp.OptSongDir, "");
        //File sD = new File(songDir);
        File sD = this.sourceFile.getParentFile();
        if (sD != null && sD.exists()) {
            fc.setCurrentDirectory(sD);
        }

        int returnVal = fc.showOpenDialog(mainWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File files[] = fc.getSelectedFiles();
            for (File f: files) {
                addPage(f);
            }
            return true;
        } else {
            return false;
        }
    }
    public boolean addPage(File f) {
        if (f == null) {
            return false;
        } else if (f.isDirectory()) {
            File[] images = f.listFiles();
            for (int i = 0; i < images.length; i++) {
                pageOrder.add(new MusicPageSVG(this, images[i]));
            }
        } else if (f.isFile()) {
            pageOrder.add(new MusicPageSVG(this, f));
        }
        setDirty(true);
        notifyListeners();
        return true;
    }
    public boolean addPage(MusicPage mp) {
        return addPage(mp, -1); // add it at the end
    }
    public boolean addPage(MusicPage mp, int index) {
        if (index < 0 || index > pageOrder.size()) index = pageOrder.size();
        pageOrder.add(index, mp);
        setDirty(true);
        notifyListeners();
        return true;
    }
    
    public boolean removePage(MusicPage[] mps) {
        boolean removed = false;
        for (MusicPage mp: mps) {
            if (pageOrder.remove(mp)) {
                removed = true;
                setDirty(true);
            }
        }
        notifyListeners();
        return removed;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void setName(String name) {
        this.name = name;
        setDirty(true);
    }
    public String getName() {
        if (name != null) {
            return name;
        } else if (this.sourceFile != null) {
            return this.sourceFile.getName().replaceFirst("\\.song\\.xml", "");
        } else {
            return "No name";
        }
    }
   
    public boolean save() {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            return saveAs();
        } else {
            return serialize();
        }
    }
    public boolean saveAs() {
        final Frame mainWindow = WindowManager.getDefault().getMainWindow();
        final JFileChooser fc = new JFileChooser();
        String songDir = NbPreferences.forModule(MainApp.class).get(MainApp.OptSongDir, "");
        File sD = new File(songDir);
        if (sD.exists()) {
            fc.setCurrentDirectory(sD);
        }
        fc.addChoosableFileFilter(new SongFilter());
        int returnVal = fc.showSaveDialog(mainWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (! file.toString().endsWith(".song.xml")) {
                file = new File(file.toString().concat(".song.xml"));
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
    
    public static Song open() {
        final Frame mainWindow = WindowManager.getDefault().getMainWindow();
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        
        String songDir = NbPreferences.forModule(MainApp.class).get(MainApp.OptSongDir, "");
        File sD = new File(songDir);
        if (sD.exists()) {
            fc.setCurrentDirectory(sD);
        }
        fc.addChoosableFileFilter(new SongFilter());
        
        int returnVal = fc.showOpenDialog(mainWindow);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            return deserialize(file);
        } else {
            return null;
        }        
    }
    
    public boolean serialize() {
        return serialize(this.sourceFile);
    }
    public boolean serialize(File toFile) {
        XStream xstream = new XStream();
        Annotations.configureAliases(xstream, Song.class);
        Annotations.configureAliases(xstream, MusicPageSVG.class);
        
        // Give each page a chance to do house cleaning before being saved.
        for (MusicPage mp: pageOrder) {
            mp.prepareToSave();
        }

        try {
            xstream.toXML(this, new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        
        // If this was saved using saveAs, add this file to the list of instantiated songs
        try {
            if (! Song.instantiated.containsKey(toFile.getCanonicalPath())) {
                Song.instantiated.put(toFile.getCanonicalPath(), this);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        setDirty(false);
        for (MusicPage mp: pageOrder) {
            mp.isDirty = false;
        }

        return true;
    }
    
    static Song deserialize(File f) {
        if (f == null || !f.getName().endsWith(".song.xml")) return null;

        Song s;
        
        String canonicalPath = "";
        try {
            canonicalPath = f.getCanonicalPath();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        if (Song.instantiated.containsKey(canonicalPath)) return Song.instantiated.get(canonicalPath);
        
        XStream xs = new XStream();
        Annotations.configureAliases(xs, Song.class);
        Annotations.configureAliases(xs, MusicPageSVG.class);
        
        try {
            s = (Song) xs.fromXML(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        } catch (FileNotFoundException ex) {
            //ex.printStackTrace();
            //ErrorManager.getDefault().notify(ErrorManager.WARNING, ex);
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            MainApp.log("Failed to deserialize " + canonicalPath);
            ErrorManager.getDefault().notify(ex);
            return null;
        }
        
        s.sourceFile = new File(canonicalPath);
        for (MusicPage mp: s.pageOrder) mp.deserialize(s);
        findPages(s);
        
        Song.instantiated.put(canonicalPath, s);

        return s;
    }

    /** We store absolute path names in the song file. If the song has moved the paths
     * for the page files might no longer be valid. This function attempts to fix that.
     * It expects s.sourceFile to already be in canonical form.
     */
    static void findPages(Song s) {
        for (MusicPage mp: s.pageOrder)  {
           File f = mp.getSourceFile();
           if (f != null && !f.exists()) {
               File newFile = Utils.findFileRelative(s.getSourceFile(), f);
               if (newFile != null) {
                   mp.setSourceFile(newFile);
                   s.isDirty = true;
               }
           }
        }
    }

    public void addPropertyChangeListener (PropertyChangeListener pcl) {
        propListeners.add(pcl);
    }
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        propListeners.remove(pcl);
    }
    private void fire(String propertyName, Object old, Object nue) {
        // Passing 0 below on purpose, so you only synchronize for one atomic call
        PropertyChangeListener[] pcls = propListeners.toArray(new PropertyChangeListener[0]);
        for (int i = 0; i < pcls.length; i++) {
            pcls[i].propertyChange(new PropertyChangeEvent(this, propertyName, old, nue));
        }
    }

    public void addChangeListener(ChangeListener listener) {
        pageListeners.add(listener);
    }
    public void removeChangeListener(ChangeListener listener) {
        pageListeners.remove(listener);
    }
    public void notifyListeners() {
        ChangeListener[] cls = pageListeners.toArray(new ChangeListener[0]);
        for (int i = 0; i < cls.length; i++) cls[i].stateChanged(new ChangeEvent(this));
    }

    public int compareTo(Song other) {
        return getName().compareTo(other.getName());
    }

}