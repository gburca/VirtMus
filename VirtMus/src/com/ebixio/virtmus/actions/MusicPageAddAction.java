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

import com.ebixio.virtmus.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.SystemAction;

@ActionID(id = "com.ebixio.virtmus.actions.MusicPageAddAction", category = "MusicPage")
@ActionRegistration(displayName = "CTL_MusicPageAddAction", lazy = false)
@ActionReference(path = "Toolbars/MusicPage", name = "AddPagesAction", position = 100)
public final class MusicPageAddAction extends CookieAction {
    
    @Override
    protected void performAction(Node[] activatedNodes) {
        Song s = activatedNodes[0].getLookup().lookup(Song.class);
        if (s.addPage()) {
            SystemAction.get(SongSaveAction.class).setEnabled(true);
        }
    }
    
    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(MusicPageAddAction.class, "CTL_MusicPageAddAction");
    }
    
    @Override
    protected Class[] cookieClasses() {
        return new Class[] {
            Song.class
        };
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/AddPagesAction.gif";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
}

