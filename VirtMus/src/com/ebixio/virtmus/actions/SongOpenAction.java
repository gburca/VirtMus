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

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.PlayList;
import com.ebixio.virtmus.PlayListNode;
import com.ebixio.virtmus.Song;
import java.beans.PropertyVetoException;
import javax.swing.Action;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.actions.SystemAction;

public final class SongOpenAction extends CallableSystemAction {
    
    public void performAction() {
        Song s = Song.open();
        
        if (s != null) {
            PlayList pl = Utilities.actionsGlobalContext().lookup(PlayList.class);
            PlayListNode pln = Utilities.actionsGlobalContext().lookup(PlayListNode.class);
            
            if (pl != null) {
                pl.addSong(s);
            } else {
                MainApp.findInstance().playLists.get(0).addSong(s);
                // No need to allow saving this playlist
            }
            
            // Needed to enable SavePlayListAction without changing the selected node
            Action a = SystemAction.get(SavePlayListAction.class);
            if (pl.type == PlayList.Type.Normal) {
                a.setEnabled(true);
            } else {
                a.setEnabled(false);
            }

//                ExplorerManager em = MainApp.findInstance().getExplorerManager();
//                Node[] sn = em.getSelectedNodes();
//                for (Node n: sn) {
//                    if (n.getClass() == PlayListNode.class && ((PlayListNode)n).playList == pl) {
//                        for (Action a: n.getActions(true)) {
//                            if (a != null && a.getClass() == SavePlayListAction.class) {
//                                a.setEnabled(true);
//                            }
//                        }
//                    }
//                }
            
        }
    }
    
    public String getName() {
        return NbBundle.getMessage(SongOpenAction.class, "CTL_SongOpenAction");
    }
    
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/SongOpenAction.png";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
}