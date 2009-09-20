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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.File;
import java.io.IOException;
import org.openide.util.Exceptions;

/**
 *
 * @author gburca
 */
public class FileConverter implements Converter, ConverterMatcher {
    XStream xs = new XStream();

    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        File f = (File)value;
        try {
            writer.setValue(f.getCanonicalPath());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return new File(reader.getValue());
    }

    public boolean canConvert(Class clazz) {
        return File.class.isAssignableFrom(clazz);
    }

}
