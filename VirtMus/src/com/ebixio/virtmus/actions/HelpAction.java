package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.Utils;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

public final class HelpAction extends CallableSystemAction {
    
    public void performAction() {
        try {
            /*
            JOptionPane.showMessageDialog(null, "App: " + Utils.getAppPath() +
            "\nApp1: " + Utils.getAppPath1() +
            "\nApp2: " + Utils.getAppPath2());
             */
            File appPath = Utils.getAppPath();
            if (appPath == null) {
                appPath = Utils.getAppPath1();
            }
            if (appPath == null) {
                appPath = Utils.getAppPath2();
            }
            if (appPath == null) {
                return;
            }
            String appPathS = appPath.getCanonicalPath().replace(File.separatorChar, '/');
            // This URL is only valid when VirtMus is running from the zip distribution
            Utils.openURL("\"file://" + appPathS + "/Docs/index.html\"");
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
