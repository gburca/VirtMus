/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.PlayList;
import java.io.File;
import java.util.HashMap;
import javax.swing.JOptionPane;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class PlayListDelete extends CookieAction {

    protected void performAction(Node[] activatedNodes) {
        HashMap<File, String> toDelete = new HashMap<File, String>();
        
        for (Node n: activatedNodes) {
            PlayList pl = (PlayList) n.getLookup().lookup(PlayList.class);
            if (pl.type == PlayList.Type.Normal) {
                toDelete.put(pl.getSourceFile(), pl.getName());
            }
        }
        
        String s = com.ebixio.util.Util.join(toDelete.values(), ", ");
        int returnVal = JOptionPane.showConfirmDialog(null,
                        NbBundle.getMessage(PlayListDelete.class, "CTL_PlayListDeleteMbText") +
                        "\n" + s + "?",
                        NbBundle.getMessage(PlayListDelete.class, "CTL_PlayListDeleteMbTitle"),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (returnVal == JOptionPane.NO_OPTION) return;
        
        for (File f: toDelete.keySet()) {
            try {
                f.delete();
            } catch (Exception e) {
                // Ignore exception
            }
        }
        
        MainApp.findInstance().refresh();
    }

    protected int mode() {
        return CookieAction.MODE_ALL;
    }

    public String getName() {
        return NbBundle.getMessage(PlayListDelete.class, "CTL_PlayListDelete");
    }

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

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}

