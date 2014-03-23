/*
 * MusicPage.java
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

package com.ebixio.virtmus.xml;

import com.ebixio.virtmus.MainApp.Rotation;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.MusicPageSVG;
import com.ebixio.virtmus.imgsrc.PdfImg;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import org.openide.util.Exceptions;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class MusicPageConverter implements Converter {
    private final Converter defaultConverter;
    private final ReflectionProvider reflectionProvider;

    public MusicPageConverter(Converter defaultConverter, ReflectionProvider reflectionProvider) {
        this.defaultConverter = defaultConverter;
        this.reflectionProvider = reflectionProvider;
    }

    @Override
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
                Dimension dim = mp.imgSrc.getDimension();
                writer.addAttribute("width", String.valueOf(dim.width));
                writer.addAttribute("height", String.valueOf(dim.height));
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

    @Override
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
        //String type = reader.getAttribute("type");
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
            System.out.println("Ignored exception");
        }
        reader.moveUp();

        if (!reader.hasMoreChildren()) return mp;
        
        reader.moveDown();
        String width = reader.getAttribute("width");
        String height = reader.getAttribute("height");
        String svg = reader.getValue();
        if (svg != null) {
            reflectionProvider.writeField(mp, "annotationSVG", svg, MusicPageSVG.class);

            if (width != null && height != null) {
                try {
                    Dimension dim = new Dimension(Integer.parseInt(width), Integer.parseInt(height));
                    if (mp.imgSrc.getClass().equals(PdfImg.class)) {
                        PdfImg pdf = (PdfImg)mp.imgSrc;
                        pdf.setDimension(dim);
                    }
                } catch (Exception e) {
                    System.out.print("Ignored exception");
                }
            }
        }
        reader.moveUp();

        // TODO: return mp.readResolve(); ?
        return mp;
    }

    @Override
    public boolean canConvert(Class type) {
        return MusicPage.class.isAssignableFrom(type);
    }

}
