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
package com.ebixio.virtmus;

import com.ebixio.util.Log;
import com.ebixio.virtmus.stats.StatsLogger;
import java.awt.EventQueue;
import org.openide.awt.ToolbarPool;
import org.openide.modules.ModuleInstall;
import org.openide.windows.WindowManager;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {

            @Override
            public void run() {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        StatsLogger.findInstance().startingUp();
                        Log.log("VirtMus module: restored");
                        ToolbarPool.getDefault().setConfiguration("StandardToolbar");
                        MainApp.findInstance();
                    }
                });
            }
        });
    }
}
