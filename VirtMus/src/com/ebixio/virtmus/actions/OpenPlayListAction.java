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
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

@ActionID(id = "com.ebixio.virtmus.actions.OpenPlayListAction", category = "PlayList")
@ActionRegistration(displayName = "CTL_OpenPlayListAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Shortcuts", name = "DO-O"),
    @ActionReference(path = "Toolbars/PlayList", name = "OpenPlayListAction", position = 200)})
public final class OpenPlayListAction extends CallableSystemAction {
    
    @Override
    public void performAction() {
        MainApp.findInstance().addPlayList();
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(OpenPlayListAction.class, "CTL_OpenPlayListAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/OpenPlayListAction.gif";
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
