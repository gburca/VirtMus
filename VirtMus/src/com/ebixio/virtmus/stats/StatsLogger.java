/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ebixio.virtmus.stats;

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.Utils;
import com.ebixio.virtmus.options.Options;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;
import java.util.prefs.Preferences;
import java.util.zip.GZIPOutputStream;
import org.json.JSONStringer;
import org.openide.awt.HtmlBrowser;
import org.openide.modules.Places;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.openide.util.io.NullOutputStream;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class StatsLogger {
    private static final Logger LOG = Logger.getLogger(StatsLogger.class.getName());
    private static final Logger statsLog = Logger.getLogger("com.ebixio.virtmus.stats");
    private String logSet;
    private Handler logHandler;
    private Preferences pref = NbPreferences.forModule(StatsLogger.class);

    //static final String UI_LOGGER = NbBundle.getMessage(Installer.class, "UI_LOGGER_NAME");
    // OK, Maybe Later, Never

    public StatsLogger() {
        try {

            statsLog.setUseParentHandlers(false);
            statsLog.setLevel(Level.ALL);

            logSet = pref.get(Options.OptLogSet, "A");
            logHandler = makeLogHandler(logSet);
            if (logHandler != null) {
                statsLog.addHandler(logHandler);
                //log.removeHandler(fHandler);
            } else {
                LOG.log(Level.INFO, "Stats logging init failed.");
                statsLog.setLevel(Level.OFF);
            }

            //log.addHandler(new StatsLoggerHandler());

            statsLog.log(getCpuInfo());
            statsLog.log(getScreenSizeInfo());
            // TODO: Log memory, java version, etc...

        } catch (SecurityException ex) {
            LOG.log(Level.FINEST, null, ex);
        }
    }

    public void submitLogs() {
        String oldLogSet = rotate();
        uploadLogs(oldLogSet);
    }

    private boolean uploadLogs(final String oldLogSet) {
        if (oldLogSet == null) return false;

        File logsDir = getLogsDir();
        if (logsDir == null) return false;

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("VirtMus-" + oldLogSet + "-") &&
                        name.endsWith(".log");
            }
        };

        for (File f: logsDir.listFiles(filter)) {
            GZIPOutputStream gzip; // line 1287
        }

        return true;
    }

    private boolean upload1Log(File logFile) {
        try {
            URL url = new URL("http://ebixio.com/virtmus/analytics2");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setReadTimeout(10 * 1000);
            conn.setDoOutput(true);
            conn.setDoInput(true);  // To read the response
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "x-application/gzip");
            conn.setRequestProperty("User-Agent", "VirtMus-" + MainApp.VERSION);

            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            GZIPOutputStream gzip = new GZIPOutputStream(os);
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(logFile))) {

            }
            gzip.finish();
            os.flush();
            os.close();

        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return true;
    }

    private synchronized String rotate() {
        String oldLogSet = logSet;

        // Switch over the logging to the new log set
        if (oldLogSet.equals("A")) {
            logSet = "B";
        } else {
            logSet = "A";
        }

        Handler newLogHandler = makeLogHandler(logSet);
        if (newLogHandler != null) {
            statsLog.removeHandler(logHandler);
            statsLog.addHandler(newLogHandler);
            logHandler.close();
            logHandler = newLogHandler;
            pref.put(Options.OptLogSet, logSet);
        } else {
            logSet = oldLogSet;
            return null;
        }

        return oldLogSet;
    }

    private Handler makeLogHandler(String newLogSet) {
        Handler handler = null;
        try {
            String pattern = "VirtMus-" + newLogSet + "-%g.log";
            File logFile = getLogFile(pattern);
            handler = new FileHandler(logFile.getAbsolutePath(),
                    50*1000*1000, 10, true);
            handler.setFormatter(new XMLFormatter());
            handler.setEncoding("utf-8");
        } catch (IOException | SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
        return handler;
    }

    static LogRecord getCpuInfo() {
        LogRecord log = new LogRecord(Level.INFO, "CPU Info");
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Object [] params = new Object[]{os.getAvailableProcessors(), os.getArch()};
        log.setParameters(params);
        return log;
    }

    static LogRecord getScreenSizeInfo() {
        LogRecord log = new LogRecord(Level.INFO, "Screen Size");
        List<Object> params = new ArrayList<>();

        int screens = Utils.getNumberOfScreens();
        params.add(screens);

        Dimension[] sizes = Utils.getScreenSizes();
        for (Dimension d: sizes) {
            params.add(d.width);
            params.add(d.height);
        }

        log.setParameters(params.toArray());
        return log;
    }

    public static void logVersion(String prevVersion, boolean statsEnabled) {
        try {
            URL url = new URL("http://ebixio.com/virtmus/analytics2");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setReadTimeout(10 * 1000);
            conn.setDoOutput(true);
            conn.setDoInput(true);  // To read the response
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "VirtMus-" + MainApp.VERSION);

            try (DataOutputStream outS = new DataOutputStream(conn.getOutputStream())) {
                String postData = new JSONStringer()
                    .object()
                        .key("version").value(MainApp.VERSION)
                        .key("installId").value(MainApp.getInstallId())
                        .key("prevVersion").value(prevVersion)
                        .key("statsEnabled").value(statsEnabled)
                    .endObject().toString();

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
        }   catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /** The directory where the logs are stored. */
    private File getLogsDir() {
        File userDir = Places.getUserDirectory();
        if (userDir != null) {
            return new File(new File(userDir, "var"), "log");
        } else {
            return null;
        }
    }

    /** This is the file we will log to (and upload). */
    private File getLogFile(String pattern) {
        if (pattern == null) {
            pattern = "VirtMus.log";
        }
        File logsDir = getLogsDir();
        if (logsDir != null) {
            return new File(logsDir, pattern);
        } else {
            return null;
        }
    }

    private class StatsLoggerHandler extends Handler {
        private File logFile;
        private OutputStream os;
        private ExecutorService executor;

        public StatsLoggerHandler() {
            logFile = getLogFile();
            if (logFile != null) {
                try {
                    logFile.getParentFile().mkdirs();
                    boolean append = true;
                    os = new BufferedOutputStream(new FileOutputStream(logFile, append));
                } catch (FileNotFoundException ex) {
                    LOG.log(Level.FINEST, null, ex);
                    os = new NullOutputStream();
                }
            } else {
                os = new NullOutputStream();
            }

            // How do we want the log messages formatted
            this.setFormatter(new XMLFormatter());

            executor = Executors.newSingleThreadExecutor();
        }

        @Override
        public void publish(LogRecord record) {

            writeLog(os, record);
        }

        @Override
        public void flush() {
            try {
                os.flush();
            } catch (IOException ex) {
                LOG.log(Level.FINEST, null, ex);
            }
        }

        @Override
        public void close() throws SecurityException {
            try {
                os.close();
            } catch (IOException ex) {
                LOG.log(Level.FINEST, null, ex);
            }
        }

        /** The directory where the logs are stored. */
        private File getLogsDir() {
            File userDir = Places.getUserDirectory();
            if (userDir != null) {
                return new File(new File(userDir, "var"), "log");
            } else {
                return null;
            }
        }

        /** This is the file we will log to (and upload). */
        private File getLogFile() {
            File logsDir = getLogsDir();
            if (logsDir != null) {
                return new File(logsDir, "VirtMus.log");
            } else {
                return null;
            }
        }

        private void writeLog(OutputStream os, LogRecord rec) {
            try {
                String log = this.getFormatter().format(rec);
                os.write(log.getBytes("utf-8"));
                os.flush();
            } catch (IOException ignore) {
                LOG.log(Level.FINEST, null, ignore);
            }
        }
    }
}
