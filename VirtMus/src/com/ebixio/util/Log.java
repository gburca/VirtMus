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

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.options.Options;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;
import javax.swing.SwingWorker;
import org.netbeans.modules.uihandler.api.Controller;
import org.openide.awt.HtmlBrowser;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 * Log various operational info to var/log/messages.log in the user's
 * .virtmus directory.
 * 
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
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
        // The stack trace includes the exception info. No need to log it.
        //log(t.toString());
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        log(sw.toString());
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

    /** Configures the UI Gesture logging. */
    public static void configUiLog() {
        /* When enabled, it prompts the user to upload exceptions to ERROR_URL.
        This creates a dialog box that includes a Username/Password field as well
        as an offer to register. We don't support userid's and registration, so
        disable it to prevent confusion.
        
        See:
            org/netbeans/modules/exceptions/Bundle.properties
            File system: uihandler.exceptionreporter
        */
        Controller.getDefault().setEnableExceptionHandler(false);
        Preferences pref = NbPreferences.forModule(MainApp.class);
        
        // Assign an InstallId if it's not set.
        long installId = pref.getLong(Options.OptInstallId, 0);
        if (installId == 0) {
            Random r = new Random();
            while (installId <= 0) installId = r.nextLong();
            pref.putLong(Options.OptInstallId, installId);
        }
        
        String prevVersion = pref.get(Options.OptPrevAppVersion, "0.00");

        LogRecord rec = new LogRecord(Level.INFO, "VIRTMUS");
        rec.setParameters(new Object[]{MainApp.VERSION, installId, prevVersion});
        Log.uiLog(rec);
        
        Preferences corePref = NbPreferences.root().node("org/netbeans/core");
        
        if (pref.getBoolean(Options.OptLogVersion, true)) {
            if (!corePref.getBoolean("usageStatisticsEnabled", true)) {
                pref.putBoolean(Options.OptLogVersion, false);
                Log.logVersion(installId, prevVersion, false);
            } else {
                Log.logVersion(installId, prevVersion, true);
            }
        }
    }
    
    /** Adds an entry to the UI Gesture log. This log is uploaded to
     * virtmus.com only if the user allows it.
     * @param rec A log record to log. */
    public static void uiLog(final LogRecord rec) {
        /* This logger should match branding/.../Bundle.properties UI_LOGGER_NAME
        since that's what is submitted to the web server. Changes to this code,
        or to Bundle.properties generally require a clean+rebuild.

        If UI_LOGGER_NAME is com.ebixio.virtmus.ui, we won't get some of the info
        NetBeans already logs (memory available, etc...). See:
            http://wiki.netbeans.org/UILoggingInPlatform
        */
        //Logger logger = Logger.getLogger("com.ebixio.virtmus.ui");
        Logger.getLogger("org.netbeans.ui.virtmus").log(rec);
    }
    
    /** Adds an entry to the UI Gesture log.
     * @param t Exception to log. */
    public static void uiLog(final Throwable t) {
        //Exceptions.printStackTrace(t);
        LogRecord rec = new LogRecord(Level.WARNING, "VIRTMUS_EX");
        rec.setThrown(t);
        uiLog(rec);

        // Could also do:
        //Logger logger = Logger.getLogger("org.netbeans.ui.virtmus");
        //logger.log(Level.SEVERE, "Example exception record", t);
    }

    /** For debug purposes, we can force a UI Gesture log submission.
     * The UI Gesture module is not flexible enough for our purposes. We can't
     * control when or how often the logs are submitted. We also can't control
     * the post-upload behavior. We must return some bogus HTML or else the logs
     * that were just submitted are not erased. Etc...
     *
     * UI logs get uploaded after 1000 logs, or 20Mb (UIHandler.java)
     * Metrics logs get uploaded after 400 logs, 33+rand(14) days, or 10Mb (MetricsHandler.java)
     */
    public static void submitUiLogs() {
        (new SwingWorker<String, Object>() {
            @Override
            protected String doInBackground() throws Exception {
                Controller ctrlr = Controller.getDefault();

                /* Auto-submit should be true after the first submit, since in the
                HTML form, we only give the user the option to auto-submit, so once
                they submit, auto-submit is enabled. */
                Log.log("RecCnt = " + ctrlr.getLogRecordsCount() +
                    " Auto-submit: " + ctrlr.isAutomaticSubmit());

                /* Only the UI Gesture logs (not the metrics) are submitted this
                way, because Controller.submit() calls:
                    Installer.displaySummary("WELCOME_URL", true, false, true);
                Which then calls:
                    displaySummary(msg, explicit, auto, connectDialog, DataType.DATA_UIGESTURE, null, null);
                This also forces the dialog to display, even if auto-submit was
                previously selected.
                */
                ctrlr.submit();
                return null;
            }
        }).execute();
    }

    public static void logVersion(long installId, String prevVersion, boolean statsEnabled) {
        try {
            URL url = new URL("http://ebixio.com/virtmus/analytics");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setReadTimeout(10 * 1000);
            conn.setDoOutput(true);
            conn.setDoInput(true);  // To read the response
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "VirtMus-" + MainApp.VERSION);
            
            try (DataOutputStream outS = new DataOutputStream(conn.getOutputStream())) {
                String postData =
                        "version="          + URLEncoder.encode(MainApp.VERSION, "UTF-8") +
                        "&installId="       + String.valueOf(installId) +
                        "&prevVersion="      + URLEncoder.encode(prevVersion, "UTF-8") +
                        "&statsEnabled="    + Boolean.toString(statsEnabled);
                outS.writeBytes(postData);
                outS.flush();
            }
            
            StringBuilder rsp = new StringBuilder();            
            InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String buff;
            while ((buff = br.readLine()) != null && rsp.length() < 1e6) {
                rsp.append(buff);
            }
            
            //Log.log("HTTP Response: " + conn.getResponseCode() + " " + rsp.toString());

            // This can be used to notify the user that a newer version is available
            if (rsp.length() > 0) {
                File f = File.createTempFile("VersionPost", "html");
                f.deleteOnExit();
                try (FileWriter w = new FileWriter(f)) {
                    w.write(rsp.toString());
                    URL rspUrl = Utilities.toURI(f).toURL();
                    HtmlBrowser.URLDisplayer.getDefault().showURL(rspUrl);
                }
            }
            
        } catch (MalformedURLException ex) {
            uiLog(ex);
        } catch (IOException ex) {
            uiLog(ex);
        }
    }

    /** Turns on full NetBeans logging for debug purposes.
     * This creates a VirtMus.log file (typically in the app's root directory)
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
            "org.netbeans.modules.uihandler",
            "org.netbeans.modules.uihandler.Installer",
            "org.netbeans.modules.uihandler.Installer.class",
            //"com.ebixio.virtmus.metrics",   // From branding
            //"org.netbeans.ui"               // From branding
        };
                            
        try {
            boolean append = false;
            FileHandler fHandler = new FileHandler("VirtMus.log", append);
            fHandler.setFormatter(new SimpleFormatter());
            Handler mHandler = new MemoryHandler(fHandler, 1000, Level.SEVERE);

            Logger log = Logger.getLogger("org.netbeans");
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
