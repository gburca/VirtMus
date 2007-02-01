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

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author gburca
 */
public class Utils {
    
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

    static Dimension[] getScreenSizes() {
        Vector<Dimension> sizes = new Vector<Dimension>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        
        for (int i = 0; i < gs.length; i++) {
            DisplayMode dm = gs[i].getDisplayMode();
            sizes.add(new Dimension(dm.getWidth(), dm.getHeight()));
        }
        
        return (Dimension[]) sizes.toArray();
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
    
    // <editor-fold defaultstate="collapsed" desc=" Sizing and scaling ">    
    static double scaleProportional(Rectangle container, Rectangle item) {
        double scaleX = (double)container.width / (double)item.width;
        double scaleY = (double)container.height / (double)item.height;
        return Math.min(scaleX, scaleY);
    }
    
    static Rectangle shrinkToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        if (scale > 1) {
            return item;
        } else {
            return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
        }
    }
    
    static Rectangle stretchToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        if (scale < 1) {
            return item;
        } else {
            return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
        }
    }
    
    static Rectangle scaleToFit(Rectangle container, Rectangle item) {
        double scale = scaleProportional(container, item);
        return new Rectangle((int)(item.width * scale), (int)(item.height * scale));        
    }
    
    static Point centerItem(Rectangle container, Rectangle item) {
        int x = (container.width / 2) - (item.width / 2);
        int y = (container.height / 2) - (item.height / 2);
        return new Point(x, y);
    }
    // </editor-fold>
    
    /** Search for page in same relative directory. PlayLists (and Songs) store the
     * full path to the song file (or music page files). If the playlist/song is
     * moved (along with the song/musicpage files) we try to locate the songs/pages
     * by assuming they reside in the same directory relative to the playlist/song file.
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
     */
    static File findFileRelative(File newSrc, File oldTarget) {
        if (newSrc == null || oldTarget == null) return null;
        
        if (oldTarget.exists()) {
            return oldTarget;
        }

        try {
            newSrc = newSrc.getCanonicalFile();
            String newParentName = newSrc.getParent();
            
            oldTarget = oldTarget.getCanonicalFile();
            String oldFileName = oldTarget.getName();
            String oldParentName = oldTarget.getParent();
            
            // See if the page files are in the same directory as the song file
            File testF = new File(newParentName + File.separator + oldFileName);
            if (testF.exists()) return testF;
            
            String[] dirs = null;
            if (File.separator.equals("\\")) {
                // Double it once to escape from Java and once more to escape from RegEx
                dirs = oldParentName.split("\\\\");
            } else {
                dirs = oldParentName.split(File.separator);
            }
            
            if (dirs == null) return null;
            
            if (dirs.length > 0) {
                String test = "";
                for (int i = dirs.length - 1; i >= 0; i--) {
                    if (test.length() > 0) {
                        test = dirs[i] + File.separator + test;
                    } else {
                        test = dirs[i];
                    }
                    testF = new File(newParentName + File.separator + test + File.separator + oldFileName);
                    if (testF.exists()) return testF;
                }
            } else {
                testF = new File(newParentName + File.separator + oldFileName);
                if (testF.exists()) return testF;
            }
            
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (PatternSyntaxException ex) {
            ex.printStackTrace();
            return null;
        }
        
        return null;
    }
    
    /** Attempts to launch an external browser to handle a URL. */
    public static boolean openURL(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                        new Class[] {String.class});
                openURL.invoke(null, new Object[] {url});
            } else if (osName.startsWith("Windows"))
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            else { //assume Unix or Linux
                String[] browsers = {
                    "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
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
            //JOptionPane.showMessageDialog(null, errMsg + ":\n" + e.getLocalizedMessage());
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
    
}
