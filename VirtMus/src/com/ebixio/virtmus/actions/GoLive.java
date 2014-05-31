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
import com.ebixio.virtmus.*;
import com.ebixio.virtmus.options.Options;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.actions.CookieAction;

@ActionID(id = "com.ebixio.virtmus.actions.GoLive", category = "VirtMus")
@ActionRegistration(displayName = "#CTL_GoLive", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Shortcuts", name = "F5"),
    @ActionReference(path = "Menu/View", position = 100),
    @ActionReference(path = "Toolbars/General", name = "GoLive", position = 100)})
public final class GoLive extends CookieAction {
    
    @Override
    protected void performAction(Node[] activatedNodes) {
        LiveWindowJOGL lw = null;
        //Log.log("Java.Library.Path = " + System.getProperty("java.library.path", "NOT SET"));
        Boolean openGL = Boolean.parseBoolean(NbPreferences.forModule(MainApp.class).get(Options.OptUseOpenGL, "false"));
        
        if (openGL) {
            try {
                lw = doLiveWindowJOGL(activatedNodes);
            } catch (UnsatisfiedLinkError e) {
                Log.log(e);
                
                if (lw != null) { lw.dispose(); }
                doLiveWindow(activatedNodes);
            }
        } else {
            doLiveWindow(activatedNodes);
        }

    }
    
    protected LiveWindowJOGL doLiveWindowJOGL(Node[] activatedNodes) {
        MusicPage mp = (MusicPage) activatedNodes[0].getLookup().lookup(MusicPage.class);
        Song s = (Song) activatedNodes[0].getLookup().lookup(Song.class);
        PlayList pl = (PlayList) activatedNodes[0].getLookup().lookup(PlayList.class);
        LiveWindowJOGL lw;

        //LiveWindowJOGL.main(null);    // Only when created form a thread other than EDT
        lw = new LiveWindowJOGL();
        lw.setVisible(true);
        
        if (s != null && mp != null) {
            lw.setSong(s, mp);
        } else if (s != null) {
            lw.setSong(s);
        } else if (pl != null && pl.songs.size() > 0) {
            lw.setPlayList(pl);
        } else if (lw != null) {
            lw.dispose();
            return null;
        }
        
        return lw;
    }
    
    protected void doLiveWindow(Node[] activatedNodes) {
        MusicPage mp = (MusicPage) activatedNodes[0].getLookup().lookup(MusicPage.class);
        Song s = (Song) activatedNodes[0].getLookup().lookup(Song.class);
        PlayList pl = (PlayList) activatedNodes[0].getLookup().lookup(PlayList.class);

        // Acquiring the current Graphics Device and Graphics Configuration
        GraphicsEnvironment graphEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphDevice = graphEnv.getDefaultScreenDevice();
        GraphicsConfiguration graphicConf = graphDevice.getDefaultConfiguration();

        //BufferCapabilities bufCap = graphicConf.getBufferCapabilities();
        //Log.log("Graphics buffering: isPageFlipping() = " + bufCap.isPageFlipping());
        //Log.log("Graphics buffering: isFullScreenRequired() = " + bufCap.isFullScreenRequired());

        LiveWindow lw = new LiveWindow(graphicConf);
        lw.setVisible(true);
        
        if (s != null && mp != null) {
            lw.setLiveSong(s, mp);
        } else if (s != null) {
            lw.setLiveSong(s);
        } else if (pl != null && pl.songs.size() > 0) {
            lw.setPlayList(pl);
        } else {
            lw.dispose();
        }
    }
    
    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(GoLive.class, "CTL_GoLive");
    }
    
    @Override
    protected Class[] cookieClasses() {
        return new Class[] {
            PlayList.class,
            Song.class
        };
    }

    @Override
    protected String iconResource() {
        return "com/ebixio/virtmus/resources/GoLiveAction.gif";
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
