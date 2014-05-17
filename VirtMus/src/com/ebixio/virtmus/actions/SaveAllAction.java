/*
 * Copyright (C) 2006-2012  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

import com.ebixio.virtmus.PlayListSet;
import com.ebixio.virtmus.VirtMusLookup;
import java.util.Collection;
import javax.swing.SwingUtilities;
import org.netbeans.spi.actions.AbstractSavable;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;

@ActionID(id = "com.ebixio.virtmus.actions.SaveAllAction", category = "VirtMus")
@ActionRegistration(displayName = "#CTL_SaveAllAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Shortcuts", name = "D-S"),
    @ActionReference(path = "Menu/File", name = "SaveAllAction", position = 100),
    @ActionReference(path = "Toolbars/General", name = "SaveAllAction", position = 300)})
public final class SaveAllAction extends NodeAction implements LookupListener {

    private final Lookup.Result<AbstractSavable> lookupSavable;
    
    public SaveAllAction() {
        super();
          
        // Components can obtain this action by doing:
        // SaveAllAction saa = (SaveAllAction)SystemAction.get(SaveAllAction.class);
        
        // All savables get added to this lookup. We register for updates.
        lookupSavable = VirtMusLookup.getInstance().lookupResult(AbstractSavable.class);
        lookupSavable.addLookupListener(this);
    }
    
    @Override
    protected void performAction(Node[] node) {
        PlayListSet.findInstance().saveAll();
        SystemAction.get(SaveAllAction.class).setEnabled(false);
    }

    @Override
    protected boolean enable(Node[] node) {
        return PlayListSet.findInstance().isDirty();
    }

    @Override
    protected boolean surviveFocusChange() {
        return true;
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(SaveAllAction.class, "CTL_SaveAllAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/SaveAllAction.gif";
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
    public void resultChanged(LookupEvent ev) {
        final Collection<? extends AbstractSavable> sNodes = lookupSavable.allInstances();
        
        if (SwingUtilities.isEventDispatchThread()) {
            this.setEnabled(!sNodes.isEmpty());
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setEnabled(!sNodes.isEmpty());
                }
            });
        }
    }

}
