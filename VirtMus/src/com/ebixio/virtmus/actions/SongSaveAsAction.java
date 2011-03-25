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

import com.ebixio.util.Log;
import com.ebixio.virtmus.Song;
import java.util.Collection;
import org.openide.awt.StatusDisplayer;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.CookieAction;

public final class SongSaveAsAction extends CookieAction {
    
    @Override
    protected void performAction(Node[] node) {
        Lookup.Result<Song> lr = Utilities.actionsGlobalContext().lookupResult(Song.class);
        Collection c = lr.allInstances();
        
        if (c.size() == 1) {
            Song s = (Song) c.iterator().next();
            if (s.saveAs()) {
                StatusDisplayer.getDefault().setStatusText("Saved song as " + s.getSourceFile().toString());
            } else {
                StatusDisplayer.getDefault().setStatusText("Song was NOT saved!");                
            }
        } else {
            Log.log("Multiple songs selected?");
        }
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(SongSaveAsAction.class, "CTL_SongSaveAsAction");
    }
    
    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/document-save-as.png";
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    protected Class<?>[] cookieClasses() {
        return new Class[] {Song.class};
    }

}
