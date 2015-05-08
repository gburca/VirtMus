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

package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.PlayList;
import com.ebixio.virtmus.PlayListNode;
import com.ebixio.virtmus.Song;
import com.ebixio.virtmus.SongNode;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;

@ActionID(id = "com.ebixio.virtmus.actions.SongRemoveAction", category = "Song")
@ActionRegistration(displayName = "#CTL_SongRemoveAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Menu/Song"),
    @ActionReference(path = "Toolbars/Song", name = "SongRemoveAction", position = 300)})
public final class SongRemoveAction extends NodeAction {
    
    @Override
    public void performAction(Node[] activatedNodes) {
        for (Node n: activatedNodes) {
            PlayList pl = (PlayList) n.getLookup().lookup(PlayList.class);
            Song s = (Song) n.getLookup().lookup(Song.class);
            
            if (pl != null && s != null) {
                pl.removeSong(s);
                
                Action a = SystemAction.get(SavePlayListAction.class);
                if (pl.type != PlayList.Type.AllSongs) {
                    a.setEnabled(true);
                } else {
                    a.setEnabled(false);
                }

            }
        }
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(SongRemoveAction.class, "CTL_SongRemoveAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/SongRemoveAction.gif";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    protected boolean enable(Node[] nodes) {
        // Make sure all nodes are "song" nodes
        boolean result = false;
        for (Node n: nodes) {
            Song s = (Song) n.getLookup().lookup(Song.class);
            PlayList pl = (PlayList) n.getLookup().lookup(PlayList.class);
            if (s == null) {
                return false;
            } else {
                result = true;
            }
            if (pl == null || pl.type == PlayList.Type.AllSongs) return false;
        }
        
        return result;
    }
    
}
