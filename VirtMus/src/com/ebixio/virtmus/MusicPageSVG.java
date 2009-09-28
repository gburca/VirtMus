/*
 * Created on Oct 17, 2007, 6:07:17 PM
 * 
 * MusicPageAnnotation.java
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

import com.ebixio.virtmus.imgsrc.PdfImg;
import com.ebixio.virtmus.shapes.*;
import com.ebixio.virtmus.svg.SvgGenerator;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Dimension2D;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.gvt.CanvasGraphicsNode;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.GVTTreeWalker;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.util.XMLResourceDescriptor;
import org.openide.util.Exceptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.ls.*;

/**
 *
 * @author GBURCA
 */
@XStreamAlias("page")
public class MusicPageSVG extends MusicPage {
    /** Annotations shapes that have not yet been transfered to the svgDocument */
    private transient ArrayList<VmShape> shapes = new ArrayList<VmShape>();
    /** The DOM SVG document */
    protected transient SVGDocument svgDocument = null;
    /** The graphics node of the SVG annotations  */
    private transient GraphicsNode graphicsNode = null;
    
    /** This field contains the SVG document string. It is only updated when
     * prepareToSave() is called and should be considered invalid at all other times.
     */
    private String annotationSVG = null;
    
    
    /**
     * This is the "id" attribute of the background "image" element that we add
     * to exported SVG files and delete from imported SVG files.
     */
    public transient final static String SVG_BACKGROUND_ID = "VirtMusBackground";

    public MusicPageSVG(Song song, File sourceFile, Object opt) {
        super(song, sourceFile, opt);
    }
    
    @Override
    public void deserialize(Song s) {
        super.deserialize(s);
        shapes = new ArrayList<VmShape>();
        setAnnotationSVG(this.annotationSVG, false);
    }
    
    public void setAnnotationSVG(String svgStr, boolean flagAsDirty) {
        setAnnotationSVG(str2Document(svgStr), flagAsDirty);
    }
    public void setAnnotationSVG(SVGDocument svgDoc, boolean flagAsDirty) {
        shapes.clear();
        graphicsNode = null;
        svgDocument = svgDoc;
        
        if (flagAsDirty) {
            setDirty(true);
            if (this.changeListener != null) {
                changeListener.stateChanged(new ChangeEvent(this));
            }
        }
        song.notifyListeners();
    }
    
    @Override
    public void prepareToSave() {
        if (!hasAnnotations()) {
            this.annotationSVG = null;
        } else {
            transferShapes2Doc();
            updateGraphicsNode();
            this.annotationSVG = document2Str(svgDocument);
        }
    }

    protected SVGDocument addImgBackground(String svgStr) {
        return addImgBackground(MusicPageSVG.str2Document(svgStr));
    }
    
    /**
     * We add an "image" background to the SVG so that the user can edit the SVG
     * externally.
     * 
     * When importing back in we will remove the background image.
     * @param doc The DOM document to add the background image to.
     * @return The updated DOM document.
     */
    protected SVGDocument addImgBackground(SVGDocument doc) {
        try {
            if (doc == null) {
                return doc;
            }

            File imgFile = imgSrc.createImageFile();
            if (imgFile == null) return doc;

            Element img = doc.createElement("image");
            img.setAttribute("xlink:href", imgFile.getCanonicalPath());
            img.setAttribute("width", Integer.toString(imgSrc.getDimension().width));
            img.setAttribute("height", Integer.toString(imgSrc.getDimension().height));
            img.setAttribute("x", Integer.toString(0));
            img.setAttribute("y", Integer.toString(0));
            img.setAttribute("id", MusicPageSVG.SVG_BACKGROUND_ID);
            Element root = doc.getDocumentElement(); // <svg>
            if (root != null) {
                Node firstChild = root.getFirstChild();
                if (firstChild != null) {
                    root.insertBefore(img, firstChild);
                } else {
                    root.appendChild(img);
                }
            } else {
                // We should never be in this situation.
                root = doc.createElement("svg");
                root.appendChild(img);
                doc.appendChild(root);
            }
            // If we created a new document, it won't have width/height
            if (!root.hasAttribute("width")) {
                root.setAttribute("width", Integer.toString(imgSrc.getDimension().width));
            }
            if (!root.hasAttribute("height")) {
                root.setAttribute("height", Integer.toString(imgSrc.getDimension().height));
            }
            return doc;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return doc;
    }


    protected SVGDocument removeImgBackground(SVGDocument doc) {
        if (doc == null) return doc;
        
        Element root = doc.getDocumentElement();    // <svg>
        NodeList images = root.getElementsByTagName("image");
        for (int i = 0; i < images.getLength(); i++) {
            Node n = images.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element image = (Element)n;
                String id = image.getAttribute("id");
                if (id.compareTo(MusicPageSVG.SVG_BACKGROUND_ID) == 0) {
                    root.removeChild(n);
                }
            }
        }
        
        return doc;
    }

    public void importSVG(File fromFile) {
        if (fromFile == null) return;
        
        if (!(fromFile.exists() && fromFile.canRead())) return;
        
        SVGDocument doc = MusicPageSVG.getSVGDocument(fromFile);
        removeImgBackground(doc);
        setAnnotationSVG(doc, true);
    }
    
    /**
     * Generates an SVG document that can be edited with external SVG editors.
     * @return 
     */
    public String export2SvgStr() {
        String svg = null;
        SVGDocument document;
        
        prepareToSave();
        
        if (this.annotationSVG != null) {
            document = addImgBackground(this.annotationSVG);
        } else {
            // Create empty document
            SvgGenerator gen = new SvgGenerator();
            document = addImgBackground(gen.getSVG());            
        }
        
        svg = MusicPageSVG.document2Str(document);
        return (svg == null) ? "" : svg;
    }
    
    /**
     * Generates an SVG document that can be edited with external SVG editors.
     * @param toFile The SVG file to export to. This file will be overwritten.
     */
    public void export2SVG(File toFile) {
        if (toFile == null) return;

        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(toFile), "UTF-8");
            out.write(export2SvgStr());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }        
    }


    /**
     * Launches an external SVG editor to edit an SVG file. After the external
     * editor exits, it asks the MusicPage to load the edited SVG file as its
     * annotation.
     * @param editorPath The external editor to use
     */
    synchronized public void externalSvgEdit(String editorPath) {
        try {

            File svgFile = File.createTempFile("VirtMus", ".svg");
            export2SVG(svgFile);

            List<String> command = new ArrayList<String>();

            //command.add("c:\\Program Files\\Inkscape\\inkscape.exe");
            command.add(editorPath);
            //command.add("-f");
            command.add(svgFile.getCanonicalPath());

            ProcessBuilder builder = new ProcessBuilder(command);
            //Map<String, String> environ = builder.environment();
            //builder.directory(new File(System.getenv("temp")));
            builder.directory(svgFile.getParentFile());
            builder.redirectErrorStream(true);

            //System.out.println("Directory : " + System.getenv("temp"));
            final Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                MainApp.log(line);
            }
            MainApp.log("SVG editor program terminated!");

            if (svgFile.canRead()) {
                importSVG(svgFile);
            }

            if (svgFile.canWrite()) {
                svgFile.delete();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        imgSrc.destroyImageFile();
    }
    
    public void clearAnnotations() {
        shapes.clear();
        svgDocument = null;
        graphicsNode = null;
        setDirty(true);
        song.notifyListeners();
    }
    
    public void addAnnotation(VmShape s) {
        shapes.add(s);
        setDirty(true);
        song.notifyListeners();
    }
    
    /**
     * Checks to see if there are any annotations available (visible on the page).
     * The annotations could come from the SVG in the song.xml file, or from
     * drawings done by the user on the page/canvas.
     * @return 
     */
    public boolean hasAnnotations() {
        return !(svgDocument == null && shapes.isEmpty());
    }
    
    /**
     * Paints all the annotations for this page on the Graphics2D object passed in.
     * @param g2d The graphics to paint the annotations on.
     */
    @Override
    public void paintAnnotations(Graphics2D g2d) {
        if (graphicsNode == null) {
            updateGraphicsNode();
        }
        
        if (graphicsNode != null) {
            graphicsNode.paint(g2d);
        }

        for (VmShape s: shapes) {
            s.paint(g2d);
        }
    }
    
    /**
     * Updates the existing SVG document with the new annotations (if any) from 
     * the com.ebixio.virtmus.shapes.* objects in the <b>shapes</b> array.
     */
    public void transferShapes2Doc() {

        if (shapes.isEmpty()) return;
        
        SvgGenerator svgGenerator = new SvgGenerator();
        SVGGraphics2D svgGraphics2D = svgGenerator.getGraphics();
        
        // The dimensions of the SVG page (should match the size of the
        // music page image the annotations were drawn on).
        svgGraphics2D.setSVGCanvasSize(imgSrc.getDimension());

        for (VmShape s: shapes) {
            s.paint(svgGraphics2D);
        }
        shapes.clear();
        
        SVGDocument newSvgDoc = str2Document(svgGenerator.getSVG());
        if (svgDocument == null) {
            svgDocument = newSvgDoc;
        } else {
            // The root <svg> of the existing document.
            // We will append new shapes to this document.
            Element svgRoot = svgDocument.getRootElement();

            Element element = svgGraphics2D.getRoot();  // The <svg> element
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() != Node.COMMENT_NODE) {
                    if ( !child.getNodeName().equals("defs") ) {
                        Node dup = svgDocument.importNode(child, true);
                        svgRoot.appendChild(dup);
                    }
                }
            }
        }        
    }
    
    @Override
    public MusicPageSVG clone() {
        return this.clone(this.song);
    }
    public MusicPageSVG clone(Song song) {
        MusicPageSVG mp;
        if (imgSrc.getClass() == PdfImg.class) {
            PdfImg img = (PdfImg)imgSrc;
            mp = new MusicPageSVG(song, imgSrc.getSourceFile(), img.pageNum);
        } else {
            mp = new MusicPageSVG(song, imgSrc.getSourceFile(), null);
        }
        mp.setName(this.getName());
        mp.rotation = this.rotation;
        prepareToSave();
        mp.setAnnotationSVG(this.annotationSVG, false);
        return mp;
    }


    /**
     * Converts (serializes) a w3c Document to string
     * @param doc The document to serialize
     * @return An XML string that contains the document.
     */
    static public String document2Str(Document doc) {
        String docStr = null;
        if (doc == null) return docStr;
        
        TransformerFactory tFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = tFactory.newTransformer();

            DOMSource source = new DOMSource(doc);
            StreamResult result;
            
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            result = new StreamResult(byteStream);
            
            String SVG_MEDIA_TYPE = "image/svg+xml";
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, SVG_MEDIA_TYPE);
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(source, result);
            
            docStr = byteStream.toString();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return docStr;
    }

    static public SVGDocument str2Document(String src) {
        SVGDocument document = null;
        if (src == null) return document;
        
        try {

            // Load the document
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);

            document = (SVGDocument) f.createDocument("file:///nosuchfile", new ByteArrayInputStream(src.getBytes()));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return document;
    }
    
    @SuppressWarnings("deprecation")
    static public SVGDocument getSVGDocument(File file) {
        SVGDocument document = null;
        try {
            // Load the document
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);

            document = (SVGDocument) f.createDocument(file.toURL().toString());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return document;
    }
    
    protected void updateGraphicsNode() {
        if (svgDocument == null) {
            graphicsNode = null;
            return;
        }

        // Build the tree and get the document dimensions
        UserAgentAdapter userAgentAdapter;
        Dimension dim = imgSrc.getDimension();
        if (dim.width > 1 && dim.height > 1) {
            // If the SVG document contains dimensions, which one has precedence,
            // the SVG or the userAgentAdapter?
            userAgentAdapter = new MyUserAgentAdapter(dim);
        } else {
            userAgentAdapter = new UserAgentAdapter();
        }
        BridgeContext bridgeContext = new BridgeContext(userAgentAdapter);

        GVTBuilder builder = new GVTBuilder();

        graphicsNode = builder.build(bridgeContext, svgDocument);

//        CanvasGraphicsNode cgn = getCanvasGraphicsNode(graphicsNode);
//        if (cgn != null) {
//            cgn.setViewingTransform(new AffineTransform());
//        }

    }
    
    protected CanvasGraphicsNode getCanvasGraphicsNode(GraphicsNode gn) {
        if (!(gn instanceof CompositeGraphicsNode))
            return null;
        CompositeGraphicsNode cgn = (CompositeGraphicsNode)gn;
        List children = cgn.getChildren();
        if (children.size() == 0)
            return null;
        gn = (GraphicsNode)children.get(0);
        if (!(gn instanceof CanvasGraphicsNode))
            return null;
        return (CanvasGraphicsNode)gn;
    }

    
    /**
     * If the SVG document/file does not have dimensions:
     * <code>
     *      <svg width="123" height="456" ... />
     * </code>
     * all the nodes will have a clip size of 1. This function deletes the clip.
     * 
     * The real solution is to add dimensions to the SVG document or to use a
     * UserAgentAdapter that provides a getViewportSize function which returns a
     * size corresponding to the canvas that the SVG will be painted on (see for
     * example: MyUserAgentAdapter).
     * 
     * @see com.ebixio.virtmus.MusicPageAnnotations.MyUserAgentAdapter
     */
    private void clearClip(GraphicsNode gNode) {
        GVTTreeWalker treeWalker = new GVTTreeWalker(gNode);
        GraphicsNode currNode;
        while ((currNode = treeWalker.nextGraphicsNode()) != null) {
            currNode.setClip(null);
        }        
    }
        

    protected class MyUserAgentAdapter extends UserAgentAdapter {
        Dimension dim;
        
        public MyUserAgentAdapter(Dimension dim) {
            this.dim = dim;
        }
        
        @Override
        public Dimension2D getViewportSize() {
            return dim;
        }
    }
}
