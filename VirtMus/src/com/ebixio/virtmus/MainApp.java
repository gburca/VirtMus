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
import com.ebixio.virtmus.stats.StatsLogger;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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

        Log.log("MainApp::MainApp start");
        //Log.enableDebugLogs();

        Preferences pref = NbPreferences.forModule(MainApp.class);
        String version = pref.get(Options.OptAppVersion, "0.00");
        if (! VERSION.equals(version)) {
            pref.put(Options.OptPrevAppVersion, version);
            pref.put(Options.OptAppVersion, VERSION);

            // Reset counter to re-prompt user to contribute stats
            pref.putInt(Options.OptStartCounter, 1);
            pref.putBoolean(Options.OptCheckVersion, true);

            LogRecord lr = new LogRecord(Level.INFO, "VIRTMUS_UPGRADE");
            lr.setParameters(new Object[]{getInstallId(), version, VERSION});
            StatsLogger.getLogger().log(lr);

            // v4.00 is the first one that kept track of versions.
            if ("0.00".equals(version)) {
                // This is a fresh install, or an upgrade from pre-4.00
            } else {
                // This is an upgrade from post-4.00
            }
        }

        // Figure out the new version before calling startedUp().
        StatsLogger.findInstance().startedUp();

        System.getProperties().put("org.icepdf.core.scaleImages", "false");
        System.getProperties().put("org.icepdf.core.awtFontLoading", "true");

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

    /** Get the random ID that identifies this VirtMus installation.
     * @return  A random ID that's unique to this installation. */
    public static long getInstallId() {
        Preferences pref = NbPreferences.forModule(MainApp.class);

        // Assign an InstallId if it's not set.
        long installId = pref.getLong(Options.OptInstallId, 0);
        if (installId == 0) {
            Random r = new Random();
            while (installId <= 0) installId = r.nextLong();
            pref.putLong(Options.OptInstallId, installId);
        }

        return installId;
    }

}
