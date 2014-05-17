/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.PlayListSet;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

@ActionID(id = "com.ebixio.virtmus.actions.RefreshAction", category = "VirtMus")
@ActionRegistration(displayName = "#CTL_RefreshAction", lazy = false)
@ActionReference(path = "Menu/File", position = 150)
public final class RefreshAction extends CallableSystemAction {

    @Override
    public void performAction() {
        PlayListSet.findInstance().addAllPlayLists(true);
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(RefreshAction.class, "CTL_RefreshAction");
    }

    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/RefreshAction.png";
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
