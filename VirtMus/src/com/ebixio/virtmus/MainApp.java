/*
 * MainApp.java
 *
 * Copyright (C) 2006-2014  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

package com.ebixio.virtmus;

import com.ebixio.util.Log;
import com.ebixio.virtmus.options.Options;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.openide.awt.StatusDisplayer;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public final class MainApp {

    private static MainApp instance;

    public static final String VERSION = "4.00";

    /** Creates a new instance of MainApp */
    private MainApp() {

        Preferences pref = NbPreferences.forModule(MainApp.class);
        String version = pref.get(Options.OptAppVersion, "0.00");
        if (! VERSION.equals(version)) {
            pref.put(Options.OptPrevAppVersion, version);
            pref.put(Options.OptAppVersion, VERSION);

            // v4.00 is the first one that kept track of versions.
            if ("0.00".equals(version)) {
                // This is a fresh install, or an upgrade from pre-4.00
            } else {
                // This is an upgrade from post-4.00
            }
        }

        Log.configUiLog();
        //Log.enableDebugLogs();
        Log.submitUiLogs();
        Log.log("MainApp::MainApp start");

        System.getProperties().put("org.icepdf.core.scaleImages", "false");
        System.getProperties().put("org.icepdf.core.awtFontLoading", "true");

        //System.setProperty("nb.show.statistics.ui", "true");
        //System.getProperties().put("nb.show.statistics.ui", "true");

        addSelectionListeners();

        Log.log("MainApp::MainApp finished");
    }

    public static synchronized MainApp findInstance() {
        if (instance == null) {
            instance = new MainApp();
        }
        return instance;
    }

    /**
     * Add listeners for the current selection. When the user changes the selected
     * Song or PlayList, we update the status bar.
     */
    private void addSelectionListeners() {
        PropertyChangeListener pcl = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                    Node[] nodes = (Node[]) evt.getNewValue();
                    if (nodes.length == 0) {return;}
                    Lookup l = nodes[0].getLookup();
                    Collection songs = l.lookupResult(Song.class).allInstances();
                    if (!songs.isEmpty()) {
                        Song s = (Song) songs.iterator().next();
                        displayFile("Song: ", s.getSourceFile());
                    } else {
                        // Let's see if we have a playlist
                        Collection playlists = l.lookupResult(PlayList.class).allInstances();
                        if (!playlists.isEmpty()) {
                            PlayList p = (PlayList) playlists.iterator().next();
                            displayFile("PlayList: ", p.getSourceFile());
                        }
                    }
                }
            }
            private void displayFile(String pre, File f) {
                if (f != null) {
                    setStatusText(pre + f.getAbsolutePath());
                } else {
                    setStatusText(pre + "no file");
                }
            }
        };

        // To update the status bar when songs/playlists are selected
        CommonExplorers.MainExplorerManager.addPropertyChangeListener(pcl);
        CommonExplorers.TagsExplorerManager.addPropertyChangeListener(pcl);
    }

    /** Handle setting the status bar text from non-EDT threads
     * @param msg The status bar text to display. */
    public static void setStatusText(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StatusDisplayer.getDefault().setStatusText(msg);
            }
        });
    }

}
