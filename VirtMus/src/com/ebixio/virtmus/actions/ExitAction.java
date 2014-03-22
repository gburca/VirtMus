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

import org.openide.LifecycleManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

@ActionID(id = "com.ebixio.virtmus.actions.ExitAction", category = "VirtMus")
@ActionRegistration(displayName = "#CTL_ExitAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Shortcuts", name = "O-F4"),
    @ActionReference(path = "Menu/File", name = "ExitAction", position = 300)})
public final class ExitAction extends CallableSystemAction {
    
    @Override
    public void performAction() {
        // see: http://www.netbeans.org/download/dev/javadoc/org-openide-util/org/openide/LifecycleManager.html#exit()
        
        // This effectively calls MainApp$VirtMusLifecycleManager.exit()
        LifecycleManager.getDefault().exit();
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(ExitAction.class, "CTL_ExitAction");
    }
    
    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
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
