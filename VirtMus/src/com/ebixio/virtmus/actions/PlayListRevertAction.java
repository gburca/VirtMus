/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.PlayList;
import com.ebixio.virtmus.PlayListSet;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

@ActionID(id = "com.ebixio.virtmus.actions.PlayListRevertAction", category = "PlayList")
@ActionRegistration(displayName = "#CTL_PlayListRevertAction", lazy = false)
@ActionReference(path = "Menu/PlayList", position = 400)
public final class PlayListRevertAction extends NodeAction {

    @Override
    protected void performAction(Node[] activatedNodes) {
        for (Node n: activatedNodes) {
            PlayList pl = (PlayList) n.getLookup().lookup(PlayList.class);
            pl.setDirty(false); // To remove PlayListSavable from the lookup
            PlayListSet.findInstance().replacePlayList(pl, PlayList.deserialize(pl.getSourceFile()));
            setEnabled(false);
        }
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(PlayListRevertAction.class, "CTL_PlayListRevertAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/EditUndo.png";
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

