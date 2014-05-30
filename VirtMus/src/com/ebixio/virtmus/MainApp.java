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
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.Date;
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

    public static enum Rotation {
        Clockwise_0, Clockwise_90, Clockwise_180, Clockwise_270;
        // <editor-fold defaultstate="collapsed" desc=" Rotation Behaviors ">
        double radians() {
            switch(this) {
                case Clockwise_90: return Math.PI / 2;
                case Clockwise_180: return Math.PI;
                case Clockwise_270: return Math.PI / 2 * 3;
                case Clockwise_0:
                default:
                    return 0;
            }
        }
        int degrees() {
            switch(this) {
                case Clockwise_90: return 90;
                case Clockwise_180: return 180;
                case Clockwise_270: return 270;
                case Clockwise_0:
                default:
                    return 0;
            }
        }
        public AffineTransform getTransform(Dimension d) {
            switch (this) {
                case Clockwise_90:
                    /**
                     * Writes on surface of dimension d at 90 degrees clockwise
                     * [ 0  -1  width ]
                     * [ 1   0    0   ]
                     * [ 0   0    1   ]
                     *
                     * x' = width - y
                     * y' = x
                     */
                    return new AffineTransform(0, 1, -1, 0, d.width, 0);
                case Clockwise_180:
                    /**
                     * Writes upside down
                     * [ -1  0  width ]
                     * [  0 -1  height]
                     * [  0  0     1  ]
                     *
                     * x' = width - x
                     * y' = height - y
                     */
                    return new AffineTransform(-1, 0, 0, -1, d.width, d.height);
                case Clockwise_270:
                    /**
                     * [  0  1    0    ]
                     * [ -1  0  height ]
                     * [  0  0    1    ]
                     *
                     * x' = y
                     * y' = height - x
                     */
                    return new AffineTransform(0, -1, 1, 0, 0, d.height);
                case Clockwise_0:
                default:
                    return new AffineTransform();
            }
        }
        /** Rotates the dimension (if needed) */
        Dimension getSize(Dimension d) {
            switch(this) {
                case Clockwise_90:
                case Clockwise_270:
                    return new Dimension(d.height, d.width);
                default:
                    return new Dimension(d);
            }
        }
        // </editor-fold>
    }
    public static enum ScrollDir { Vertical, Horizontal }
    public Rotation screenRot;
    public ScrollDir scrollDir;

    public static final String OptPlayListDir       = "PlayListDirectory";
    public static final String OptSongDir           = "SongDirectory";
    public static final String OptScreenRot         = "LiveScreenOrientation";
    public static final String OptPageScrollAmount  = "PageScrollPercentage";
    public static final String OptPageScrollDir     = "ScrollDirection";
    public static final String OptUseOpenGL         = "UseOpenGL";
    public static final String OptSvgEditor         = "SvgEditor";
    public static final String OptInstallId         = "InstallId";
    public static final String OptLogVersion        = "LogVersion";

    /** Creates a new instance of MainApp */
    private MainApp() {
        Log.configUiLog();
        //Log.enableDebugLogs();
        Log.log("MainApp::MainApp start");

        System.getProperties().put("org.icepdf.core.scaleImages", "false");
        System.getProperties().put("org.icepdf.core.awtFontLoading", "true");

        //System.setProperty("nb.show.statistics.ui", "true");
        //System.getProperties().put("nb.show.statistics.ui", "true");

        Preferences pref = NbPreferences.forModule(MainApp.class);

        screenRot = Rotation.valueOf( pref.get(OptScreenRot, Rotation.Clockwise_0.toString()) );
        scrollDir = ScrollDir.valueOf( pref.get(OptPageScrollDir, ScrollDir.Horizontal.toString()) );

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

    // <editor-fold defaultstate="collapsed" desc=" Listeners ">

    // </editor-fold>
}
