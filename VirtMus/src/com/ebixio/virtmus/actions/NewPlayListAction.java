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
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

@ActionID(id = "com.ebixio.virtmus.actions.NewPlayListAction", category = "PlayList")
@ActionRegistration(displayName = "#CTL_NewPlayListAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Shortcuts", name = "D-S-N"),
    @ActionReference(path = "Toolbars/PlayList", name = "NewPlayListAction", position = 100)})
public final class NewPlayListAction extends CallableSystemAction {
    
    @Override
    public void performAction() {
        PlayList pl = new PlayList();
        if (pl.saveAs()) {
            PlayListSet.findInstance().addPlayList(pl);
        }
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(NewPlayListAction.class, "CTL_NewPlayListAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/NewPlayListAction.png";
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
