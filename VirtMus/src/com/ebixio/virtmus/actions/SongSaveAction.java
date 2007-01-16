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
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.LocalFileSystem;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.actions.NodeAction;

public final class SongSaveAction extends NodeAction {

    protected void performAction(Node[] activatedNodes) {
        int songs = 0;
        Song s = null;
        for (Node n: activatedNodes) {
            s = (Song) n.getLookup().lookup(Song.class);
            if (s != null && s.isDirty()) {
                s.save();
                songs++;
            }
        }
        
        if (songs == 1) {
            StatusDisplayer.getDefault().setStatusText("Saved song to " + s.getSourceFile().toString());
        } else {
            StatusDisplayer.getDefault().setStatusText("Saved song to " + songs + " song(s)");
        }
        
        setEnabled(false);
    }

//    public void performAction() {
//        Lookup.Result<Song> lr = Utilities.actionsGlobalContext().lookupResult(Song.class);
//        Collection c = lr.allInstances();
//        
//        if (c.size() == 1) {
//            Song s = (Song) c.iterator().next();
//            if (s.save()) {
//                StatusDisplayer.getDefault().setStatusText("Saved song to " + s.getSourceFile().toString());
//            } else {
//                StatusDisplayer.getDefault().setStatusText("Song was NOT saved!");                
//            }
//        } else {
//            MainApp.log("Multiple songs selected?");
//        }
//    }
    
    public String getName() {
        return NbBundle.getMessage(SongSaveAction.class, "CTL_SongSaveAction");
    }
    
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/document-save.png";
    }
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected boolean asynchronous() {
        return false;
    }


    protected boolean enable(Node[] node) {
        for (Node n: node) {
            Song s = (Song) n.getLookup().lookup(Song.class);
            if (s != null && s.isDirty()) return true;
        }
        
        return false;
    }
    
}
