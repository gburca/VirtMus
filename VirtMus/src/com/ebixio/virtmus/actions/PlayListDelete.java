/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.PlayList;
import com.ebixio.virtmus.PlayListSet;
import java.util.HashMap;
import javax.swing.JOptionPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

@ActionID(id = "com.ebixio.virtmus.actions.PlayListDelete", category = "PlayList")
@ActionRegistration(displayName = "#CTL_PlayListDelete", lazy = false)
@ActionReference(path = "Menu/PlayList", position = 500)
public final class PlayListDelete extends CookieAction {

    @Override
    protected void performAction(Node[] activatedNodes) {
        HashMap<PlayList, String> toDelete = new HashMap<>();
        
        for (Node n: activatedNodes) {
            PlayList pl = (PlayList) n.getLookup().lookup(PlayList.class);
            if (pl.type == PlayList.Type.Normal) {
                toDelete.put(pl, pl.getName());
            }
        }
        
        String s = com.ebixio.util.Util.join(toDelete.values(), ", ");
        int returnVal = JOptionPane.showConfirmDialog(null,
                        NbBundle.getMessage(PlayListDelete.class, "CTL_PlayListDeleteMbText") +
                        "\n" + s + "?",
                        NbBundle.getMessage(PlayListDelete.class, "CTL_PlayListDeleteMbTitle"),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (returnVal == JOptionPane.NO_OPTION) return;
        
        for (PlayList p: toDelete.keySet()) {
            PlayListSet.findInstance().deletePlayList(p);
        }
        PlayListSet.findInstance().addAllPlayLists(true);
    }

    @Override
    protected int mode() {
        return CookieAction.MODE_ALL;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(PlayListDelete.class, "CTL_PlayListDelete");
    }

    @Override
    protected Class[] cookieClasses() {
        return new Class[]{PlayList.class};
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/RemovePagesAction.gif";
    }
    
    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
        //putValue("noIconInMenu", Boolean.TRUE);
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

