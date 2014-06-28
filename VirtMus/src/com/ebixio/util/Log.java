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

import com.ebixio.virtmus.stats.StatsLogger;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;

/**
 * Log various operational info to var/log/messages.log in the user's
 * .virtmus directory.
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class Log {
    private static final Logger logger = Logger.getLogger("com.ebixio.virtmus");
    private static final boolean ENABLED = true;

    /** Log an INFO message.
     * @param msg Message */
    public static void log(String msg) {
        log(msg, Level.INFO, false);
    }

    public static void log(Level lev, String msg) {
        log(msg, lev, false);
    }

    public static void log(Level level, String msg, Object param) {
        logger.log(level, msg, param);
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
        // The stack trace includes the exception info. No need to log it.
        //log(t.toString());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        log(sw.toString());

        //Exceptions.printStackTrace(t);
        LogRecord rec = new LogRecord(Level.WARNING, "VirtMus Exception");
        rec.setThrown(t);
        StatsLogger.getLogger().log(rec);

        // Could also do:
        //Logger logger = Logger.getLogger("org.netbeans.ui.virtmus");
        //logger.log(Level.SEVERE, "Example exception record", t);
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

    /** Turns on full NetBeans logging for debug purposes.
     * This creates a VirtMus.log file (typically in the app's var/log directory)
     * which contains just the SEVERE entries. Everything else can be found in
     * ~/.virtmus/var/log/messages.log (or build/testuserdir/var/log/messages.log)
     * and in the IDE log.
     *
     * To log to VirtMus.log:
     *  Logger.getLogger("org.netbeans").log(Level.SEVERE, "Test log msg");
     */
    public static void enableDebugLogs() {
        // Pick the loggers to enable
        //Enumeration<String> loggers = LogManager.getLogManager().getLoggerNames();
        String[] loggers = {
            //"org.netbeans.modules.options.OptionsDisplayerImpl",
            //"org.netbeans.core.windows.services.NbPresenter"
            //"org.netbeans.ui",
            //"org.netbeans.modules.uihandler",
            //"org.netbeans.modules.uihandler.Installer",
            //"org.netbeans.modules.uihandler.Installer.class",
            //"com.ebixio.virtmus.metrics",   // From branding
            //"org.netbeans.ui"               // From branding
        };

        try {
            boolean append = false;
            String fn = StatsLogger.getLogFile("VirtMusDebug-%g.log").getPath();
            FileHandler fHandler = new FileHandler(fn, append);
            fHandler.setFormatter(new SimpleFormatter());
            Handler mHandler = new MemoryHandler(fHandler, 1000, Level.SEVERE);

            Logger log;
            //log = Logger.getLogger("org.netbeans");
            //log.addHandler(mHandler);
            //log.setLevel(Level.ALL);

            for (String lgr: loggers) {
                log = Logger.getLogger(lgr);
                log.addHandler(mHandler);
                log.setLevel(Level.ALL);
            }
        } catch (IOException | SecurityException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
        }
    }
}
