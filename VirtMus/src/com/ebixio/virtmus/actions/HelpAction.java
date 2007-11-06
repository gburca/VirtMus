package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.Utils;
import java.io.File;
import java.io.IOException;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

public final class HelpAction extends CallableSystemAction {
    
    public void performAction() {
        try {
            /*
            JOptionPane.showMessageDialog(null, "App1: " + Utils.getAppPath() +
            "\nApp1: " + Utils.getAppPath1() +
            "\nApp2: " + Utils.getAppPath2());
             */
            String appPath = Utils.getAppPath().getCanonicalPath();
            appPath = appPath.replace(File.separatorChar, '/');
            // This URL is only valid when VirtMus is running from the zip distribution
            Utils.openURL("\"file://" + appPath + "/Docs/VirtMus.html\"");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public String getName() {
        return NbBundle.getMessage(HelpAction.class, "CTL_HelpAction");
    }
    
    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
}
