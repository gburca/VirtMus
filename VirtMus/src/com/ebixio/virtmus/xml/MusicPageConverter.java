/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus.xml;

import com.ebixio.virtmus.MainApp.Rotation;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.MusicPageSVG;
import com.ebixio.virtmus.imgsrc.GenericImg;
import com.ebixio.virtmus.imgsrc.PdfImg;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import org.openide.util.Exceptions;

/**
 *
 * @author GBURCA1
 */
public class MusicPageConverter implements Converter {
    private final Converter defaultConverter;
    private final ReflectionProvider reflectionProvider;

    public MusicPageConverter(Converter defaultConverter, ReflectionProvider reflectionProvider) {
        this.defaultConverter = defaultConverter;
        this.reflectionProvider = reflectionProvider;
    }

    public void marshal(Object obj, HierarchicalStreamWriter writer, MarshallingContext context) {
        MusicPageSVG mp = (MusicPageSVG)obj;
        
        try {
            Field f = reflectionProvider.getField(MusicPage.class, "name");
            String v = (String) f.get(mp);

            if (v != null && v.length() > 0) {
                writer.startNode("name");
                writer.setValue(v);
                writer.endNode();
            }

            writer.startNode("sourceFile");
            if (mp.imgSrc.getClass().equals(PdfImg.class)) {
                PdfImg pdf = (PdfImg)mp.imgSrc;
                writer.addAttribute("pageNum", String.valueOf(pdf.getPageNum()));
            }

            writer.setValue(mp.imgSrc.sourceFile.getCanonicalPath());

            writer.endNode();

            writer.startNode("rotation");
            context.convertAnother(mp.rotation);
            writer.endNode();

            f = reflectionProvider.getField(MusicPageSVG.class, "annotationSVG");
            v = (String)f.get(mp);
            if (v != null) {
                writer.startNode("annotationSVG");
                writer.setValue(v);
                writer.endNode();
            }

        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String name = null;
        reader.moveDown();
        if ("name".equals(reader.getNodeName())) {
            name = reader.getValue();
            reader.moveUp();
            reader.moveDown();
        }
        MusicPageSVG mp;

        String page = reader.getAttribute("pageNum");
        File f = new File(reader.getValue());
        reader.moveUp();
        if (page != null) {
            int pageNum = Integer.parseInt(page);
            mp = new MusicPageSVG(null, f, pageNum);
        } else {
            mp = new MusicPageSVG(null, f, null);
        }
        if (name != null) {
            reflectionProvider.writeField(mp, "name", name, MusicPage.class);
        }

        reader.moveDown();
        try {
            mp.rotation = (Rotation)context.convertAnother(mp, Rotation.class);
        } catch (Exception e) {
            System.out.println("ignore");
        }
        reader.moveUp();

        if (!reader.hasMoreChildren()) return mp;
        
        reader.moveDown();
        String svg = (String)context.convertAnother(mp, String.class);
        if (svg != null) {
            reflectionProvider.writeField(mp, "annotationSVG", svg, MusicPageSVG.class);
        }
        reader.moveUp();

        // TODO: return mp.readResolve(); ?
        return mp;
    }

    public boolean canConvert(Class type) {
        return MusicPage.class.isAssignableFrom(type);
    }

}
