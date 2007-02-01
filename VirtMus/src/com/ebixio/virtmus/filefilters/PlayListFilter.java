/*
 * PlayListFilter.java
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

package com.ebixio.virtmus.filefilters;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author gburca
 */
public class PlayListFilter extends FileFilter {
    public boolean accept(File f) {
        if (f.isFile() && f.toString().endsWith(".playlist.xml")) {
            return true;
        } else if (f.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    public String getDescription() {
        return "Playlist files";
    }
}
