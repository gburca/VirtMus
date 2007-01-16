/*
 * MusicPageNode.java
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

import com.ebixio.virtmus.actions.MusicPageCloneAction;
import com.ebixio.virtmus.actions.MusicPageRemoveAction;
import com.ebixio.virtmus.actions.RenameItemAction;
import com.ebixio.virtmus.actions.SongSaveAction;
import java.text.MessageFormat;
import javax.swing.Action;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.DeleteAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author gburca
 */
public class MusicPageNode extends AbstractNode {
    
    /** Creates a new instance of MusicPageNode */
    public MusicPageNode(MusicPage page) {
        super(Children.LEAF, Lookups.fixed(new Object[]{page.song, page}));
        setName(page.getName());
        displayFormat = new MessageFormat("{0}");
    }
    
    public boolean canCut()     { return true; }
    public boolean canDestroy() { return true; }
    public boolean canRename()  { return true; }
    
    public Action[] getActions(boolean context) {
        return new Action[] {
            SystemAction.get( SongSaveAction.class ),
            null,
            SystemAction.get( CopyAction.class ),
            SystemAction.get( CutAction.class ),
            SystemAction.get( DeleteAction.class ),
            null,
            SystemAction.get ( MusicPageCloneAction.class ),
            SystemAction.get ( MusicPageRemoveAction.class ),
            SystemAction.get ( RenameItemAction.class )
        };
    }
    
}
