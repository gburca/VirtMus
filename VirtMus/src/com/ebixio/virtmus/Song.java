/*
 * Song.java
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
import com.ebixio.util.NotifyUtil;
import com.ebixio.util.NumberRange;
import com.ebixio.util.PropertyChangeSupportUnique;
import com.ebixio.util.Util;
import com.ebixio.virtmus.filefilters.SongFilter;
import com.ebixio.virtmus.imgsrc.GenericImg;
import com.ebixio.virtmus.imgsrc.IcePdfImg;
import com.ebixio.virtmus.imgsrc.ImgSrc;
import com.ebixio.virtmus.imgsrc.PdfImg;
import com.ebixio.virtmus.imgsrc.PdfViewImg;
import com.ebixio.virtmus.options.Options;
import com.ebixio.virtmus.shapes.VmShape;
import com.ebixio.virtmus.stats.StatsCollector;
import com.ebixio.virtmus.xml.MusicPageConverter;
import com.ebixio.virtmus.xml.PageOrderConverter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.io.xml.TraxSource;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.netbeans.spi.actions.AbstractSavable;
import org.openide.filesystems.FileObject;
import org.openide.loaders.SaveAsCapable;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
@XStreamAlias("song")
public class Song implements Comparable<Song> {
    @XStreamAlias("Pages")
    public final List<MusicPage> pageOrder = Collections.synchronizedList(new ArrayList<MusicPage>());

    @XStreamAlias("Name")
    private String name = null;

    @XStreamAlias("Tags")
    private String tags = null;

    @XStreamAlias("Notes")
    private String notes = null;

    @XStreamAsAttribute
    private String version = MainApp.VERSION;   // Used in the XML output

    // transients are not initialized when the object is deserialized !!!
    private transient File sourceFile = null;

    public static final String PROP_TAGS = "tagsProp";
    public static final String PROP_NAME = "nameProp";
    public static final String PROP_ANNOT = "annotProp"; // Annotations add/rm
    public static final String PROP_DIRTY = "dirtyProp";

    private transient PropertyChangeSupportUnique pcs = new PropertyChangeSupportUnique(this);
    private transient final Object pcsMutex = new Object();
    // Could change this to EventListenerList if we had more than 1 event type
    private transient List<ChangeListener> pageListeners = Collections.synchronizedList(new LinkedList<ChangeListener>());

    /* We should instantiate each song only once.
     * That way when a page is added/removed from it the change will be reflected in all playlists containing the song. */
    private transient static Map<String, Song> instantiated = Collections.synchronizedMap(new HashMap<String, Song>());
    /** Keeps track of de-serializations that are in progress. */
    private transient static Map<String, CountDownLatch> inProgress = new HashMap<>();
    /** The de-serializer lock. */
    private transient static final Object deserLock = new Object();

    private static final Icon ICON = ImageUtilities.loadImageIcon(
            "com/ebixio/virtmus/resources/SongNode.png", false);
    private transient SongSavable savable = null;

    private transient static Transformer songXFormer;

    static {
        InputStream songXform = Song.class.getResourceAsStream("/com/ebixio/virtmus/xml/SongTransform.xsl");
        TransformerFactory factory = TransformerFactory.newInstance();
        try {
            songXFormer = factory.newTransformer(new StreamSource(songXform));
            songXFormer.setOutputProperty(OutputKeys.INDENT, "yes");
            songXFormer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (TransformerConfigurationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /** Creates a new Song instance.
     * This constructor is NOT called when the object is deserialized.
     */
    public Song() {
        addPropertyChangeListener(PROP_ANNOT, StatsCollector.findInstance());
        addPropertyChangeListener(PROP_DIRTY, StatsCollector.findInstance());
    }

    /** Creates a new instance of Song from a file, or a directory of files.
     * This constructor is NOT called when the object is deserialized.
     * @param f The file/directory to create the song from.
     */
    public Song(File f) {
        addPage(f);
        addPropertyChangeListener(PROP_ANNOT, StatsCollector.findInstance());
        addPropertyChangeListener(PROP_DIRTY, StatsCollector.findInstance());
    }

    /** Constructors are not called (and transients are not initialized)
     * when the object is deserialized !!! */
    private Object readResolve() {
        pcs = new PropertyChangeSupportUnique(this);
        pageListeners = Collections.synchronizedList(new LinkedList<ChangeListener>());
        savable = null;
        version = MainApp.VERSION;

        addPropertyChangeListener(PROP_ANNOT, StatsCollector.findInstance());
        addPropertyChangeListener(PROP_DIRTY, StatsCollector.findInstance());

        return this;
    }

    public boolean isDirty() {
        return savable != null;
    }
    public void setDirty(boolean isDirty) {

        if (isDirty) {
            if (savable == null) {
                savable = new SongSavable(this);
                VirtMusLookup.getInstance().add(savable);
                notifyListeners();
                fire(PROP_DIRTY, false, true);
            }
        } else {
            if (savable != null) {
                savable.saved();
                VirtMusLookup.getInstance().remove(savable);
                savable = null;
                notifyListeners();
                fire(PROP_DIRTY, true, false);
            }
        }
    }

    /**
     * Present the user with a file-open dialog to select a page image to add to
     * this song. The user may select more than one item. Each item is passed to
     * {@link Song#addPage(java.io.File)} to be added.
     *
     * @return true, unless the user canceled out.
     */
    public boolean addPage() {
        final Frame mainWindow = WindowManager.getDefault().getMainWindow();
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);

        File sD = this.sourceFile.getParentFile();
        if (sD != null && sD.exists()) {
            fc.setCurrentDirectory(sD);
        } else {
            String songDir = NbPreferences.forModule(MainApp.class).get(Options.OptSongDir, "");
            sD = new File(songDir);
            if (sD.exists()) fc.setCurrentDirectory(sD);
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

    /**
     * Adds one or more pages to this song.
     * @param f Can be a directory (of images, PDFs, etc...), or a single PDF or
     * image file.
     *
     * @return
     */
    public boolean addPage(File f) {
        if (f == null) {
            return false;
        } else if (f.isDirectory()) {
            File[] images = f.listFiles();
            for (File image : images) {
                if (image.isFile()) {
                    addPage(image);
                }
            }
        } else if (f.isFile()) {
            if (f.getName().toLowerCase().endsWith(".pdf")) {
                int pdfPages;
                org.icepdf.core.pobjects.Document doc = new org.icepdf.core.pobjects.Document();
                try {
                    doc.setFile(f.getCanonicalPath());
                    pdfPages = doc.getNumberOfPages();
                } catch (Exception e) {
                    pdfPages = 0;
                    JOptionPane.showMessageDialog(null, e.toString(), "PDF Error", JOptionPane.WARNING_MESSAGE);
                }
                doc.dispose();
                if (pdfPages > 0) {
                    String pageRange = JOptionPane.showInputDialog(f.getName() + "\nPage range?",
                            "1-" + pdfPages);
                    NumberRange range = new NumberRange(pageRange);
                    for (int p : range) {
                        if (p > 0 && p <= pdfPages) {
                            pageOrder.add(new MusicPageSVG(this, f, p - 1));
                        }
                    }
                }
            } else {
                pageOrder.add(new MusicPageSVG(this, f, null));
            }
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

    public void reorder(int[] order) {
        MusicPage[] mp = new MusicPage[order.length];
        for (int i = 0; i < order.length; i++) {
            mp[order[i]] = pageOrder.get(i);
        }

        pageOrder.clear();
        pageOrder.addAll(Arrays.asList(mp));

        setDirty(true);
        notifyListeners();
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void setName(String name) {
        if (!Util.isDifferent(this.name, name)) return;

        String oldName = this.name;
        this.name = name;
        fire(PROP_NAME, oldName, name);
        setDirty(true);
        notifyListeners();
    }
    public String getName() {
        if (name != null && name.length() > 0) {
            return name;
        } else if (this.sourceFile != null) {
            return this.sourceFile.getName().replaceFirst("\\.song\\.xml", "");
        } else {
            return "No name";
        }
    }

    public void setTags(String tags) {
        if (!Util.isDifferent(this.tags, tags)) return;

        String oldTags = this.tags;
        this.tags = tags;
        fire(PROP_TAGS, oldTags, tags);
        setDirty(true);
        notifyListeners();
    }
    public String getTags() {
        return tags;
    }

    public void setNotes(String notes) {
        if (!Util.isDifferent(this.notes, notes)) return;

        this.notes = notes;
        setDirty(true);
    }
    public String getNotes() {
        return notes;
    }

    /** Keeps track of how many annotations were made to this song. Used for
     * statistical reports.
     * @param page
     * @param s
     */
    public void addedAnnot(MusicPage page, VmShape s) {
        fire(PROP_ANNOT, null, s);
    }

    public void removedAnnot(MusicPage page, VmShape s) {
        fire(PROP_ANNOT, s, null);
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
        String songDir = NbPreferences.forModule(MainApp.class).get(Options.OptSongDir, "");
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

        String songDir = NbPreferences.forModule(MainApp.class).get(Options.OptSongDir, "");
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

    static private void configXStream(XStream xs) {
        xs.setMode(XStream.NO_REFERENCES);
        //Converter c = xstream.getConverterLookup().lookupConverterForType(MusicPage.class);
        //c = xstream.getConverterLookup().lookupConverterForType(MusicPageSVG.class);

        xs.processAnnotations(Song.class);
        xs.processAnnotations(MusicPageSVG.class);
        xs.processAnnotations(ImgSrc.class);
        xs.processAnnotations(IcePdfImg.class);
        xs.processAnnotations(PdfViewImg.class);
        xs.processAnnotations(PdfImg.class);
        xs.processAnnotations(GenericImg.class);
        //xs.addDefaultImplementation(ArrayList.class, List.class);

        xs.registerConverter(new MusicPageConverter(
                xs.getConverterLookup().lookupConverterForType(MusicPageSVG.class),
                xs.getReflectionProvider()));
        xs.registerLocalConverter(Song.class, "pageOrder", new PageOrderConverter(xs));
        xs.addDefaultImplementation(MusicPageSVG.class, MusicPage.class);

    }

    public boolean serialize() {
        return serialize(this.sourceFile);
    }

    public boolean serialize(File toFile) {
        version = MainApp.VERSION;  // Update version

        XStream xstream = new XStream();
        configXStream(xstream);

        // Give each page a chance to do house cleaning before being saved.
        for (MusicPage mp: pageOrder) {
            mp.prepareToSave();
        }

        boolean debug = false;
        if (debug) {
            try {
                xstream.toXML(this, new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8"));
            } catch (UnsupportedEncodingException | FileNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        try {
            TraxSource traxSource = new TraxSource(this, xstream);
            OutputStreamWriter buffer = new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8");

            synchronized (Song.class) {
                songXFormer.transform(traxSource, new StreamResult(buffer));
            }

            //xstream.toXML(this, new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8"));
        } catch (FileNotFoundException | UnsupportedEncodingException | TransformerException ex) {
            Log.log(ex);
            return false;
        }

        // If this was saved using saveAs, add this file to the list of instantiated songs
        try {
            if (! Song.instantiated.containsKey(toFile.getCanonicalPath())) {
                Song.instantiated.put(toFile.getCanonicalPath(), this);
            }
        } catch (IOException ex) {
            Log.log(ex);
        }

        setDirty(false);
        for (MusicPage mp: pageOrder) {
            mp.isDirty = false;
        }

        return true;
    }

    /** De-serialization helper class.
     * Performs the de-serialization and notifies waiting threads when it's done.
     */
    private static class Deserializer implements Runnable {
        CountDownLatch latch;
        File file;
        String canonicalPath;
        public Deserializer(CountDownLatch latch, File file, String canonicalPath) {
            this.latch = latch;
            this.file = file;
            this.canonicalPath = canonicalPath;
        }

        @Override
        public void run() {
            Song s = Song.deserializeCore(file, canonicalPath);
            if (s != null) {
                synchronized(deserLock) {
                    Song.instantiated.put(canonicalPath, s);
                    Song.inProgress.remove(canonicalPath);
                }
            }
            // Notify all pending threads that the song is ready
            latch.countDown();
        }
    }

    /**
     * De-serializes a song file.
     *
     * This function will block until the de-serialization is complete. The song
     * de-serialization can be in one of 3 states:
     * <li>Not started
     * <li>In progress
     * <li>Completed
     *
     * If it's completed, we just return the de-serialized song. If it's not yet
     * started, we start a new Deserializer and wait for it to complete before
     * returning the song (or null). If a job is already in progress we await()
     * the same latch, and return the song (or null) when the job is done.
     *
     * @param f A song file to de-serialize
     * @return The de-serialized song, or null if an error was encountered.
     */
    static Song deserialize(File f) {
        if (f == null || !f.getName().endsWith(".song.xml")) return null;

        String canonicalPath;
        try {
            canonicalPath = f.getCanonicalPath();
        } catch (IOException ex) {
            //Exceptions.attachMessage(ex, "No canonical path for " + f.toString());
            return null;
        }

        CountDownLatch latch;
        Deserializer r = null;

        synchronized(deserLock) {
            if (instantiated.containsKey(canonicalPath)) { // deserialization completed
                //Log.log("Song deserialization completed: " + canonicalPath, Level.FINEST);
                return instantiated.get(canonicalPath);
            } else if (!inProgress.containsKey(canonicalPath)) { // deser not started
                //Log.log("Song deserialization not started: " + canonicalPath, Level.FINEST);
                latch = new CountDownLatch(1);
                inProgress.put(canonicalPath, latch);
                r = new Deserializer(latch, f, canonicalPath);
            } else { // deserialization in progress
                //Log.log("Song deserialization in progress: " + canonicalPath, Level.FINEST);
                latch = inProgress.get(canonicalPath);
            }
        }

        if (r != null) {
            r.run();
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }

        return instantiated.get(canonicalPath);
    }

    /**
     * The core de-serialization code. Callers must ensure the arguments
     * are valid. No error checking is done on them.
     * @param f The file to be de-serialized
     * @param canonicalPath The result of f.getCanonicalPath()
     * @return
     */
    private static Song deserializeCore(File f, String canonicalPath) {
        Song s;

        XStream xs = new XStream();
        configXStream(xs);

        FileInputStream fis = null;
        ByteArrayOutputStream xformed = null;
        try {
            fis = new FileInputStream(f);

            xformed = new ByteArrayOutputStream();
            synchronized (Song.class) {
                songXFormer.transform(new StreamSource(fis), new StreamResult(xformed));
            }

            xformed = convertReferences(new ByteArrayInputStream(xformed.toByteArray()));

            boolean debug = false;
            if (debug) {
                File f2 = new File(f.getName() + ".conv");
                try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f2), "UTF-8")) {
                    w.write(xformed.toString("UTF-8"));
                }
            }

            s = (Song) xs.fromXML(xformed.toString("UTF-8"));

        } catch (FileNotFoundException ex) {
            //ErrorManager.getDefault().notify(ErrorManager.WARNING, ex);
            NotifyUtil.error("Song file not found", canonicalPath, ex);
            Log.log("Song file not found " + canonicalPath);
            return null;
        } catch (IOException | TransformerException ex) {
            //Exceptions.attachMessage(ex, "Failed to deserialize " + canonicalPath);
            NotifyUtil.error("Failed to read song", canonicalPath, ex);
            Log.log("Failed to deserialize " + canonicalPath);
            return null;
        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            if (xformed != null) try {
                xformed.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        s.sourceFile = new File(canonicalPath);
        synchronized (s.pageOrder) {
            for (MusicPage mp: s.pageOrder) mp.deserialize(s);
        }
        findPages(s);

        return s;
    }

    static void convertReference(Document doc, String elem) {
        XPath xPath = XPathFactory.newInstance().newXPath();

        NodeList nodes = doc.getElementsByTagName(elem);

        for (int i = 0; i < nodes.getLength(); i++) {
            NamedNodeMap attrs = nodes.item(i).getAttributes();
            Node ref = attrs.getNamedItem("reference");
            if (ref != null) {
                String refPtr = ref.getNodeValue();
                if (refPtr != null) {
                    try {
                        XPathExpression xExpr = xPath.compile(refPtr);
                        Node refNode = (Node) xExpr.evaluate(nodes.item(i), XPathConstants.NODE);
                        if (refNode != null) {
                            nodes.item(i).setTextContent(refNode.getTextContent());
                        }
                    } catch (XPathExpressionException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                attrs.removeNamedItem("reference");
            }
        }

    }

    static ByteArrayOutputStream convertReferences(InputStream stream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(stream);

            convertReference(doc, "rotation");
            convertReference(doc, "sourceFile");

            Transformer xformer = TransformerFactory.newInstance().newTransformer();

            boolean debug = false;
            if (debug) {
                try (FileOutputStream fos = new FileOutputStream("C:\\erase.xml")) {
                    StreamResult res = new StreamResult(fos);
                    Source src = new DOMSource(doc);
                    xformer.transform(src, res);
                }
            }

            StreamResult res = new StreamResult(baos);
            Source src = new DOMSource(doc);
            xformer.transform(src, res);

        } catch (TransformerException | SAXException | IOException | ParserConfigurationException ex) {
            Exceptions.printStackTrace(ex);
        }

        return baos;
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
                   //s.setDirty(true);
               }
           }
        }
    }

    /**
     * Clears all deserialized songs so they can be re-loaded
     */
    public static void clearInstantiated() {
        instantiated.clear();
    }

    public void addPropertyChangeListener (PropertyChangeListener pcl) {
        synchronized(pcsMutex) {
            pcs.addPropertyChangeListener(pcl);
        }
    }
    public void addPropertyChangeListener (String propertyName, PropertyChangeListener pcl) {
        synchronized(pcsMutex) {
            pcs.addPropertyChangeListener(propertyName, pcl);
        }
    }
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        synchronized(pcsMutex) {
            pcs.removePropertyChangeListener(pcl);
        }
    }
    private void fire(String propertyName, Object old, Object nue) {
        synchronized(pcsMutex) {
            pcs.firePropertyChange(propertyName, old, nue);
        }
    }

    public void addChangeListener(ChangeListener listener) {
        if (!pageListeners.contains(listener)) pageListeners.add(listener);
    }
    public void removeChangeListener(ChangeListener listener) {
        pageListeners.remove(listener);
    }
    public void notifyListeners() {
        ChangeListener[] cls = pageListeners.toArray(new ChangeListener[0]);
        for (ChangeListener cl : cls) {
            cl.stateChanged(new ChangeEvent(this));
        }
    }

    @Override
    public int compareTo(Song other) {
        return getName().compareTo(other.getName());
    }

    private class SongSavable extends AbstractSavable implements Icon, SaveAsCapable {

        private final Song s;

        public SongSavable(Song s) {
            if (s == null) throw new IllegalArgumentException("Null Song not allowed");
            this.s = s;
            register();
        }

        @Override
        protected String findDisplayName() {
            return s.getName();
        }

        @Override
        protected void handleSave() throws IOException {
            s.save();
            VirtMusLookup.getInstance().remove(this);
        }

        public void saved() {
            unregister();
            VirtMusLookup.getInstance().remove(this);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof SongSavable) {
                SongSavable ss = (SongSavable)other;
                return s.equals(ss.s);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            ICON.paintIcon(c, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return ICON.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return ICON.getIconHeight();
        }

        @Override
        public void saveAs(FileObject folder, String fileName) throws IOException {
            FileObject newFile = folder.getFileObject(fileName);
            s.sourceFile = new File(newFile.getNameExt());
            save();
        }
    }

}
