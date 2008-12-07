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
import javax.swing.SwingUtilities;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;

public final class SaveAllAction extends NodeAction {

    public SaveAllAction() {
        super();
        MainApp.findInstance().saveAllAction = this;
    }
    
    protected void performAction(Node[] node) {
        MainApp.findInstance().saveAll();
        SystemAction.get(SaveAllAction.class).setEnabled(false);
    }
    
    public void updateEnable() {
        // TODO: Fix this whole class to use the proper SaveAllAction pattern.
        // setEnabled should only be called from the event thread !!!
        if (SwingUtilities.isEventDispatchThread()) {
            this.setEnabled(MainApp.findInstance().isDirty());
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setEnabled(MainApp.findInstance().isDirty());
                }
            });
        }
    }

    protected boolean enable(Node[] node) {
        return MainApp.findInstance().isDirty();
    }

    @Override
    protected boolean surviveFocusChange() {
        return true;
    }
    
    public String getName() {
        return NbBundle.getMessage(SaveAllAction.class, "CTL_SaveAllAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/SaveAllAction.gif";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

}
