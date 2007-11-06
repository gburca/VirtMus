/*
 * SongNode.java
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

import com.ebixio.virtmus.actions.RenameItemAction;
import com.ebixio.virtmus.actions.SongRemoveAction;
import com.ebixio.virtmus.actions.SongSaveAction;
import com.ebixio.virtmus.actions.SongSaveAsAction;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.Action;
import org.openide.ErrorManager;
import org.openide.actions.CutAction;
import org.openide.actions.NewAction;
import org.openide.actions.PasteAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.WeakListeners;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author gburca
 */
public class SongNode extends /*IndexedNode*/ AbstractNode implements /*Transferable,*/ PropertyChangeListener {
    private Song song;
    
    /** Creates a new instance of SongNode */
    public SongNode(PlayList playList, Song song) {
        //super (new MusicPages(song), Lookups.singleton(song));
        super(new MusicPages(song), Lookups.fixed(new Object[]{playList, song}));
        this.song = song;
        setName(song.getName());
        displayFormat = new MessageFormat("{0}");
        setIconBaseWithExtension("com/ebixio/virtmus/resources/SongNode.png");

        song.addPropertyChangeListener(WeakListeners.propertyChange(this, song));
    }
    

//    public Cookie getCookie(Class klass) {
//        return song;
//    }
    
    @Override
    public boolean canDestroy() {
        return true;
    }
    
    @Override
    protected Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        Song s = getLookup().lookup(Song.class);
        
        try {
            @SuppressWarnings("unchecked")
            Property nameProp = new PropertySupport.Reflection(s, String.class, "name");
            nameProp.setName("name");
            set.put(nameProp);
        } catch (NoSuchMethodException ex) {
            ErrorManager.getDefault().notify(ex);
        }
        return sheet;
    }
    
    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            SystemAction.get( NewAction.class ),
            SystemAction.get( SongSaveAction.class ),
            SystemAction.get( SongSaveAsAction.class ),
            SystemAction.get( SongRemoveAction.class ),
            SystemAction.get( RenameItemAction.class ),
            null,
            SystemAction.get( PasteAction.class ),
            SystemAction.get( CutAction.class )
        };
    }
    
    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        //MainApp.log("SongNode::createPasteTypes " + t.toString());
        PasteType paste = getDropType( t, DnDConstants.ACTION_COPY, -1);
        if (paste != null) s.add(paste);
    }

    /* Need to tell NB how to "paste" the node being "dragged" on top of this node
     */
    @Override
    public PasteType getDropType(Transferable t, final int action, int index) {
        if (index != -1) {
            MainApp.log("SongNode::getDropType " + Integer.toString(index) + " " + Integer.toString(action));
        }
        // dropNode is the node about to be dropped on this SongNode
        final Node dropNode = NodeTransfer.node(t, DnDConstants.ACTION_COPY_OR_MOVE + NodeTransfer.CLIPBOARD_CUT);
        if (dropNode != null) {
            final MusicPage mp = dropNode.getLookup().lookup( MusicPage.class );
            // We only accept a MusicPage to be dropped on this SongNode
            if (mp != null) {
                
                return new PasteType() {
                    public Transferable paste() throws IOException {
                        //song.addPage(new MusicPage(song, mp.getSourceFile()));
                        song.addPage(mp.clone(song));
                        if ((action & DnDConstants.ACTION_MOVE) != 0) {
                            mp.song.removePage(new MusicPage[] {mp});
                        }
                        return null;
                    }
                };

            }
        }
        return null;
    }


    // <editor-fold defaultstate="collapsed" desc=" PropertyChangeListener interface ">
    public void propertyChange(PropertyChangeEvent evt) {
        if ("name".equals(evt.getPropertyName())) {
            this.fireDisplayNameChange(null, getDisplayName());
        }
    }
    // </editor-fold>
    
    public Song getSong() {
        return song;
    }

    @Override
    public boolean canCut()     { return true; }
    @Override
    public boolean canRename()  { return true; }

//    public Transferable clipboardCut() throws IOException {
//        MainApp.log("SongNode::clipboardCut");
//        return this;
//    }
    

    // <editor-fold defaultstate="collapsed" desc=" Transferable interface ">
//    public DataFlavor[] getTransferDataFlavors() {
//        MainApp.log("SongNode::clipboardCut");
//        return new DataFlavor[] { new DataFlavor(this.getClass(), "SongNode") };
//    }
//
//    public boolean isDataFlavorSupported(DataFlavor flavor) {
//        if (flavor.equals(new DataFlavor(this.getClass(), "SongNode"))) {
//            MainApp.log("SongNode::isDataFlavorSupported true");
//            return true;
//        }
//        MainApp.log("SongNode::isDataFlavorSupported flase " + flavor.toString());
//        return false;
//    }
//
//    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
//        MainApp.log("SongNode::getTransferData");
//        return this;
//    }
    // </editor-fold>
}
