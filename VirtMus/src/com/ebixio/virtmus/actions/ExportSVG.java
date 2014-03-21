/*
 * Created on Oct 17, 2007, 6:07:17 PM
 * 
 * ExportSVG.java
 * 
 * Copyright (C) 2006-2007  Gabriel Burca (gburca dash virtmus at ebixio dot com)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.ebixio.virtmus.actions;

import com.ebixio.util.Log;
import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.MusicPageSVG;
import java.io.File;
import javax.swing.SwingWorker;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.actions.CookieAction;

/**
 * @author gburca
 */
@ActionID(id = "com.ebixio.virtmus.actions.ExportSVG", category = "MusicPage")
@ActionRegistration(displayName = "CTL_ExportSVG", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Menu/MusicPage", position = 1100),
    @ActionReference(path = "Toolbars/MusicPage", position = 400)})
public final class ExportSVG extends CookieAction {

    @Override
    protected void performAction(Node[] activatedNodes) {
        MusicPageSVG mp = (MusicPageSVG)activatedNodes[0].getLookup().lookup(MusicPage.class);

        // Obtain the path to the SVG editor from the user options.
        String svgEditor = NbPreferences.forModule(MainApp.class).get(MainApp.OptSvgEditor, "");
        File editor = new File(svgEditor);
        if (!editor.canExecute()) {
            Log.log("Could not execute SVG editor: " + svgEditor);
        } else {
//            EditWorker worker = new EditWorker();
//            worker.mp = mp;
//            worker.editor = svgEditor;
//            worker.execute();
            mp.externalSvgEdit(svgEditor);
        }
    }

    private class EditWorker extends SwingWorker<Void, Void> {
        public MusicPageSVG mp;
        public String editor;
        @Override
        protected Void doInBackground() {
            mp.externalSvgEdit(editor);
            return null;
        }

    }
    
    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(ExportSVG.class, "CTL_ExportSVG");
    }

    @Override
    protected Class[] cookieClasses() {
        return new Class[]{MusicPage.class};
    }

    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/EditAnnotations.png";
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