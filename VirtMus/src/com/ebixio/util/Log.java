/*
 * Log.java
 *
 * Copyright (C) 2006-2012  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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
package com.ebixio.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.awt.NotificationDisplayer;

/**
 *
 * @author Gabriel Burca <gburca at ebixio dot com>
 */
public class Log {
    private static final Logger logger = Logger.getLogger("com.ebixio.virtmus");
    private static final boolean ENABLED = true;

    public static void log(String msg) {
        log(msg, Level.INFO, false);
    }
    
    public static void log(String msg, Level lev) {
        log(msg, lev, false);
    }
    
    public static void log(String msg, Level lev, boolean printStackDump) {
        if (!ENABLED) return;
        //logger.log(lev, getElapsedTime() + " - " + msg);
        logger.log(lev, msg);
        if (printStackDump) {
            logger.log(lev, "{0}\n", getStackTrace());
        }
    }
    public static void log(Throwable t) {
        log(t.toString());
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        log(pw.toString());
    }
       
    public static String getStackTrace() {
        StringBuilder res = new StringBuilder();
        StackTraceElement[] ste = (new Throwable()).getStackTrace();
        for (StackTraceElement e: ste) {
            res.append("Class: ").append(e.getClassName());
            res.append(" Method: ").append(e.getMethodName());
            res.append(" Line: ").append(e.getLineNumber()).append("\n");
        }
        return res.toString();
    }
}
