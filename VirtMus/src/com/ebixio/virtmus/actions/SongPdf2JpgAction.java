/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.util.EDT;
import com.ebixio.util.NotifyUtil;
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.progress.ProgressUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
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
@ActionID(id = "com.ebixio.virtmus.actions.SongPdf2JpgAction", category = "Song")
@ActionRegistration(displayName = "#CTL_SongPdf2JpgAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Menu/Song", position = 2000)
})
public class SongPdf2JpgAction extends CookieAction {

    @Override
    protected void performAction(Node[] nodes) {
        final Song s = nodes[0].getLookup().lookup(Song.class);
        RequestProcessor.getDefault().post(new SongConverter(s));
    }

    class SongConverter implements Runnable {
        Song s;
        int maxProgress;
        final AtomicInteger progressI = new AtomicInteger(1);
        final ProgressHandle handle = ProgressHandleFactory.createHandle("PDF to JPG converter");

        public SongConverter(Song s) {
            this.s = s;
            maxProgress = s.pageOrder.size();
        }

        @Override
        public void run() {
            File curSongF = s.getSourceFile();
            final File curDir = curSongF.getParentFile();

            /* Consider what happens when the song file is named "Foo.pdf.song.xml".
             * There's a good chance the song pages come from Foo.pdf in the same
             * directory. We can't just use "songFile - songExt" (i.e. Foo.pdf)
             * as the destination directory. So let the user choose the directory. */
            File destDir = chooseDestDirOnEDT(curDir);
            if (destDir == null) return;

            File curPdfF = s.pageOrder.get(0).imgSrc.getSourceFile();
            String imgStem = Utils.trimExtension(curPdfF.getName(), null);

            final ImageIcon qIcon = new ImageIcon(ImageUtilities.loadImage(
                    "com/ebixio/virtmus/resources/VirtMus32x32.png", true));

            // The "1"s below are to show a little progress before the heavy lifting
            // starts, otherwise no progress is shown until after the 1st batch
            // finishes (for newWorkStealingPool() executors).
            handle.start(maxProgress + 1);
            handle.progress(1);

            ExecutorService executor = Executors.newWorkStealingPool();
            // For more consistent progress (but slower?) use:
            //ExecutorService executor = Executors.newSingleThreadExecutor();
            int returnVal;

            for (final MusicPage mp: s.pageOrder) {
                PdfImg pdfImg = (PdfImg)mp.imgSrc;
                final File newMusicPageF = new File(destDir.getAbsolutePath() + File.separator
                    + String.format("%s-%03d.jpg", imgStem, pdfImg.pageNum));

                if (newMusicPageF.exists()) {
                    try {
                        returnVal = EDT.invokeAndWait(new Callable<Integer>() {
                            @Override
                            public Integer call() throws Exception {
                                return JOptionPane.showConfirmDialog(null,
                                    "" + newMusicPageF + " already exists. Overwrite?",
                                    "Overwrite file?", JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, qIcon);
                            }
                        });
                    } catch (InterruptedException | InvocationTargetException ex) {
                        Exceptions.printStackTrace(ex);
                        returnVal = JOptionPane.CANCEL_OPTION;
                    }
                    switch (returnVal) {
                        case JOptionPane.CANCEL_OPTION:
                            return;
                        case JOptionPane.NO_OPTION:
                            continue;
                        case JOptionPane.YES_OPTION:
                        default:
                    }
                }

                executor.execute(new MusicPageSaver(mp, newMusicPageF));
            }

            try {
                executor.shutdown();
                executor.awaitTermination(30 * maxProgress, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }

            if (!destDir.equals(curDir)) {
                try {
                    returnVal = EDT.invokeAndWait(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return JOptionPane.showConfirmDialog(null,
                                "Move PDF+Song to new dir?", "Move?",
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, qIcon);
                        }
                    });
                } catch (InterruptedException | InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                    returnVal = JOptionPane.NO_OPTION;
                }

                if (returnVal == JOptionPane.YES_OPTION) {
                    File newPdfF = new File(destDir.getPath() + File.separator + curPdfF.getName());
                    int cnt = PlayListSet.findInstance().movedPdf(curPdfF, newPdfF);
                    curPdfF.renameTo(newPdfF);

                    File newSongF = new File(destDir + File.separator + curSongF.getName());
                    curSongF.renameTo(newSongF);
                    cnt = PlayListSet.findInstance().movedSong(curSongF, newSongF);
                }
            }
        }

        class MusicPageSaver implements Runnable {
            MusicPage mp;
            File newMusicPageF;

            public MusicPageSaver(MusicPage m, File f) {
                mp = m;
                newMusicPageF = f;
            }

            @Override
            public void run() {
                MainApp.setStatusText("Writing " + newMusicPageF);
                mp.saveImg(newMusicPageF, "jpg");
                /*
                 * It's possible for progress to be called with out-of-order values
                 * unless we synchronize
                 * - progress(3) in threadA is pre-empted before it finishes
                 * - progress(4) in threadB runs to completion
                 * - threadA resumes and finds it was called with 3 (< 4)
                 */
                synchronized (progressI) {
                    handle.progress(progressI.incrementAndGet());
                }
                if (progressI.get() >= maxProgress) {
                    handle.finish(); // Remove task from the status bar
                }
            }
        }
    }


    /**
     * Wrapper around {@link chooseDestDir()} that calls it on the EDT.
     * @param origDir
     * @return
     */
    private File chooseDestDirOnEDT(final File origDir) {
        try {
            return EDT.invokeAndWait(new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return chooseDestDir(origDir);
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
            return null;
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

    // <editor-fold defaultstate="collapsed" desc=" Cookie Action ">
    @Override
    protected boolean enable(Node[] nodes) {
        if (nodes.length == 1) {
            Song s = nodes[0].getLookup().lookup(Song.class);
            return s != null && isConvertible(s);
        }
        return false;
    }

//    @Override
//    protected String iconResource() {
//        return "com/ebixio/virtmus/resources/Pdf.gif";
//    }

    @Override
    public String getName() {
        return NbBundle.getMessage(SongPdf2JpgAction.class, "CTL_SongPdf2JpgAction");
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
    // </editor-fold>
}
