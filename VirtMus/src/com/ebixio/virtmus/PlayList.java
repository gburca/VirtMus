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
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.io.xml.TraxSource;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.openide.ErrorManager;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 *
 * @author gburca
 */
@XStreamAlias("PlayList")
public class PlayList implements Comparable<PlayList> {

    @XStreamAlias("SongFiles")
    public Vector<File> songFiles = new Vector<File>();

    @XStreamAlias("Name")
    private String name = null;

    @XStreamAsAttribute
    private String version = MainApp.VERSION;   // Used in the XML output

    // We don't want to save the "Song", with all it's pages, etc... just the song.xml file name
    public transient List<Song> songs = Collections.synchronizedList(new Vector<Song>());
    protected transient boolean isDirty = false;
    
    // When separate threads are used to load the playlist songs, isFullyLoaded indicates
    // the thread has finished loading all the songs.
    public transient boolean isFullyLoaded = true;
    private transient File sourceFile = null;
    private transient Set<ChangeListener> listeners = new HashSet<ChangeListener>();
    
    public static enum Type { Default, AllSongs, Normal }
    public transient Type type = Type.Normal;

    private transient static Transformer plXFormer;

    static {
        InputStream plXform = Song.class.getResourceAsStream("/com/ebixio/virtmus/xml/PlayListTransform.xsl");
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            plXFormer = factory.newTransformer(new StreamSource(plXform));
            plXFormer.setOutputProperty(OutputKeys.INDENT, "yes");
            plXFormer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (TransformerConfigurationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    /** Creates a new instance of PlayList */
    public PlayList() {}
    
    public PlayList(String name) {
        this.name = name;
    }
    
//    /**
//     * 
//     * @param other The other playlist to make the current one equal to.
//     */
//    public void OpAssignment(PlayList other) {
//        this.songFiles.clear();
//        this.songFiles.addAll(other.songFiles);
//        
//        this.name = other.name;
//        synchronized (other.songs) {
//            synchronized (this.songs) {
//                this.songs.clear();
//                this.songs.addAll(other.songs);
//            }
//        }
//        
//        this.isDirty = other.isDirty;
//        this.isFullyLoaded = other.isFullyLoaded;
//        this.sourceFile = other.sourceFile;
//        
//        this.listeners.clear();
//        this.listeners.addAll(other.listeners);
//
//        this.type = other.type;
//    }
    
    /** This function is executed by the XStream library after an object is
     * deserialized. It needs to initialize the transient fields (which are not
     * serialized/deserialized).
     */
    private Object readResolve() {
        songs = Collections.synchronizedList(new Vector<Song>());
        isDirty = false;
        listeners = new HashSet<ChangeListener>();
        type = Type.Normal;
        version = MainApp.VERSION;
        return this;
    }
    
    public void addAllSongs(File dir, boolean removeExisting) {
        if (removeExisting) songs.clear();

        // It can take a very long time to find all the songs (depending on the
        // size of the directory tree) so we use a thread.
        addAllSongsThread t = new addAllSongsThread();
        t.dir = dir;
        t.setName("addAllSongsThread");
        t.setPriority(Thread.MIN_PRIORITY);
        
        isFullyLoaded = false;
        t.start();  // Will set isFullyLoaded to true when finished
    }

    private class addAllSongsThread extends Thread {
        public File dir;
        
        @Override
        public void run() {
            if (!(dir.exists() && dir.isDirectory())) {
                isFullyLoaded = true;
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
                    if (s != null) {
                        songs.add(s);
                        if (type != Type.Normal) sortSongsByName();
                        notifyListeners();
                    }
                }
            }
            
            isFullyLoaded = true;
            notifyListeners();
            
            Runnable r = new Runnable() {
                public void run() {
                    StatusDisplayer.getDefault().setStatusText("Loaded all songs from " + dir.getPath());
                }
            };
            SwingUtilities.invokeLater(r);
        }
    }

    public void sortSongsByName() {
        synchronized (songs) {
//            class Comparer implements Comparator {
//                    public int compare(Object song1, Object song2)
//                    {
//                            String n1 = ((Song)song1).getName();
//                            String n2 = ((Song)song2).getName();
//                            return n1.compareTo(n2);
//                    }
//            }
//            Collections.sort(songs, new Comparer());
            Collections.sort(songs);
        }
    }

    public void saveAll() {
        synchronized(songs) {
            for (Song s: songs) {
                if (s.isDirty()) s.save();
            }
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
        xs.processAnnotations(PlayList.class);
        
        // We need to re-create the songFiles
        songFiles.clear();
        synchronized (songs) {
            for (Song s: songs) {
                if (s.getSourceFile() != null) songFiles.add(s.getSourceFile());
            }
        }
        
        try {
            TraxSource traxSource = new TraxSource(this, xs);
            OutputStreamWriter buffer = new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8");
            synchronized(PlayList.class) {
                plXFormer.transform(traxSource, new StreamResult(buffer));
            }
            //xs.toXML(this, new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8"));
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
    
    public static PlayList deserialize(final File f) {
        if (f == null || !f.getName().endsWith(".playlist.xml")) return null;

        XStream xs = new XStream();
        xs.processAnnotations(PlayList.class);

        final PlayList pl;

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            pl = (PlayList) xs.fromXML(new InputStreamReader(fis, "UTF-8"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            ErrorManager.getDefault().notify(ex);
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorManager.getDefault().notify(ex);
            return null;            
        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        pl.sourceFile = f;
        pl.isFullyLoaded = false;

        Thread t = new Thread() {
            @Override public void run() {
                for (File sf: pl.songFiles) {
                    if (!sf.exists()) {
                        sf = Utils.findFileRelative(f, sf);
                    }
                    Song s = Song.deserialize(sf);
                    if (s != null) pl.songs.add(s);
                }
                pl.isFullyLoaded = true;
                pl.notifyListeners();
            }
        };
        
        t.setName("Deserialize songs for PlayList: " + pl.getName());
        t.start();
        
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
    
    public void reorder(int[] order) {
        Song[] ss = new Song[order.length];
        for (int i = 0; i < order.length; i++) {
            ss[order[i]] = songs.get(i);
        }

        songs.clear();
        for (Song s: ss) {
            songs.add(s);
        }
        
        setDirty(true);
        notifyListeners();
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
        if (name == null) {
            if (this.name == null) return;
        } else if (name.equals(this.name)) {
            return;
        }
        
        this.name = name;
        setDirty(true);
        notifyListeners();
    }

    public boolean isDirty() {
        if (type == Type.Normal) return isDirty;
        return false;
    }
    public void setDirty(boolean isDirty) {
        if (this.isDirty != isDirty) {
            this.isDirty = isDirty;
            notifyListeners();
        }
        if (MainApp.findInstance().saveAllAction != null)
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

    /**
     * Implements Comparable
     * Sorts the playlist first by type and then by name.
     * @param other
     * @return -1, 0, 1
     */
    public int compareTo(PlayList other) {
        int typeComp = type.compareTo(other.type);
        if (typeComp != 0) {
            return typeComp;
        } else {
            return getName().compareTo(other.getName());
        }
    }

}
