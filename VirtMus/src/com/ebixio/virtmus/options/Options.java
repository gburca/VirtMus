/*
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
package com.ebixio.virtmus.options;

import com.ebixio.virtmus.MainApp;
import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class Options {
    public static final String OptUseOpenGL = "UseOpenGL";
    public static final String OptPageScrollAmount = "PageScrollPercentage";
    public static final String OptSvgEditor = "SvgEditor";
    // The current application version
    public static final String OptAppVersion = "AppVersion";
    // The previous application version
    public static final String OptPrevAppVersion = "PrevAppVersion";
    // A unique ID to identify this installation
    public static final String OptInstallId = "InstallId";
    public static final String OptPlayListDir = "PlayListDirectory";
    public static final String OptSongDir = "SongDirectory";
    public static final String OptScreenRot = "LiveScreenOrientation";
    public static final String OptPageScrollDir = "ScrollDirection";
    // Set to true if the user allowed the logging of app version
    public static final String OptLogVersion = "LogVersion";
    // What log file set (A or B) we're currently writing to (see StatsLogger)
    public static final String OptLogSet = "LogFileSet";

    public static enum Rotation {
        Clockwise_0, Clockwise_90, Clockwise_180, Clockwise_270;
        // <editor-fold defaultstate="collapsed" desc=" Rotation Behaviors ">
        public double radians() {
            switch(this) {
                case Clockwise_90: return Math.PI / 2;
                case Clockwise_180: return Math.PI;
                case Clockwise_270: return Math.PI / 2 * 3;
                case Clockwise_0:
                default:
                    return 0;
            }
        }
        public int degrees() {
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
        /** Rotates the dimension (if needed)
         * @param d A dimension to rotate.
         * @return The rotated dimension */
        public Dimension getSize(Dimension d) {
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

    public ScrollDir scrollDir;
    public Rotation screenRot;

    private static Options instance;

    private Options() {
        Preferences pref = NbPreferences.forModule(MainApp.class);

        screenRot = Rotation.valueOf( pref.get(Options.OptScreenRot, Rotation.Clockwise_0.toString()) );
        scrollDir = ScrollDir.valueOf( pref.get(Options.OptPageScrollDir, ScrollDir.Horizontal.toString()) );
    }

    public static synchronized Options findInstance() {
        if (instance == null) {
            instance = new Options();
        }
        return instance;
    }
}
