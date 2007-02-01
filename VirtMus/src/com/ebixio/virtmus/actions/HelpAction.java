package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.Utils;
import javax.swing.JOptionPane;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

public final class HelpAction extends CallableSystemAction {
    
    public void performAction() {
        /*
        JOptionPane.showMessageDialog(null, "App1: " + Utils.getAppPath() +
                "\nApp1: " + Utils.getAppPath1() +
                "\nApp2: " + Utils.getAppPath2());
         */
        Utils.openURL("file://" + Utils.getAppPath() + "/Docs/index.html");
    }
    
    public String getName() {
        return NbBundle.getMessage(HelpAction.class, "CTL_HelpAction");
    }
    
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected boolean asynchronous() {
        return false;
    }
    
}
