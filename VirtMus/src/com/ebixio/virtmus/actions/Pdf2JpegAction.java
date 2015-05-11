/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.actions;

import com.ebixio.util.Log;
import com.ebixio.virtmus.MusicPage;
import com.ebixio.virtmus.PlayList;
import com.ebixio.virtmus.Song;
import com.ebixio.virtmus.Utils;
import com.ebixio.virtmus.imgsrc.GenericImg;
import com.ebixio.virtmus.imgsrc.IcePdfImg;
import com.ebixio.virtmus.imgsrc.ImgSrc;
import com.ebixio.virtmus.imgsrc.PdfImg;
import com.ebixio.virtmus.imgsrc.PdfViewImg;
import java.io.File;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.NodeAction;

/**
 *
 * @author gburca1
 */
@ActionID(id = "com.ebixio.virtmus.actions.Pdf2JpegAction", category = "Song")
@ActionRegistration(displayName = "#CTL_Pdf2JpegAction", lazy = false)
@ActionReferences(value = {
    @ActionReference(path = "Menu/Song"),
    @ActionReference(path = "Toolbars/Song", name = "Pdf2JpegAction", position = 250)})
public class Pdf2JpegAction extends CookieAction {

    @Override
    protected void performAction(Node[] nodes) {
        for (Node n: nodes) {
            Song s = n.getLookup().lookup((Song.class));
            if (hasPdfPage(s)) {
                Log.log("Converting node to JPEG: " + n.getDisplayName());
                convertSong(s);
            }
        }
    }
    
    private void convertSong(Song s) {
        File curSongF = s.getSourceFile();
        File curDir = curSongF.getParentFile();
        File newDir = new File(Utils.trimExtension(curSongF.getName(),
                Song.SONG_FILE_EXT));
        File newSongF = new File(newDir.getName() + File.separator + curSongF.getName());
        
        if (newDir.exists() || !newDir.mkdir()) {
            // TODO: Throw some exception?
            return;
        } else {
            // Move/Convert pages first
            //Song newSong = new Song();
            int mpNr = 0;
            for (MusicPage mp: s.pageOrder) {
                mpNr++;
                File newMusicPageF = new File(newDir.getName() + File.separator
                        + String.format("%s-%02d.jpg", curSongF.getName(), mpNr));
                
                if (mp.imgSrc.getImgType() != ImgSrc.ImgType.PDF) {
                    // Move to new dir
                    mp.imgSrc.sourceFile.renameTo(newMusicPageF);
                    // TODO: Check to see if another song references this file
                    // and just copy it if that's the case?
                    mp.imgSrc.sourceFile = newMusicPageF;
                } else {
                    mp.saveImg(newMusicPageF, "jpg");
                    mp.imgSrc = new GenericImg(newMusicPageF);
                }
            }
            s.save();
            curSongF.renameTo(newSongF);
        }
    }

    @Override
    protected boolean enable(Node[] nodes) {
        for (Node n: nodes) {
            Song s = n.getLookup().lookup(Song.class);
            if (hasPdfPage(s)) return true;
        }
        return false;
    }

    private boolean hasPdfPage(Song s) {
        if (s == null) return false;

        for (MusicPage mp: s.pageOrder) {
            if (mp.imgSrc.getImgType() == ImgSrc.ImgType.PDF) return true;
        }

        return false;
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
        return CookieAction.MODE_ANY;
    }

    @Override
    protected Class<?>[] cookieClasses() {
        return new Class[] { Song.class };
    }

    @Override
    protected boolean asynchronous() {
        return true;
    }
}
