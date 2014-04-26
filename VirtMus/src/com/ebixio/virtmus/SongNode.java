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
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.ErrorManager;
import org.openide.actions.CutAction;
import org.openide.actions.MoveDownAction;
import org.openide.actions.MoveUpAction;
import org.openide.actions.NewAction;
import org.openide.actions.PasteAction;
import org.openide.actions.ReorderAction;
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
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class SongNode extends AbstractNode
    implements PropertyChangeListener, ChangeListener, Comparable<SongNode> {
    private final Song song;
    
    /** Creates a new instance of SongNode
     * @param playList The playlist the song belongs to
     * @param song The song represented by this node
     * @param children The music pages belonging to this song
     */
    public SongNode(PlayList playList, Song song, MusicPages children) {
        super(children, Lookups.fixed(new Object[]{playList, song, children.getIndex()}));
        this.song = song;
        displayFormat = new MessageFormat("{0}");
        setIconBaseWithExtension("com/ebixio/virtmus/resources/SongNode.png");

        song.addPropertyChangeListener(WeakListeners.propertyChange(this, song));
        song.addChangeListener(WeakListeners.change(this, song));
    }
    
    // Used by stand-alone songs in the Tag component
    public SongNode(Song song, MusicPages children) {
        super(children, Lookups.fixed(new Object[]{song, children.getIndex()}));
        this.song = song;
        displayFormat = new MessageFormat("{0}");
        setIconBaseWithExtension("com/ebixio/virtmus/resources/SongNode.png");

        song.addPropertyChangeListener(WeakListeners.propertyChange(this, song));
        song.addChangeListener(WeakListeners.change(this, song));
    }
        
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
            Property nameProp = new PropertySupport.Reflection<String>(s, String.class, "name"); // get/setName
            Property fileProp = new PropertySupport.Reflection<File>(s, File.class, "getSourceFile", null); // only getSourceFile
            Property tagsProp = new PropertySupport.Reflection<String>(s, String.class, "tags"); // get/setTags
            nameProp.setName("Name");
            fileProp.setName("Source File");
            tagsProp.setName("Tags");
            set.put(nameProp);
            set.put(fileProp);
            set.put(tagsProp);
        } catch (NoSuchMethodException ex) {
            ErrorManager.getDefault().notify(ex);
        }
        
        sheet.put(set);
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
            SystemAction.get( CutAction.class ),
            null,
            SystemAction.get( ReorderAction.class ),
            SystemAction.get( MoveUpAction.class ),
            SystemAction.get( MoveDownAction.class )
        };
    }

    public Song getSong() {
        return song;
    }

    // <editor-fold defaultstate="collapsed" desc=" Drag-n-drop ">
    
    @Override
    public boolean canCut()     { return true; }
    @Override
    public boolean canCopy()    { return true; }

    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        //Log.log("SongNode::createPasteTypes " + t.toString() + " s:" + song.getName());
        PasteType paste = getDropType( t, DnDConstants.ACTION_COPY, -1);
        if (paste != null) s.add(paste);
    }

    /* Need to tell NB how to "paste" the node being "dragged" on top of this node
     */
    @Override
    public PasteType getDropType(Transferable t, final int action, int index) {
        if (index != -1) {
            //Log.log("SongNode::getDropType " + Integer.toString(index) + " " + Integer.toString(action) + " s:" + song.getName());
        }
        // dropNode is the node about to be dropped on this SongNode
        final Node dropNode = NodeTransfer.node(t, DnDConstants.ACTION_COPY_OR_MOVE + NodeTransfer.CLIPBOARD_CUT);
        if (dropNode != null) {
            final MusicPage mp = dropNode.getLookup().lookup( MusicPage.class );
            // We only accept a MusicPage to be dropped on this SongNode
            if (mp != null) {
                
                //Log.log("SongNode::getDropType2 " + Integer.toString(index) + " " + Integer.toString(action) + " s:" + song.getName());
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
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" PropertyChangeListener interface ">
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("nameProp".equals(evt.getPropertyName())) {
            String newName = (String)evt.getNewValue();
            this.fireDisplayNameChange(null, newName);
        }
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" Node name ">
    
    @Override
    public boolean canRename()  { return true; }
    @Override
    public void setName(String nue) {
        if (nue.equals(song.getName())) return;
        song.setName(nue);
    }

    @Override
    public String getName() {
        return song.getName();
    }
    @Override
    public String getDisplayName() {
        return getName();
    }
    @Override
    public String getHtmlDisplayName() {
        String name = getDisplayName();
        
        if (song.isDirty()) {
            name = "<i>" + name + "</i>";
        }
        
        return name;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" ChangeListener interface ">
    public void stateChanged(ChangeEvent e) {
        fireDisplayNameChange(null, null);
    }
    // </editor-fold>

    @Override
    public int compareTo(SongNode o) {
        return song.compareTo(o.song);
    }

}
