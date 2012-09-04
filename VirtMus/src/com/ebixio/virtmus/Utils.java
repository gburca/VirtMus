/*
 * Utils.java
 *
 * Copyright (C) 2006-2007  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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
import java.awt.*;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author gburca
 */
public class Utils {
    private static Cursor invisibleCursor = null;
    
    /** Creates a new instance of Utils */
    public Utils() {
    }

    // <editor-fold defaultstate="collapsed" desc=" Screen Size ">
    static Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
    static Dimension getScreenSize(int screen) {
        return getScreenSizes()[screen];
    }

    public static Dimension[] getScreenSizes() {
        ArrayList<Dimension> sizes = new ArrayList<Dimension>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        
        for (int i = 0; i < gs.length; i++) {
            DisplayMode dm = gs[i].getDisplayMode();
            sizes.add(new Dimension(dm.getWidth(), dm.getHeight()));
        }

        return sizes.toArray(new Dimension[sizes.size()]);
    }

    static int getNumberOfScreens() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            GraphicsDevice[] gs = ge.getScreenDevices();
            return gs.length;
        } catch (HeadlessException e) {
            // Thrown if there are no screen devices
            return 0;
        }
    }
    // </editor-fold>
    
    public static Cursor getInvisibleCursor() {
        if (invisibleCursor == null) {
            int[] pixels = new int[16 * 16];
            Image mouseImage = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));
            invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(mouseImage, new Point(0,0), "invisibleCursor");
        }
        return invisibleCursor;
    }
    
    // <editor-fold defaultstate="collapsed" desc=" Sizing and scaling ">    
    public static double scaleProportional(Rectangle container, Rectangle item) {
        double scaleX = (double)container.width / (double)item.width;
        double scaleY = (double)container.height / (double)item.height;
        return Math.min(scaleX, scaleY);
    }
    
    public static Rectangle shrinkToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        if (scale > 1) {
            return item;
        } else {
            return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
        }
    }
    
    public static Rectangle stretchToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        if (scale < 1) {
            return item;
        } else {
            return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
        }
    }
    
    public static Rectangle scaleToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
    }
    
    public static Point centerItem(Rectangle container, Rectangle item) {
        int x = (container.width / 2) - (item.width / 2);
        int y = (container.height / 2) - (item.height / 2);
        return new Point(x, y);
    }

    public static Rectangle scale(Rectangle rect, float scale) {
        Rectangle res = rect;
        res.width *= scale;
        res.height *= scale;
        return res;
    }
    // </editor-fold>
    
    /** Search for page in same relative directory. PlayLists (and Songs) store the
     * full path to the song file (or music page files). If the playlist/song is
     * moved (along with the song/musicpage files) we try to locate the songs/pages
     * by assuming they reside in the same directory relative to the playlist/song file.
     * 
     * Since we don't know where the original PlayList resided, we can't compute
     * the true relative location of the Songs it included, so we use heuristics.
     * 
     * If original song = /a/b/c/song.xml
     * and original page = /a/b/c/d/page.png
     * And now the song is in /x/y/z/song.xml
     *
     * We check for page as follows:
     * /x/y/z/page.png
     * /x/y/z/d/page.png
     * /x/y/z/c/d/page.png
     * /x/y/z/b/c/d/page.png
     * /x/y/z/a/b/c/d/page.png
     * 
     * That should account for the use case where the pages are in a subdirectory
     * of the song.
     * 
     * If old song = /a/b/c/song.xml
     * and old page = /a/b/d/page.png
     * And now the song is in /x/y/z/song.xml
     * 
     * We check for page as follows:
     * /x/y/d/page.png
     * /x/y/b/d/page.png
     * /x/y/a/b/d/page.png
     * 
     * /x/d/page.png
     * /x/b/d/page.png
     * /x/a/b/d/page.png
     * 
     * /d/page.png
     * /b/d/page.png
     * /a/b/d/page.png
     * 
     * @param newSrc The location of the new PlayList (or Song)
     * @param oldTarget The location of the old Song (or MusicPage)
     */
    static File findFileRelative(File newSrc, File oldTarget) {
        if (newSrc == null || oldTarget == null) return null;
        
        if (oldTarget.exists()) {
            return oldTarget;
        }

        try {
            newSrc = newSrc.getCanonicalFile();
            String newParentName = newSrc.getParent();  // /x/y/z
            
            try {
                // Canonical name can not be computed if drive is not mounted.
                // Ex: oldTarget = "D:\etc..." but there's no "D:" and songs
                // have been moved to "C:" instead.
                oldTarget = oldTarget.getCanonicalFile();
            } catch (IOException ex) {
                Log.log(ex);
            }
            String oldFileName = oldTarget.getName();   // /a/b/d
            String oldParentName = oldTarget.getParent();
            
            // See if the page files are in the same directory as the song file
            File testF = new File(newParentName + File.separator + oldFileName);
            if (testF.exists()) return testF;
            
            String[] oDirs = splitFile(oldParentName);
            if (oDirs == null) return null;
            
            if (oDirs.length > 0) {
                String test = "";
                for (int i = oDirs.length - 1; i >= 0; i--) {
                    if (test.length() > 0) {
                        test = oDirs[i] + File.separator + test;
                    } else {
                        test = oDirs[i];
                    }
                    testF = new File(newParentName + File.separator + test + File.separator + oldFileName);
                    if (testF.exists()) return testF;
                }
            } else {
                testF = new File(newParentName + File.separator + oldFileName);
                if (testF.exists()) return testF;
            }
            
            // We could also try different roots (drives) on Windows?
            //File[] roots = File.listRoots();
            Path oPath = oldTarget.getParentFile().toPath();
            Path nPath = newSrc.getParentFile().toPath();
            Path root = nPath.getRoot();
            for (int i = nPath.getNameCount() - 1; i > 0; i--) {
                for (int j = oPath.getNameCount() - 1; j >= 0; j--) {
                    Path oSub = oPath.subpath(j, oPath.getNameCount());
                    Path test = nPath.subpath(0, i).resolve(oSub);
                    test = test.resolve(oldFileName);
                    if (test.toFile().exists()) {
                        return test.toFile();
                    } else if (root != null) {
                        test = root.resolve(test);
                        if (test.toFile().exists()) {
                            return test.toFile();
                        }
                    }
                }
            }
            
        } catch (IOException ex) {
            Log.log(ex);
            return null;
        } catch (PatternSyntaxException ex) {
            Log.log(ex);
            return null;
        }
        
        return null;
    }
    
    public static String[] splitFile(String f) {
        String[] parts;
        if (File.separator.equals("\\")) {
            // Double it once to escape from Java and once more to escape from RegEx
            parts = f.split("\\\\");
        } else {
            parts = f.split(File.separator);
        }
        return parts;
    }
    
    /** Attempts to launch an external browser to handle a URL.
     * @param url The URL to launch the default browser with.
     * @return <b>true</b> on success
     */
    public static boolean openURL(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                @SuppressWarnings("unchecked")
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                        new Class[] {String.class});
                openURL.invoke(null, new Object[] {url});
            } else if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else { //assume Unix or Linux
                String[] browsers = {
                    "firefox", "opera", "konqueror", "epiphany", "google-chrome", "mozilla", "netscape" };
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++)
                    if (Runtime.getRuntime().exec(
                    new String[] {"which", browsers[count]}).waitFor() == 0)
                        browser = browsers[count];
                if (browser == null)
                    throw new Exception("Could not find web browser");
                else
                    Runtime.getRuntime().exec(new String[] {browser, url});
            }
        } catch (Exception e) {
            return false;
        }
        
        return true;
    }
    
    /** Provides the path to the top level application folder.
     * 
     * Due to differences in how files are laid out when running out of the NetBeans IDE
     * versus an unpacked ZIP distribution, this methods only works on Windows and Linux
     * when the application was deployed using a ZIP file.
     * 
     * This method is not guaranteed to work on all JVMs but works on Sun JVMs.
     */
    public static File getAppPath() {
        CodeSource source = MainApp.findInstance().getClass().getProtectionDomain().getCodeSource();
        if (source == null) return null;
        
        File codeLoc;
        try {
            URI sourceURI = new URI(source.getLocation().toString());
            codeLoc = new File(sourceURI);
        } catch (URISyntaxException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
        
        if (!codeLoc.isDirectory()) {
            codeLoc = codeLoc.getParentFile();
            if (codeLoc == null) return null;
        }
        
        // The app root directory is two levels up in a zip distribution
        codeLoc = codeLoc.getParentFile().getParentFile();
        
        return codeLoc;
    }
    
    /** Doesn't work on Windows or Linux */
    public static File getAppPath1() {
        File appPath = new File(System.getProperty("java.class.path"));
        try {
            appPath = appPath.getCanonicalFile().getParentFile();
        } catch (IOException e) {
            return null;
        }
        
        return appPath;
    }
    
    /** Works on windows, but not on Linux */
    public static File getAppPath2() {
        File appPath = new File(".");
        try {
            appPath = appPath.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
        
        return appPath;
    }

    public static File[] listFilesAsArray(File directory, FilenameFilter filter, boolean recurse) {
        Collection<File> files = listFiles(directory, filter, recurse);
        File[] arr = new File[files.size()];
        return files.toArray(arr);
    }

    public static Collection<File> listFiles(File directory, FilenameFilter filter, boolean recurse) {
        // List of files / directories
        ArrayList<File> files = new ArrayList<File>();
        // Get files / directories in the directory
        File[] entries = directory.listFiles();

        // Go over entries
        for (File entry : entries) {
            // If there is no filter or the filter accepts the
            // file / directory, add it to the list
            if (filter == null || filter.accept(directory, entry.getName())) {
                files.add(entry);
            }

            // If the file is a directory and the recurse flag
            // is set, recurse into the directory
            if (recurse && entry.isDirectory()) {
                files.addAll(listFiles(entry, filter, recurse));
            }
        }

        return files;
    }

    public static String shortenString(String orig, int charsToRemove) {
        if (charsToRemove <= 0) { return orig; }
        if (charsToRemove >= orig.length() - 1) { return orig.charAt(0) + "..."; }

        int cut = orig.length() - charsToRemove;
        int s1len = cut / 2 + cut % 2;
        int s2len = cut / 2;
        String s1 = orig.substring(0, s1len);
        String s2 = orig.substring(orig.length() - s2len);
        return s1 + "..." + s2;
    }
}
