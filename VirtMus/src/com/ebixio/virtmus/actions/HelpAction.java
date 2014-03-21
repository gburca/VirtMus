package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.Utils;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

@ActionID(id = "com.ebixio.virtmus.actions.HelpAction", category = "Help")
@ActionRegistration(displayName = "CTL_HelpContentsAction", lazy = false)
@ActionReference(path = "Menu/Help", position = 101)
public final class HelpAction extends CallableSystemAction {
    
    @Override
    public void performAction() {
        try {
            /*
            JOptionPane.showMessageDialog(null, "App: " + Utils.getAppPath() +
            "\nApp1: " + Utils.getAppPath1() +
            "\nApp2: " + Utils.getAppPath2());
             */
            File appPaths[] = { Utils.getAppPath(), Utils.getAppPath1(), Utils.getAppPath2() };
            for (File appPath: appPaths) {
                if (appPath == null) continue;
                String appPathS = appPath.getCanonicalPath().replace(File.separatorChar, '/');
                appPathS += "/Docs/index.html";
                File index = new File(appPathS);
                if (index.canRead()) {
                    Utils.openURL("file://" + appPathS);
                    return;
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(HelpAction.class, "CTL_HelpAction");
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
