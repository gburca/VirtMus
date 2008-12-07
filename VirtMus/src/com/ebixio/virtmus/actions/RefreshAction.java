/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.MainApp;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.actions.CallableSystemAction;

public final class RefreshAction extends CallableSystemAction {

    public void performAction() {
        MainApp.findInstance().refresh();
    }

    public String getName() {
        return NbBundle.getMessage(RefreshAction.class, "CTL_RefreshAction");
    }

    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/RefreshAction.png";
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}
