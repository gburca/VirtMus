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

import com.ebixio.util.Log;
import com.ebixio.util.WeakPropertyChangeListener;
import com.ebixio.virtmus.actions.GoLive;
import com.ebixio.virtmus.actions.PlayListDelete;
import com.ebixio.virtmus.actions.PlayListRevertAction;
import com.ebixio.virtmus.actions.RenameItemAction;
import com.ebixio.virtmus.actions.SavePlayListAction;
import com.ebixio.virtmus.actions.SongNewAction;
import com.ebixio.virtmus.actions.SongOpenAction;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.ErrorManager;
import org.openide.actions.PasteAction;
import org.openide.actions.ReorderAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
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

        playList.addPropertyChangeListener(PlayList.PROP_NAME, new WeakPropertyChangeListener(this, playList));
        playList.addChangeListener(WeakListeners.change(this, playList));
    }

    @Override
    public Action[] getActions(boolean context) {
        switch (playList.type) {
            case Normal:
                return new Action[] {
                    SystemAction.get(GoLive.class),
                    SystemAction.get(SavePlayListAction.class),
                    SystemAction.get(PlayListRevertAction.class),
                    null,
                    SystemAction.get(SongNewAction.class),
                    SystemAction.get(SongOpenAction.class),
                    null,
                    SystemAction.get(PasteAction.class),
                    null,
                    SystemAction.get(RenameItemAction.class),
                    SystemAction.get(ReorderAction.class),
                    null,
                    SystemAction.get(PlayListDelete.class)

                };
            case Default:
                return new Action[] {
                    SystemAction.get(GoLive.class),
                    null,
                    SystemAction.get(SongNewAction.class),
                    SystemAction.get(SongOpenAction.class),
                    null,
                    SystemAction.get(PasteAction.class)
                };
            case AllSongs:
                return new Action[] {
                    SystemAction.get(GoLive.class),
                    null,
                    SystemAction.get(SongNewAction.class),
                    SystemAction.get(SongOpenAction.class),
                };
            default:
                return new Action[]{};
        }
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        PlayList pl = getLookup().lookup(PlayList.class);
        boolean normal = pl.type == PlayList.Type.Normal;

        try {
            Property nameProp = new PropertySupport.Reflection<>(pl, String.class, "getName", normal ? "setName" : null); // get/setName
            Property fileProp = new PropertySupport.Reflection<>(pl, File.class, "getSourceFile", null); // only getSourceFile
            Property songsProp = new PropertySupport.Reflection<>(pl, Integer.class, "getSongCnt", null); // only getSongCnt
            Property tagsProp = new PropertySupport.Reflection<>(pl, String.class, "tags"); // get/setTags
            Property notesProp = new PropertySupport.Reflection<>(pl, String.class, "notes"); // get/setNotes
            nameProp.setName("Name");
            fileProp.setName("Source File");
            songsProp.setName("Songs");
            tagsProp.setName("Tags");
            tagsProp.setShortDescription(
                NbBundle.getMessage(PlayListTopComponent.class, "CTL_TagsDescription"));
            notesProp.setName("Notes");
            set.put(nameProp);
            set.put(fileProp);
            set.put(songsProp);
            if (normal) {
                set.put(tagsProp);
                set.put(notesProp);
            }
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
    public PasteType getDropType(final Transferable t, final int action, int index) {
        //Log.log("PlayListNode::getDropType p:" + playList.getName() + " i:" + Integer.toString(index));

        // Can't paste into the AllSongs PlayList
        if (playList.type == PlayList.Type.AllSongs) return null;

        DataFlavor[] flavors = t.getTransferDataFlavors();

        if (t.isDataFlavorSupported(SongFlavor.SONG_FLAVOR)) {
            return new PasteType() {
                @Override
                public Transferable paste() throws IOException {
                    try {
                        Song song = (Song)t.getTransferData(SongFlavor.SONG_FLAVOR);
                        if (song != null) playList.addSong(song);

                        final Node songNode = NodeTransfer.node(t, NodeTransfer.DND_MOVE + NodeTransfer.CLIPBOARD_CUT);
                        if (songNode != null) {
                            //final PlayList source = node.getLookup().lookup(PlayList.class);
                            songNode.destroy();
                        }
                    } catch (UnsupportedFlavorException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    return null;
                }
            };
        } else if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            // TODO: Handle file drops. Convert PDFs into songs, and sets of
            // images into a single song?
            return new PasteType() {
                @Override
                public Transferable paste() throws IOException {
                    try {
                        List fileList = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
                        Log.log("Pasting file");
                    } catch (UnsupportedFlavorException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    return null;
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public boolean canCut()     { return false; }   // Makes no sense to cut a PlayList
    @Override
    public boolean canCopy()    { return false; }   // Makes no sense to copy a PlayList

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" Node name ">

    @Override
    public boolean canRename() {
        return playList.type == PlayList.Type.Normal;
    }

    @Override
    public void setName(String nue) {
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

        if (playList.isFullyLoaded()) {
            name = "<font color='!textText'>" + name + "</font>";
        } else {
            name = "<font color='!controlShadow'>" + name + " (loading)</font>";
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

    public PlayList getPlayList() {
        return playList;
    }

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
        if (PlayList.PROP_NAME.equals(evt.getPropertyName())) {
            String newName = (String)evt.getNewValue();
            this.fireDisplayNameChange(null, newName);
        }
    }
    // </editor-fold>

    @Override
    public int compareTo(PlayListNode o) {
        return playList.compareTo(o.playList);
    }

    @Override
    public String toString() {
        if (playList.type == PlayList.Type.Normal) {
            return playList.getName() + " [" + playList.getSourceFile().getAbsolutePath() + "]";
        } else {
            return playList.getName();
        }
    }
}
