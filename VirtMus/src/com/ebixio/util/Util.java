/*
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
package com.ebixio.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.openide.util.Exceptions;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Gabriel Burca
 */
public class Util {

    public static String join(Collection<String> s, String delimiter) {
        if (s.isEmpty()) {
            return "";
        }
        Iterator<String> iter = s.iterator();
        StringBuilder buffer = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        return buffer.toString();
    }

    /**
     * Helper function to test if the new string is different from the old one.
     * Used by PlayLists and Songs to determine if the tags or notes properties
     * have changed.
     *
     * @param old Old (existing) string
     * @param newStr New string
     * @return True if the two arguments are different
     */
    public static boolean isDifferent(String old, String newStr) {
        // When the user selects a property but then clicks away, sometimes the
        // default value (<null value>) gets entered as the new value.
        if ("<null value>".equals(newStr)) newStr = null;

        if (newStr != null) {
            newStr = newStr.trim();
            if (newStr.length() == 0) newStr = null;
        }
        if (newStr == null) {
            if (old == null) return false;
        } else if (newStr.equals(old)) {
            return false;
        }
        return true;
    }

    public static Validator getValidator(InputStream xsd) {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema;
        Validator validator = null;

        try {
            schema = factory.newSchema(new StreamSource(xsd));
            validator = schema.newValidator();
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        }

        return validator;
    }

    public static boolean validateXml(File xml, InputStream xsd) {
        return validateXml(xml, getValidator(xsd));
    }

    public static boolean validateXml(File xml, Validator validator) {
        if (validator == null) return false;

        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = parser.parse(xml);
            validator.validate(new DOMSource(document));

            return true;    // No exceptions == valid document

        } catch (IOException | ParserConfigurationException | SAXException ex) {
            Exceptions.printStackTrace(ex);
        }

        return false;
    }
}
