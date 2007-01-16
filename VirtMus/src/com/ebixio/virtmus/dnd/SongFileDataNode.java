/*
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

package com.ebixio.virtmus.dnd;

import org.openide.loaders.DataNode;
import org.openide.nodes.Children;

public class SongFileDataNode extends DataNode {
    
    private static final String IMAGE_ICON_BASE = "com/ebixio/virtmus/dnd/audio-x-generic.png";
    
    public SongFileDataNode(SongFileDataObject obj) {
        super(obj, Children.LEAF);
        setIconBaseWithExtension(IMAGE_ICON_BASE);
    }
    
    //    /** Creates a property sheet. */
    //    protected Sheet createSheet() {
    //        Sheet s = super.createSheet();
    //        Sheet.Set ss = s.get(Sheet.PROPERTIES);
    //        if (ss == null) {
    //            ss = Sheet.createPropertiesSet();
    //            s.put(ss);
    //        }
    //        // TODO add some relevant properties: ss.put(...)
    //        return s;
    //    }
    
}
