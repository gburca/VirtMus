/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.PlayList;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.NodeAction;

@ActionID(id = "com.ebixio.virtmus.actions.PlayListRevertAction", category = "PlayList")
@ActionRegistration(displayName = "CTL_PlayListRevertAction", lazy = false)
@ActionReference(path = "Menu/PlayList", position = 400)
public final class PlayListRevertAction extends NodeAction {

    protected void performAction(Node[] activatedNodes) {
        for (Node n: activatedNodes) {
            PlayList pl = (PlayList) n.getLookup().lookup(PlayList.class);
            MainApp.findInstance().replacePlayList(pl, PlayList.deserialize(pl.getSourceFile()));
            setEnabled(false);
        }
    }

    public String getName() {
        return NbBundle.getMessage(PlayListRevertAction.class, "CTL_PlayListRevertAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/EditUndo.png";
    }
    
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

