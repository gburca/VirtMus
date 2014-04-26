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

package com.ebixio.virtmus;

import org.openide.explorer.ExplorerManager;

/**
 * Provides some common VirtMus ExplorerManager(s).
 * 
 * In order to display the same set of nodes in multiple explorer views and have
 * the selection synchronized across different TopComponents, we must use a
 * common ExplorerManager.
 * We want to synchronize the song shown in the PlayList
 * TopComponent
 * 
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class CommonExplorers {

    /**
     * This is the main VirtMus ExplorerManager.
     * We want to be able to share this between PlayList, Thumbs, etc...
     */
    public static final ExplorerManager MainExplorerManager = new ExplorerManager();
    
    public static final ExplorerManager TagsExplorerManager = new ExplorerManager();
}
