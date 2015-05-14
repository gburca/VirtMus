/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.virtmus.MainApp;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.PlayList;
import com.ebixio.virtmus.PlayListSet;
import com.ebixio.virtmus.Song;
import com.ebixio.virtmus.Utils;
import com.ebixio.virtmus.imgsrc.ImgSrc;
import com.ebixio.virtmus.imgsrc.PdfImg;
import java.awt.Frame;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.windows.WindowManager;

/**
 * This action converts a song with PDF pages into one with JPG pages.
 *
 * If we try to fully automate this action, it is not clear what the right thing
 * to do is in certain cases:
 *
 * 1. The song contains a mixture of PDF and non-PDF pages. Do we move the
 * non-PDF pages to the new directory? They may be referenced from other songs.
 *
 * 2. The song is from a PDF book. Do we move the PDF to the new directory? If
 * we do then the other songs from the same book are left in a strange state.
 *
 * To simplify the action:
 *
 * Only allow conversion of all-PDF songs.
 *
 * Ask the user to provide a destination directory for the JPGs.
 *
 * If different from the current PDF directory, ask the user if we should move
 * the song+pdf to the new directory.
 *
 * Leave it up to the user to create a new JPG song with the generated JPGs.
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
@ActionID(id = "com.ebixio.virtmus.actions.Pdf2JpegAction", category = "Song")
@ActionRegistration(displayName = "#CTL_Pdf2JpegAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Menu/Song"),
    @ActionReference(path = "Toolbars/Song", name = "Pdf2JpegAction", position = 900)})
public class Pdf2JpegAction extends CookieAction {

    @Override
    protected void performAction(Node[] nodes) {
        Song s = nodes[0].getLookup().lookup(Song.class);
        convertSong(s);
    }

    private void convertSong(Song s) {
        /* Consider what happens when the song file is named "Foo.pdf.song.xml".
         * There's a good chance the song pages come from Foo.pdf in the same
         * directory. We can't just use "songFile - songExt" as the destination
         * directory. So let the user choose the directory.
         */
        File curSongF = s.getSourceFile();
        File curDir = curSongF.getParentFile();

        boolean move = false;
        File destDir = chooseDestDir(curDir);
        if (destDir == null) return;

        File curPdfF = s.pageOrder.get(0).imgSrc.getSourceFile();
        String imgStem = Utils.trimExtension(curPdfF.getName(), null);

        for (MusicPage mp: s.pageOrder) {
            PdfImg pdfImg = (PdfImg)mp.imgSrc;
            File newMusicPageF = new File(destDir.getAbsolutePath() + File.separator
                + String.format("%s-%03d.jpg", imgStem, pdfImg.pageNum));
            if (newMusicPageF.exists()) {
                // OOPS... TODO
            }
            MainApp.setStatusText("Writing " + newMusicPageF);
            mp.saveImg(newMusicPageF, "jpg");
        }

        if (!destDir.equals(curDir)) {
            int returnVal = JOptionPane.showConfirmDialog(null, "Move PDF+Song to new dir?", "Move?", JOptionPane.YES_NO_OPTION);
            move = (returnVal == JOptionPane.YES_OPTION);
        }

        if (move) {
            File newPdfF = new File(destDir.getPath() + File.separator + curPdfF.getName());
            int cnt = updateSongs(curPdfF, newPdfF);
            curPdfF.renameTo(newPdfF);

            File newSongF = new File(destDir + File.separator + curSongF.getName());
            curSongF.renameTo(newSongF);
            cnt = updatePlayLists(curSongF, newSongF);
        }
    }

    private File chooseDestDir(File origDir) {
        final Frame mainWindow = WindowManager.getDefault().getMainWindow();
        final JFileChooser fc = new JFileChooser();
        if (origDir != null && origDir.exists()) {
            fc.setCurrentDirectory(origDir);
        }

        fc.setApproveButtonToolTipText("Save JPGs to the selected directory");
        fc.setDialogTitle("Choose destination directory for JPGs");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setMultiSelectionEnabled(false);

        fc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
            @Override
            public String getDescription() {
                return "JPG destination directory";
            }
        });

        if (fc.showDialog(mainWindow, "Select Directory") == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            boolean success = true;
            if (file.exists()) {
                if (file.isDirectory()) {
                    return file;
                }
            } else {
                if (file.mkdirs()) return file;
            }
        }

        return null;
    }

    // Update PlayLists so they all point to the new song.
    // A PlayList could reference the same song more than once.
    private int updatePlayLists(File oldS, File newS) {
        int updated = 0;
        for (PlayList pl: PlayListSet.findInstance().playLists) {
            for (Song s: pl.songs) {
                if (s.getSourceFile() != null &&
                        s.getSourceFile().equals(oldS)) {
                    s.setSourceFile(newS);
                    updated++;
                }
            }

            pl.save();  // Only saves if isDirty
        }
        return updated;
    }

    private int updateSongs(File oldPdf, File newPdf) {
        int updated = 0;
        for (PlayList pl: PlayListSet.findInstance().playLists) {
            for (Song s: pl.songs) {
                boolean needsSave = false;
                for (MusicPage mp: s.pageOrder) {
                    if (mp.imgSrc.getSourceFile().equals(oldPdf)) {
                        mp.imgSrc.setSourceFile(newPdf);
                        needsSave = true;
                    }
                }
                if (needsSave) {
                    s.save();
                    updated++;
                }
            }
        }
        return updated;
    }

    @Override
    protected boolean enable(Node[] nodes) {
        if (nodes.length == 1) {
            Song s = nodes[0].getLookup().lookup(Song.class);
            return s != null && isConvertible(s);
        }
        return false;
    }

    /**
     * Checks to see if the song is convertible.
     *
     * That means it has at least 1 page, all the pages are PDF, and they all
     * come from the same PDF.
     *
     * @param s
     * @return
     */
    private boolean isConvertible(Song s) {
        if (s.pageOrder.isEmpty()) return false;

        File f = s.pageOrder.get(0).getSourceFile();
        if (f == null || !f.exists()) return false;

        for (MusicPage mp: s.pageOrder) {
            if (mp.imgSrc.getImgType() != ImgSrc.ImgType.PDF) return false;
            if (! f.equals(mp.imgSrc.sourceFile)) return false;
        }

        return true;
    }

//    @Override
//    protected String iconResource() {
//        return "com/ebixio/virtmus/resources/Pdf.gif";
//    }

    @Override
    public String getName() {
        return NbBundle.getMessage(Pdf2JpegAction.class, "CTL_Pdf2JpegAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    protected Class<?>[] cookieClasses() {
        return new Class[] { Song.class };
    }

    @Override
    protected boolean asynchronous() {
        // If this is set to true, we need to make sure the dialogs use the EDT.
        return false;
    }
}
