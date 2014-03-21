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
import com.ebixio.virtmus.MusicPageNode;
import com.ebixio.virtmus.Song;
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

@ActionID(id = "com.ebixio.virtmus.actions.MusicPageCloneAction", category = "MusicPage")
@ActionRegistration(displayName = "CTL_MusicPageCloneAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Menu/MusicPage", position = 200),
    @ActionReference(path = "Toolbars/MusicPage", name = "MusicPageCloneAction", position = 200)})
public final class MusicPageCloneAction extends NodeAction {
    
    public void performAction(Node[] activatedNodes) {
        for (Node n: activatedNodes) {
            Song song = (Song) n.getLookup().lookup(Song.class);
            MusicPage mp =(MusicPage) n.getLookup().lookup(MusicPage.class);
            if (song != null && mp != null) {
                int index = song.pageOrder.indexOf(mp);
                song.addPage(mp.clone(), index + 1);
                SystemAction.get(SongSaveAction.class).setEnabled(true);
            }
        }
    }
    
    public String getName() {
        return NbBundle.getMessage(MusicPageCloneAction.class, "CTL_MusicPageCloneAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/CloneMusicPage.gif";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }

    protected boolean enable(Node[] nodes) {
        for (Node n: nodes) {
            MusicPage mp = (MusicPage) n.getLookup().lookup(MusicPage.class);
            if (mp != null) return true;
        }
        return false;
    }
    
}
