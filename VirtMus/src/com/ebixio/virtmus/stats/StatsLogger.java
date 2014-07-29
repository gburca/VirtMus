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
package com.ebixio.virtmus.stats;

import com.ebixio.util.Log;
import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.options.Options;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.swing.JOptionPane;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONStringer;
import org.openide.awt.HtmlBrowser;
import org.openide.modules.Places;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;

/**
 * Logs anonymous statistics to a file, and optionally uploads the logs.
 *
 * Every time VirtMus starts up, the logSet is swapped (between A/B) and the set
 * that is not currently in use is a candidate for uploading if the user agreed
 * to it. This class handles the swapping/rotation, compression, and upload.
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class StatsLogger {
    private static StatsLogger instance = null;
    private final Logger statsLog = Logger.getLogger("com.ebixio.virtmus.stats");
    private String logSet;
    private Handler logHandler = null;
    private static Preferences pref = NbPreferences.forModule(MainApp.class);

    // What did the user say when we asked them to upload the stats?
    private static enum UploadStats {Yes, No,
        // Ask the user at a later time
        Maybe,
        // The user was never asked
        Unknown};

    private StatsLogger() {
        try {

            statsLog.setUseParentHandlers(false);
            statsLog.setLevel(Level.ALL);

            Logger uiLogger = Logger.getLogger("org.netbeans.ui");
            // org.netbeans.ui.focus = maybe too much info

            uiLogger.setUseParentHandlers(false);
            uiLogger.setLevel(Level.ALL);

            logSet = pref.get(Options.OptLogSet, "A");
            if (! changeHandler(makeLogHandler(logSet))) {
                Log.log("Stats logging init failed.");
                statsLog.setLevel(Level.OFF);
                uiLogger.setLevel(Level.OFF);
            }

        } catch (SecurityException ex) {
            Log.log(ex);
        }
    }

    public static synchronized StatsLogger findInstance() {
        if (instance == null) {
            instance = new StatsLogger();
        }
        return instance;
    }

    /** This is the stats logger. If we provide a log(LogRecord r) method instead,
     * all logs show this StatsLogger/log() as the class/method name.
     * @return A logger for anonymous stats.
     */
    public static Logger getLogger() {
        return StatsLogger.findInstance().statsLog;
    }

    /** Called when VirtMus is starting up.
     * Should only be called once per startup, and before we do any logging. */
    public void startingUp() {
        // At this time, everything we need to do has been done by the singleton ctor.
        // In the future we may need to do additional handling here.
    }

    /** Called after VirtMus has started up.
     * Should only be called once per startup, and after we've detected the new
     * and old app version (if this was an upgrade).
     */
    public void startedUp() {
        int launch = pref.getInt(Options.OptStartCounter, 1);
        pref.putInt(Options.OptStartCounter, launch + 1);

        UploadStats upload = UploadStats.valueOf(
                pref.get(Options.OptUploadStats, UploadStats.Unknown.name()) );

        // Only ask the 2nd time the user starts up the app
        if (launch > 1) {
            if ( upload == UploadStats.Unknown ||
                (upload == UploadStats.Maybe && (launch % 10 == 0)) ||
                (upload == UploadStats.No && (launch % 100 == 0))
            ) {

                UploadStats newUpload = promptUser();
                if (newUpload != upload) {
                    pref.put(Options.OptUploadStats, newUpload.name());
                    upload = newUpload;
                }
            }
        }

        long installId = MainApp.getInstallId();
        String prevVersion = pref.get(Options.OptPrevAppVersion, "0.00");
        if (pref.getBoolean(Options.OptCheckVersion, true)) {
            StatsLogger.checkForNewVersion(installId, prevVersion, upload);
        }

        if (upload != UploadStats.No) {

            LogRecord rec = new LogRecord(Level.INFO, "VirtMus Version");
            rec.setParameters(new Object[]{MainApp.VERSION, prevVersion, installId});
            statsLog.log(rec);

            StatsCollector.logStartup(statsLog);
            // TODO: Log uptime, etc...
        }

        if (upload == UploadStats.Yes) {
            uploadLogs();
        }
    }

    /**
     * Ask the user if they want to help by uploading stats logs.
     */
    private UploadStats promptUser() {
        String yes = NbBundle.getMessage(StatsLogger.class, "BTN_Yes"),
                maybeLater = NbBundle.getMessage(StatsLogger.class, "BTN_MaybeLater"),
                never = NbBundle.getMessage(StatsLogger.class, "BTN_Never");

        int userChoice = JOptionPane.showOptionDialog(null,
                NbBundle.getMessage(StatsLogger.class, "MSG_Text"),
                NbBundle.getMessage(StatsLogger.class, "MSG_Title"),
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new Object[]{yes, maybeLater, never}, yes);

        switch (userChoice) {
            case 0: return UploadStats.Yes;
            case 2: return UploadStats.No;
            case 1: return UploadStats.Maybe;
            case -1: // User closed the dialog. Leave it as "Unknown".
            default:
                return UploadStats.Unknown;
        }
    }

    /**
     * Submits stats logs to the server. Spawns a separate thread to do all the
     * work so that we don't block the UI if the server doesn't respond.
     */
    private void uploadLogs() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String oldLogSet = rotate();
                uploadLogs(oldLogSet);
            }
        };
        Thread t = new Thread(r);
        t.setName("SubmitLogs");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** Should only be called from uploadLogs(). Compresses all files that belong
     to the given log set, and uploads all compressed files to the server. */
    private boolean uploadLogs(final String logSet) {
        if (logSet == null) return false;

        File logsDir = getLogsDir();
        if (logsDir == null) return false;
        gzipLogs(logsDir, logSet);

        // Uploading only gz'd files
        FilenameFilter gzFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".gz");
            }
        };

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost("http://ebixio.com/virtmus/analytics-upload");
        post.setHeader("User-Agent", "VirtMus-" + MainApp.VERSION);

        MultipartEntityBuilder entity = MultipartEntityBuilder.create();
        entity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        entity.addPart("InstallId", new StringBody(String.valueOf(MainApp.getInstallId()), ContentType.TEXT_PLAIN));

        ContentType ct = ContentType.create("x-application/gzip");
        File[] toUpload = logsDir.listFiles(gzFilter);
        for (File f: toUpload) {
            entity.addPart("VirtMusStats", new FileBody(f, ct, f.getName()));
        }

        post.setEntity(entity.build());
        try {
            HttpResponse response = client.execute(post);

            int status = response.getStatusLine().getStatusCode();
            Log.log(Level.INFO, "Log upload result: {0}", status);
            if (status == HttpStatus.SC_OK) { // Success (200)
                for (File f: toUpload) {
                    f.delete();
                }
            }

            HttpEntity rspEntity = response.getEntity();
            EntityUtils.consume(rspEntity);
            client.close();
        } catch (IOException ex) {
            Log.log(ex);
        }

        return true;
    }

    /**
     * Gzips log files. It is safe to run this more than once on the same directory.
     * @param logsDir The directory to search for un-zipped logs.
     * @param logSet The log set to zip up.
     */
    private static void gzipLogs(File logsDir, final String logSet) {
        FilenameFilter logFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // VirtMus-A-0.log, VirtMus-A-0.log.1
                return Pattern.matches("VirtMus-" + logSet + "-\\d+\\.log(\\.\\d+)*", name);
            }
        };

        // Zip up log files to be uploaded
        int counter = 0;
        for (File f: logsDir.listFiles(logFilter)) {
            String newName;
            File newFile;
            do {
                newName = String.format("%s%sVirtMus-%03d.gz", f.getParent(), File.separator, ++counter);
                newFile = new File(newName);
            } while (newFile.exists());

            if (gzipFile(f, newName)) {
                f.delete();
            }
        }


    }

    /**
     * Compresses a file using gzip. The original file is left un-touched.
     *
     * @param orig The file to compress
     * @param gz The name of the compressed file (should end in .gz)
     * @return true if the compression succeeded.
     */
    private static boolean gzipFile(File orig, String gz) {
        try (
            FileInputStream fis = new FileInputStream(orig);
            FileOutputStream fos = new FileOutputStream(gz);
            GZIPOutputStream gzos = new GZIPOutputStream(fos);
        ) {
            byte[] buffer = new byte[1024];
            int len;
            while((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
            return true;
        } catch (FileNotFoundException ex) {
            Log.log(ex);
        } catch (IOException ex) {
            Log.log(ex);
        }

        return false;
    }

    /** Rotates the log files/sets. */
    private synchronized String rotate() {
        String oldLogSet = logSet;

        // Switch over the logging to the new log set
        if (oldLogSet.equals("A")) {
            logSet = "B";
        } else {
            logSet = "A";
        }

        Handler newLogHandler = makeLogHandler(logSet);
        if (changeHandler(newLogHandler)) {
            pref.put(Options.OptLogSet, logSet);
            return oldLogSet;
        } else {
            logSet = oldLogSet;
            return null;
        }
    }

    /** Called when the log handler changes (when the logs are rotated). */
    private synchronized boolean changeHandler(Handler newLogHandler) {
        if (newLogHandler == null) return false;

        Logger uiLogger = Logger.getLogger("org.netbeans.ui");

        if (logHandler != null) {
            statsLog.removeHandler(logHandler);
            uiLogger.removeHandler(logHandler);
            logHandler.close();
        }

        statsLog.addHandler(newLogHandler);
        uiLogger.addHandler(newLogHandler);

        logHandler = newLogHandler;
        return true;
    }

    /** Creates a log handler for the stats logging.
     * @param newLogSet An identifier: "A" or "B".
     */
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
            Log.log(ex);
        }
        return handler;
    }

    /**
     * Check for new versions of VirtMus.
     * @param installId The random ID identifying this install.
     * @param prevVersion The previous version of VirtMus installed.
     * @param statsEnabled Set to true if the user is participating in stats collection.
     */
    private static void checkForNewVersion(long installId, String prevVersion, UploadStats statsEnabled) {
        try {
            URL url = new URL("http://ebixio.com/virtmus/analytics");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setReadTimeout(10 * 1000);
            conn.setDoOutput(true);
            conn.setDoInput(true);  // To read the response
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "VirtMus-" + MainApp.VERSION);

            try (DataOutputStream outS = new DataOutputStream(conn.getOutputStream())) {
                // TODO: Use http://wiki.fasterxml.com/JacksonHome to avoid warnings?
                String postData = new JSONStringer()
                    .object()
                        .key("version").value(MainApp.VERSION)
                        /* installId MUST be sent as string since JS on the server
                        side only has 64-bit float values and can't represent
                        all long int values, leading to truncation of some digits
                        since the JS float mantisa has only 53 bits (not 64).
                        See: http://www.2ality.com/2012/07/large-integers.html
                        */
                        .key("installId").value(String.valueOf(installId))
                        .key("prevVersion").value(prevVersion)
                        .key("statsEnabled").value(statsEnabled.name())
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

            if (conn.getResponseCode() == 200 && statsEnabled == UploadStats.No) {
                // If the user doesn't want to participate, he probably doesn't
                // want to be checking for new releases either, so disable it.
                pref.putBoolean(Options.OptCheckVersion, false);
            }

            // This is used to notify the user that a newer version is available
            if (rsp.length() > 0 && statsEnabled != UploadStats.No) {
                File f = File.createTempFile("VersionPost", "html");
                f.deleteOnExit();
                try (FileWriter w = new FileWriter(f)) {
                    w.write(rsp.toString());
                    URL rspUrl = Utilities.toURI(f).toURL();
                    HtmlBrowser.URLDisplayer.getDefault().showURL(rspUrl);
                }
            }
        } catch (MalformedURLException ex) {
            Log.log(ex);
        } catch (IOException ex) {
            Log.log(ex);
        }
    }

    /** The directory where the logs are stored. */
    public static File getLogsDir() {
        File userDir = Places.getUserDirectory();
        if (userDir != null) {
            return new File(new File(userDir, "var"), "log");
        } else {
            return null;
        }
    }

    /** This is the file we will log to (and upload). */
    public static File getLogFile(String pattern) {
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
}
