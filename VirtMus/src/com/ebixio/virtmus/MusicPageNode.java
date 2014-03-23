/*
 * MusicPageNode.java
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

import com.ebixio.virtmus.actions.MusicPageCloneAction;
import com.ebixio.virtmus.actions.MusicPageRemoveAction;
import com.ebixio.virtmus.actions.RenameItemAction;
import com.ebixio.virtmus.actions.SongSaveAction;
import com.ebixio.virtmus.imgsrc.PdfImg;
import java.io.File;
import java.text.MessageFormat;
import javax.swing.Action;
import org.openide.ErrorManager;
import org.openide.actions.CopyAction;
import org.openide.actions.CutAction;
import org.openide.actions.MoveDownAction;
import org.openide.actions.MoveUpAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class MusicPageNode extends AbstractNode {
    private MusicPage page;
    
    /** Creates a new instance of MusicPageNode
     * @param page The MusicPage represented by this node.
     */
    public MusicPageNode(MusicPage page) {
        super(Children.LEAF, Lookups.fixed(new Object[]{page.song, page,
            (new MusicPages(page.song)).getIndex()
        }));
        this.page = page;
        //setName(page.getName());
        displayFormat = new MessageFormat("{0}");
    }

    public MusicPage getPage() {
        return page;
    }
    
    @Override
    public boolean canDestroy() { return true; }
    
    // <editor-fold defaultstate="collapsed" desc=" Node name ">
    @Override
    public boolean canRename()  { return true; }
    @Override
    public void setName(String nue) {
        if (nue.equals(page.getName())) return;
        page.setName(nue);
    }

    @Override
    public String getName() {
        return page.getName();
    }
    @Override
    public String getDisplayName() {
        return getName();
    }
    @Override
    public String getHtmlDisplayName() {
        String name = getDisplayName();
        
        if (page.isDirty()) {
            name = "<i>" + name + "</i>";
        }
        
        return name;
    }

    // </editor-fold>

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            SystemAction.get( SongSaveAction.class ),
            null,
            SystemAction.get( CopyAction.class ),
            SystemAction.get( CutAction.class ),
            //SystemAction.get( DeleteAction.class ), // Using MusicPageRemoveAction.class instead
            null,
            SystemAction.get ( MusicPageCloneAction.class ),
            SystemAction.get ( MusicPageRemoveAction.class ),
            SystemAction.get ( RenameItemAction.class ),
            null,
            SystemAction.get( MoveUpAction.class ),
            SystemAction.get( MoveDownAction.class )
        };
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        MusicPage mp = getLookup().lookup(MusicPage.class);

        try {
            Property nameProp = new PropertySupport.Reflection<String>(mp, String.class, "name"); // get/setName
            Property fileProp = new PropertySupport.Reflection<File>(mp, File.class, "getSourceFile", null); // only getSourceFile
            Property pageProp = new PropertySupport.Reflection<Integer>(mp, Integer.class, "getPageNumber", null); // only getPageNumber
            Property typeProp;
            if (mp.imgSrc.getClass().equals(PdfImg.class)) {
                typeProp = new PropertySupport.Reflection<Class>(mp.imgSrc, Class.class, "getInnerClass", null);
            } else {
                typeProp = new PropertySupport.Reflection<Class>(mp.imgSrc, Class.class, "getClass", null);
            }
            nameProp.setName("Name");
            fileProp.setName("Source File");
            pageProp.setName("Page Number in Song");
            pageProp.setShortDescription("This is the n-th (0-based) page in the song");
            typeProp.setName("Page Type");
            set.put(nameProp);
            set.put(fileProp);
            set.put(pageProp);
            set.put(typeProp);

            Property pdfPage = null;
            if (mp.imgSrc instanceof PdfImg) {
                PdfImg pdf = (PdfImg)mp.imgSrc;
                pdfPage = new PropertySupport.Reflection<Integer>(pdf, Integer.class, "getPageNum", null);
            }

            if (pdfPage != null) {
                pdfPage.setName("PDF Page Number");
                set.put(pdfPage);
                pdfPage.setShortDescription("This is the n-th page in the PDF source");
            }
        } catch (NoSuchMethodException ex) {
            ErrorManager.getDefault().notify(ex);
        }

        sheet.put(set);
        return sheet;

    }

    // <editor-fold defaultstate="collapsed" desc=" Drag-n-Drop ">
    @Override
    public boolean canCut()     { return true; }
    @Override
    public boolean canCopy()    { return true; }
   
    // </editor-fold>

}
