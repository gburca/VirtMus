/*
 * SvgGenerator.java
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

package com.ebixio.virtmus.svg;

import java.io.StringWriter;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.openide.util.Exceptions;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * @author gburca
 */
public class SvgGenerator {
    DOMImplementation domImpl;
    static final String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI; // = "http://www.w3.org/2000/svg";
    Document document;
    SVGGraphics2D svgGenerator;
    // we want to use CSS style attributes
    boolean useCSS = true;

    public SvgGenerator() {
        // Get a DOMImplementation.
        domImpl = SVGDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document.
        document = domImpl.createDocument(svgNS, "svg", null);

        // Create an instance of the SVG Generator.
        // Note that SVGGraphics2D does not touch the "document" it is create with. That
        // document is used as a factory to create Elements (Document.createElementNS).
        svgGenerator = new SVGGraphics2D(document);
    }
    
    /**
     * Get a Graphics2D object that can be painted onto to generate SVG documents.
     */
    public SVGGraphics2D getGraphics() {
        return svgGenerator;
    }
    
    /**
     * Generates an SVG document from the drawing operations performed on the
     * Graphics2D object returned by getGraphics().
     */
    public String getSVG() {
        StringWriter out = new StringWriter();
        try {
            svgGenerator.stream(out, useCSS);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return out.toString();
    }

}
