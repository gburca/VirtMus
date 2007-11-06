/*
 * PlayListNode.java
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

import com.ebixio.virtmus.actions.GoLive;
import com.ebixio.virtmus.actions.RenameItemAction;
import com.ebixio.virtmus.actions.SavePlayListAction;
import com.ebixio.virtmus.actions.SongNewAction;
import com.ebixio.virtmus.actions.SongOpenAction;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author gburca
 */
public class PlayListNode extends AbstractNode {
    private PlayList playList;
    
    /** Creates a new instance of PlayListNode */
    public PlayListNode(PlayList playList) {
        super (new Songs(playList), Lookups.singleton(playList));
        this.playList = playList;
        setName(playList.getName());
        displayFormat = new MessageFormat("{0}");
        setIconBaseWithExtension("com/ebixio/virtmus/resources/PlayListNode.png");
    }
    
    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            SystemAction.get(GoLive.class),
            SystemAction.get(SavePlayListAction.class),
            null,
            SystemAction.get(SongNewAction.class),
            SystemAction.get(SongOpenAction.class),
            null,
            SystemAction.get(RenameItemAction.class)
        };
    }
    
    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        PasteType paste = getDropType(t, DnDConstants.ACTION_COPY, -1);
        if (paste != null) s.add(paste);
    }
    
    @Override
    public PasteType getDropType(Transferable t, final int action, int index) {
        final Node dropNode = NodeTransfer.node(t, DnDConstants.ACTION_COPY_OR_MOVE + NodeTransfer.CLIPBOARD_CUT);
        if (dropNode != null) {
            final Song song = dropNode.getLookup().lookup(Song.class);
            
            // Prevent a song for being dropped on the source playlist... ?
            if (song != null && !this.equals(dropNode.getParentNode()) ) {
                return new PasteType() {
                    public Transferable paste() throws IOException {
                        if (playList.type != PlayList.Type.AllSongs) {
                            playList.addSong(song);
                        }
                        if ((action & DnDConstants.ACTION_MOVE) != 0) {
                            final PlayList source = dropNode.getLookup().lookup(PlayList.class);
                            if (source != null && source.type != PlayList.Type.AllSongs) {
                                source.removeSong(song);
                            }
                        }
                        return null;
                    }
                };
            }
        }
        return null;
    }
    
    @Override
    public boolean canRename() { return true; }
    
    public boolean removeSong(Song s) {
        return this.playList.removeSong(s);
    }
}
