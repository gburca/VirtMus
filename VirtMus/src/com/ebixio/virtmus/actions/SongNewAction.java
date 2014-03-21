/*
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

import com.ebixio.virtmus.*;
import java.awt.Frame;
import java.io.File;
import javax.swing.JFileChooser;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

@ActionID(id = "com.ebixio.virtmus.actions.SongNewAction", category = "Song")
@ActionRegistration(displayName = "CTL_SongNewAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Shortcuts", name = "D-N"),
    @ActionReference(path = "Toolbars/Song", name = "NewSongAction", position = 100)})
public final class SongNewAction extends CallableSystemAction {
    
    public void performAction() {
        Song s = new Song();
        if (s.saveAs()) {
            PlayList pl = Utilities.actionsGlobalContext().lookup(PlayList.class);
            if (pl != null) {
                pl.addSong(s);
            } else {
                MainApp.findInstance().playLists.get(0).addSong(s);
            }
            // Add the song to the All Songs playlist
            PlayList allSongs = MainApp.findInstance().playLists.get(1);
            if (pl != allSongs) {
                allSongs.addSong(s);
            }
        }
    }
    
    public String getName() {
        return NbBundle.getMessage(SongNewAction.class, "CTL_SongNewAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/NewSongAction.gif";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
}
