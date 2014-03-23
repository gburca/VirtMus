/*
 * PageOrderConverter.java
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

package com.ebixio.virtmus.xml;

import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.MusicPageSVG;
import com.ebixio.virtmus.Song;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * TODO: Handle sourceFile references in the song file, or modify the xsl to
 * get rid of them.
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class PageOrderConverter implements Converter, ConverterMatcher {
    XStream xs;

    public PageOrderConverter(XStream xs) {
        this.xs = xs;
    }

    @SuppressWarnings(value={"unchecked"})
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        List<MusicPage> list = (List<MusicPage>)value;
        synchronized(list) {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                xs.marshal(i.next(), writer);
            }
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        List<MusicPage> order = Collections.synchronizedList(new Vector<MusicPage>());
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            MusicPage mp = (MusicPage)xs.unmarshal(reader);
            if (mp != null) order.add(mp);
            reader.moveUp();
        }
        return order;
    }

    public boolean canConvert(Class clazz) {
        return true;
    }

}
