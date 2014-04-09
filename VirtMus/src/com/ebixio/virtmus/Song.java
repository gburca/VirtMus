/*
 * Song.java
 *
 * Copyright (C) 2006-2009  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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
import com.ebixio.virtmus.filefilters.SongFilter;
import com.ebixio.virtmus.imgsrc.GenericImg;
import com.ebixio.virtmus.imgsrc.IcePdfImg;
import com.ebixio.virtmus.imgsrc.ImgSrc;
import com.ebixio.virtmus.imgsrc.PdfImg;
import com.ebixio.virtmus.imgsrc.PdfViewImg;
import com.ebixio.virtmus.xml.MusicPageConverter;
import com.ebixio.virtmus.xml.PageOrderConverter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.io.xml.TraxSource;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
    @XStreamAlias("pages")
    public final List<MusicPage> pageOrder = Collections.synchronizedList(new ArrayList<MusicPage>());
    public String name = null;
    public String tags = null;
    @XStreamAsAttribute
    private String version = MainApp.VERSION;   // Used in the XML output
    
    // transients are not initialized when the object is deserialized !!!
    private transient File sourceFile = null;
    private transient List<PropertyChangeListener> propListeners = Collections.synchronizedList(new LinkedList<PropertyChangeListener>());
    private transient List<ChangeListener> pageListeners = Collections.synchronizedList(new LinkedList<ChangeListener>());
    /* We should instantiate each song only once.
     * That way when a page is added/removed from it the change will be reflected in all playlists containing the song. */
    //private transient static HashMap<String, Song> instantiated = Collections.synchronizedMap(new HashMap<String, Song>());
    private transient static HashMap<String, Song> instantiated = new HashMap<String, Song>();

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

    public Song() {}
    
    /** Creates a new instance of Song from a file, or a directory of files
     * @param f The file/directory to create the song from.
     */
    public Song(File f) {
        addPage(f);
    }
    
    /** Constructors are not called (and transients are not initialized)
     * when the object is deserialized !!! */
    private Object readResolve() {
        propListeners = Collections.synchronizedList(new LinkedList<PropertyChangeListener>());
        pageListeners = Collections.synchronizedList(new LinkedList<ChangeListener>());
        savable = null;
        version = MainApp.VERSION;
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
            }
        } else {
            if (savable != null) {
                savable.saved();
                // TODO: Don't we have to remove it from the VirtMusLookup?
                VirtMusLookup.getInstance().remove(savable);
                savable = null;
                notifyListeners();
            }
        }
    }
    
    public boolean addPage() {
        final Frame mainWindow = WindowManager.getDefault().getMainWindow();
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        
        File sD = this.sourceFile.getParentFile();
        if (sD != null && sD.exists()) {
            fc.setCurrentDirectory(sD);
        } else {
            String songDir = NbPreferences.forModule(MainApp.class).get(MainApp.OptSongDir, "");
            sD = new File(songDir);
            if (sD != null && sD.exists()) fc.setCurrentDirectory(sD);
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
                if (images[i].isFile()) addPage(images[i]);
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
                    String[] pages = pageRange.split("-");
                    Integer p1 = Integer.decode(pages[0]);
                    Integer p2 = Integer.decode(pages[1]);
                    for (int p = p1; p <= p2; p++) {
                        pageOrder.add(new MusicPageSVG(this, f, p - 1));
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
        if (name == null) {
            if (this.name == null) return;
        } else if (name.equals(this.name)) {
            return;
        }
        
        String oldName = this.name;
        this.name = name;
        fire("nameProp", oldName, name);
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
        if (tags == null) {
            if (this.tags == null) return;
        } else if (tags.equals(this.tags)) {
            return;
        }

        String oldTags = this.tags;
        this.tags = tags;
        fire("tagsProp", oldTags, tags);
        setDirty(true);
        notifyListeners();
    }
    public String getTags() {
        return tags;
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
        XStream xstream = new XStream();
        configXStream(xstream);

        // Give each page a chance to do house cleaning before being saved.
        for (MusicPage mp: pageOrder) {
            mp.prepareToSave();
        }

//        try {
//            xstream.toXML(this, new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8"));
//        } catch (UnsupportedEncodingException ex) {
//            Exceptions.printStackTrace(ex);
//        } catch (FileNotFoundException ex) {
//            Exceptions.printStackTrace(ex);
//        }

        try {
            TraxSource traxSource = new TraxSource(this, xstream);
            OutputStreamWriter buffer = new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8");

            synchronized (Song.class) {
                songXFormer.transform(traxSource, new StreamResult(buffer));
            }

            //xstream.toXML(this, new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8"));
        } catch (FileNotFoundException ex) {
            Log.log(ex);
            return false;
        } catch (Exception ex) {
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
    
    static Song deserialize(File f) {
        if (f == null || !f.getName().endsWith(".song.xml")) return null;

        Song s;
        
        String canonicalPath;
        try {
            canonicalPath = f.getCanonicalPath();
        } catch (IOException ex) {
            Exceptions.attachMessage(ex, "No canonical path for " + f.toString());
            return null;
        }
        
        if (Song.instantiated.containsKey(canonicalPath)) return Song.instantiated.get(canonicalPath);

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
            
//            File f2 = new File(f.getName() + ".conv");
//            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f2), "UTF-8");
//            w.write(xformed.toString("UTF-8"));
//            w.close();

            s = (Song) xs.fromXML(xformed.toString("UTF-8"));

            //s = (Song) xs.fromXML(new InputStreamReader(fis, "UTF-8"));
        } catch (FileNotFoundException ex) {
            //ErrorManager.getDefault().notify(ErrorManager.WARNING, ex);
            NotifyUtil.error("Song file not found", canonicalPath, ex);
            Log.log("Song file not found " + canonicalPath);
            return null;
        } catch (Exception ex) {
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
        
        Song.instantiated.put(canonicalPath, s);

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

//            FileOutputStream fos = new FileOutputStream("D:\\erase.xml");
//            StreamResult res = new StreamResult(fos);
//            Source src = new DOMSource(doc);
//            xformer.transform(src, res);
//            fos.close();

            StreamResult res = new StreamResult(baos);
            Source src = new DOMSource(doc);
            xformer.transform(src, res);
            
        } catch (TransformerException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ParserConfigurationException ex) {
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
        propListeners.add(pcl);
    }
    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        propListeners.remove(pcl);
    }
    public void fire(String propertyName, Object old, Object nue) {
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
