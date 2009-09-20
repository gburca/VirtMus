/*
 * DraggableThumbnail.java
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

import java.awt.Component;
import java.io.File;
import net.java.swingfx.jdraggable.Draggable;

/**
 *
 * @author gburca
 */
public class DraggableThumbnail extends Thumbnail implements Draggable {
    
    /** Creates a new instance of DraggableThumbnail */
    public DraggableThumbnail() {
        super();
    }
    public DraggableThumbnail(int w, int h) {
        super(w, h);
    }
//    public DraggableThumbnail(int w, int h, String filename, String description) {
//        super(w, h, new File(filename), description);
//    }
    public DraggableThumbnail(int w, int h, String description) {
        super(w, h, description);
    }

    public Component getComponent() {
        return this;
    }

    
}
