/*
 * PlayListNode.java
 *
 * Copyright (C) 2006-2012  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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

import com.ebixio.virtmus.actions.GoLive;
import com.ebixio.virtmus.actions.PlayListDelete;
import com.ebixio.virtmus.actions.PlayListRevertAction;
import com.ebixio.virtmus.actions.RenameItemAction;
import com.ebixio.virtmus.actions.SavePlayListAction;
import com.ebixio.virtmus.actions.SongNewAction;
import com.ebixio.virtmus.actions.SongOpenAction;
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
public class PlayListNode extends AbstractNode
    implements PropertyChangeListener, ChangeListener, Comparable<PlayListNode> {
    private final PlayList playList;
    
    /** Creates a new instance of PlayListNode
     * @param playList The PlayList represented by this node.
     * @param children The Songs in this node's PlayList.
     */
    public PlayListNode(PlayList playList, Songs children) {
        super (children, Lookups.fixed(new Object[]{playList, children.getIndex()}));
        this.playList = playList;
        displayFormat = new MessageFormat("{0}");
        setIconBaseWithExtension("com/ebixio/virtmus/resources/PlayListNode.png");
        
        playList.addPropertyChangeListener(WeakListeners.propertyChange(this, playList));
        playList.addChangeListener(WeakListeners.change(this, playList));
    }
    
    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            SystemAction.get(GoLive.class),
            SystemAction.get(SavePlayListAction.class),
            SystemAction.get(PlayListRevertAction.class),
            null,
            SystemAction.get(SongNewAction.class),
            SystemAction.get(SongOpenAction.class),
            null,
            SystemAction.get(RenameItemAction.class),
            SystemAction.get(ReorderAction.class),
            null,
            SystemAction.get(PlayListDelete.class)

        };
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        PlayList pl = getLookup().lookup(PlayList.class);

        try {
            Property nameProp = new PropertySupport.Reflection<String>(pl, String.class, "name"); // get/setName
            Property fileProp = new PropertySupport.Reflection<File>(pl, File.class, "getSourceFile", null); // only getSourceFile
            Property songsProp = new PropertySupport.Reflection<Integer>(pl, Integer.class, "getSongCnt", null);
            Property tagsProp = new PropertySupport.Reflection<String>(pl, String.class, "tags"); // get/setTags
            nameProp.setName("Name");
            fileProp.setName("Source File");
            songsProp.setName("Songs");
            tagsProp.setName("Tags");
            set.put(nameProp);
            set.put(fileProp);
            set.put(songsProp);
            set.put(tagsProp);
        } catch (NoSuchMethodException ex) {
            ErrorManager.getDefault().notify(ex);
        }

        sheet.put(set);
        return sheet;
    }
    
    // <editor-fold defaultstate="collapsed" desc=" Drag-n-drop ">

    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        super.createPasteTypes(t, s);
        //Log.log("PlayListNode::createPasteTypes " + t.toString() + " p:" + playList.getName());
        PasteType paste = getDropType(t, DnDConstants.ACTION_COPY, -1);
        if (paste != null) s.add(paste);
    }
    
    @Override
    public PasteType getDropType(Transferable t, final int action, int index) {
        //Log.log("PlayListNode::getDropType p:" + playList.getName() + " i:" + Integer.toString(index));
        final Node dropNode = NodeTransfer.node(t, DnDConstants.ACTION_COPY_OR_MOVE + NodeTransfer.CLIPBOARD_CUT);
        if (dropNode != null) {
            final Song song = dropNode.getLookup().lookup(Song.class);
            
            // Prevent a song for being dropped on the source playlist... ?
            //if (song != null && !this.equals(dropNode.getParentNode()) ) {
            if (song != null) {
                //Log.log("PlayListNode::getDropType2 p:" + playList.getName());
                return new PasteType() {
                    @Override
                    public Transferable paste() throws IOException {
                        if (playList.type != PlayList.Type.AllSongs) {
                            playList.addSong(song);
                        }
                        if ((action & DnDConstants.ACTION_MOVE) != 0) {
                            final PlayList source = dropNode.getLookup().lookup(PlayList.class);
                            if (source != null && source.type != PlayList.Type.AllSongs) {
                                source.removeSong(song);
                            }
                        }
                        return null;
                    }
                };
            }
        }
        return null;
    }

    @Override
    public boolean canCut()     { return false; }   // Makes no sense to cut a PlayList
    @Override
    public boolean canCopy()    { return false; }   // Makes no sense to copy a PlayList

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc=" Node name ">

    @Override
    public boolean canRename() { return true; }
    @Override
    public void setName(String nue) {
        if (nue.equals(playList.getName())) return;
        playList.setName(nue);
    }
    
    @Override
    public String getName() {
        return playList.getName();
    }
    @Override
    public String getDisplayName() {
        return getName();
    }
    @Override
    public String getHtmlDisplayName() {
        String name = super.getDisplayName();
        
        if (playList.isFullyLoaded) {
            name = "<font color='!textText'>" + name + "</font>";
        } else {
            name = "<font color='!controlShadow'>" + name + "</font>";
        }
        
        if (playList.isDirty()) {
            name = "<i>" + name + "</i>";
        }

        if (playList.isMissingSongs()) {
            name = "<b>" + name + "</b>";
        }
        
        return name;
    }
    // </editor-fold>

    public boolean removeSong(Song s) {
        return this.playList.removeSong(s);
    }

    // <editor-fold defaultstate="collapsed" desc=" ChangeListener interface ">
    @Override
    public void stateChanged(ChangeEvent e) {
        fireDisplayNameChange(null, null);
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

    @Override
    public int compareTo(PlayListNode o) {
        return playList.compareTo(o.playList);
    }

}
