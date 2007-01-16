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

import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.PlayList;
import com.ebixio.virtmus.Song;
import org.openide.actions.RenameAction;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.actions.SystemAction;

public final class RenameItemAction extends RenameAction {
    
    public void performAction(Node[] activatedNodes) {
        Node n = activatedNodes[0];
        String oldName = n.getName();
        super.performAction(activatedNodes);
        String newName = n.getName();

        if (! oldName.equals(newName)) {
            MusicPage mp = n.getLookup().lookup(MusicPage.class);
            Song s = n.getLookup().lookup(Song.class);
            PlayList pl = n.getLookup().lookup(PlayList.class);
            
            if (mp != null) {
                mp.setName(newName);
                SystemAction.get(SongSaveAction.class).setEnabled(true);
            } else if (s != null) {
                s.setName(newName);
                SystemAction.get(SongSaveAction.class).setEnabled(true);
            } else if (pl != null) {
                pl.setName(newName);
                SystemAction.get(SavePlayListAction.class).setEnabled(true);
            }
        }
    }
    
    public String getName() {
        return NbBundle.getMessage(RenameItemAction.class, "CTL_RenameItemAction");
    }
    
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/RenameItemAction.gif";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
}
