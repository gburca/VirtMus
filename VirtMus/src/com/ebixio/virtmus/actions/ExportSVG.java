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

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.MusicPageSVG;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.actions.CookieAction;

/**
 * @author gburca
 */
public final class ExportSVG extends CookieAction {

    protected void performAction(Node[] activatedNodes) {
        MusicPageSVG mp = (MusicPageSVG)activatedNodes[0].getLookup().lookup(MusicPage.class);

        File svgFile = mp.export2SVG(null);
        editSVG(mp, svgFile);
    }

    /**
     * Launches an external SVG editor to edit an SVG file. After the external
     * editor exits, it asks the MusicPage to load the edited SVG file as its
     * annotation.
     */
    protected void editSVG(MusicPageSVG mp, File svgFile) {
        try {
            // Obtain the path to the SVG editor from the user options.
            String svgEditor = NbPreferences.forModule(MainApp.class).get(MainApp.OptSvgEditor, "");
            File editor = new File(svgEditor);
            if (!editor.canExecute()) {
                MainApp.log("Could not execute SVG editor: " + svgEditor);
                return;
            }
            
            List<String> command = new ArrayList<String>();
            
            //command.add("c:\\Program Files\\Inkscape\\inkscape.exe");
            command.add(svgEditor);
            //command.add("-f");
            command.add(svgFile.getCanonicalPath());

            ProcessBuilder builder = new ProcessBuilder(command);
            //Map<String, String> environ = builder.environment();
            //builder.directory(new File(System.getenv("temp")));
            builder.directory(svgFile.getParentFile());

            //System.out.println("Directory : " + System.getenv("temp"));
            final Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                MainApp.log(line);
            }
            MainApp.log("SVG editor program terminated!");
            
            if (svgFile.canRead()) {
                mp.importSVG(svgFile);
            }
            
            if (svgFile.canWrite()) {
                svgFile.delete();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    public String getName() {
        return NbBundle.getMessage(ExportSVG.class, "CTL_ExportSVG");
    }

    protected Class[] cookieClasses() {
        return new Class[]{MusicPage.class};
    }

    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/EditAnnotations.png";
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}