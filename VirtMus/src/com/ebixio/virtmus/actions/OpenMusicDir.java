/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

@ActionID(id = "com.ebixio.virtmus.actions.OpenMusicDir", category = "VirtMus")
@ActionRegistration(iconBase = "com/ebixio/virtmus/resources/OpenPlayListAction.gif", displayName = "#CTL_OpenMusicDir", iconInMenu = true)
@ActionReference(path = "Menu/File", position = 0)
public final class OpenMusicDir implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        //OptionsDisplayer.getDefault().open();
        // The simple way above, or the more generic way below
        FileObject fo = FileUtil.getConfigRoot().getFileObject("Actions/Window/org-netbeans-modules-options-OptionsWindowAction.instance");
        DataObject dataObj;
        try {
            dataObj = DataObject.find(fo);
            InstanceCookie ic = dataObj.getLookup().lookup(InstanceCookie.class);
            Action a = (Action)ic.instanceCreate();
            a.actionPerformed(e);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }

    }
}
