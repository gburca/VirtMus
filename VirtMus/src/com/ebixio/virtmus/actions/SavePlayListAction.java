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
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

@ActionID(id = "com.ebixio.virtmus.actions.SavePlayListAction", category = "PlayList")
@ActionRegistration(displayName = "CTL_SavePlayListAction", lazy = false)
@ActionReference(path = "Toolbars/PlayList", name = "SavePlayListAction", position = 300)
public final class SavePlayListAction extends NodeAction {
    
    @Override
    protected void performAction(Node[] activatedNodes) {
        for (Node n: activatedNodes) {
            PlayList pl = (PlayList) n.getLookup().lookup(PlayList.class);
            if (pl.isDirty()) pl.save();
            setEnabled(false);
        }
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(SavePlayListAction.class, "CTL_SavePlayListAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/SavePlayListAction.png";
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
    protected boolean enable(Node[] node) {
        for (Node n: node) {
            PlayList pl = n.getLookup().lookup(PlayList.class);
            if (pl != null && pl.isDirty()) return true;
        }

        return false;
    }
}

