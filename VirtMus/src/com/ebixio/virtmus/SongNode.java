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

import com.ebixio.util.WeakPropertyChangeListener;
import com.ebixio.virtmus.actions.SongPdf2JpgAction;
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
import org.openide.actions.CopyAction;
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
import org.openide.util.NbBundle;
import org.openide.util.WeakListeners;
import org.openide.util.actions.SystemAction;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class SongNode extends AbstractNode
    implements PropertyChangeListener, ChangeListener, Comparable<SongNode> {
    private Song song;

    /** Creates a new instance of SongNode
     * @param playList The playlist the song belongs to
     * @param song The song represented by this node
     * @param children The music pages belonging to this song
     */
    public SongNode(PlayList playList, Song song, MusicPages children) {
        super(children, Lookups.fixed(new Object[]{playList, song, children.getIndex()}));
        nodeConfig(song);
    }

    /** Creates a new instance of SongNode. This is used by stand-alone songs
     * that are not tied to a specific PlayList (ex: in the Tag TopComponent).
     * @param song The song represented by this node
     * @param children The music pages belonging to this song
     */
    public SongNode(Song song, MusicPages children) {
        super(children, Lookups.fixed(new Object[]{song, children.getIndex()}));
        nodeConfig(song);
    }

    /** 2nd stage constructor. */
    private void nodeConfig(Song song) {
        this.song = song;
        displayFormat = new MessageFormat("{0}");
        setIconBaseWithExtension("com/ebixio/virtmus/resources/SongNode.png");

        song.addPropertyChangeListener(Song.PROP_NAME, new WeakPropertyChangeListener(this, song));
        song.addChangeListener(WeakListeners.change(this, song));
    }

    @Override
    public boolean canDestroy() {
        PlayList myPlayList = getLookup().lookup(PlayList.class);
        return myPlayList != null && myPlayList.type != PlayList.Type.AllSongs;
    }

    /**
     * Called from
     * {@link PlayListNode#getDropType(java.awt.datatransfer.Transferable, int, int)}
     * when a song is cut (Ctrl-X) from a PlayList.
     *
     * By the time this function is called, the song has already been added to
     * the destination PlayList. We need to find the source PlayList and remove
     * the song from it (which should update the nodes).
     *
     * @see PlayListNode#getDropType(java.awt.datatransfer.Transferable, int, int)
     * @throws IOException Just because it can
     */
    @Override
    public void destroy() throws IOException {
        //Song s = getLookup().lookup(Song.class);
        PlayList source = getLookup().lookup(PlayList.class);
        if (source != null && source.type != PlayList.Type.AllSongs) {
            source.removeSong(song);
        }
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
            Property notesProp = new PropertySupport.Reflection<String>(s, String.class, "notes"); // get/setNotes
            nameProp.setName("Name");
            fileProp.setName("Source File");
            tagsProp.setName("Tags");
            tagsProp.setShortDescription(
                NbBundle.getMessage(PlayListTopComponent.class, "CTL_TagsDescription"));
            notesProp.setName("Notes");
            set.put(nameProp);
            set.put(fileProp);
            set.put(tagsProp);
            set.put(notesProp);
        } catch (NoSuchMethodException ex) {
            ErrorManager.getDefault().notify(ex);
        }

        sheet.put(set);
        return sheet;
    }

    @Override
    public Action[] getActions(boolean context) {
        PlayList pl = getLookup().lookup(PlayList.class);

        // Let's not confuse the user by allowing reordering if we won't save
        // the new ordering.
        if (pl == null || pl.type == PlayList.Type.AllSongs) {
            return new Action[] {
                SystemAction.get( NewAction.class ),
                SystemAction.get( SongSaveAction.class ),
                SystemAction.get( SongSaveAsAction.class ),
                SystemAction.get( SongRemoveAction.class ),
                SystemAction.get( RenameItemAction.class ),
                null,
                SystemAction.get( CutAction.class ),
                SystemAction.get( CopyAction.class ),
                SystemAction.get( PasteAction.class ),
                null,
                SystemAction.get( SongPdf2JpgAction.class ),
            };
        } else {
            return new Action[] {
                SystemAction.get( NewAction.class ),
                SystemAction.get( SongSaveAction.class ),
                SystemAction.get( SongSaveAsAction.class ),
                SystemAction.get( SongRemoveAction.class ),
                SystemAction.get( RenameItemAction.class ),
                null,
                SystemAction.get( CutAction.class ),
                SystemAction.get( CopyAction.class ),
                SystemAction.get( PasteAction.class ),
                null,
                SystemAction.get( SongPdf2JpgAction.class ),
                null,
                // We could override these classes and redefine enable().
                // See: SongRemoveAction#enable(Node[] nodes)
                SystemAction.get( ReorderAction.class ),
                SystemAction.get( MoveUpAction.class ),
                SystemAction.get( MoveDownAction.class )
            };
        }
    }

    public Song getSong() {
        return song;
    }

    // <editor-fold defaultstate="collapsed" desc=" Drag-n-drop ">

    /**
     * This controls the CutAction context menu availability. If we return false
     * the menu option is disabled. If we return true, then it is selectable.
     *
     * @return true if cutting this node is supported.
     * @see #getActions(boolean)
     */
    @Override
    public boolean canCut() {
        return canDestroy();
    }

    @Override
    public boolean canCopy() { return true; }

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
                    @Override
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

    @Override
    public Transferable clipboardCut() throws IOException {
        Transferable deflt = super.clipboardCut();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(SongFlavor.SONG_FLAVOR) {
            @Override
            protected Song getData() {
                return getLookup().lookup(Song.class);
            }
        });
        return added;
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        Transferable deflt = super.clipboardCopy();
        ExTransferable added = ExTransferable.create(deflt);
        added.put(new ExTransferable.Single(SongFlavor.SONG_FLAVOR) {
            @Override
            protected Song getData() {
                return getLookup().lookup(Song.class);
            }
        });
        return added;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc=" PropertyChangeListener interface ">
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (Song.PROP_NAME.equals(evt.getPropertyName())) {
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
    @Override
    public void stateChanged(ChangeEvent e) {
        fireDisplayNameChange(null, null);
    }
    // </editor-fold>

    @Override
    public int compareTo(SongNode o) {
        return song.compareTo(o.song);
    }

    @Override
    public String toString() {
        return song.getName() + " [" + song.getSourceFile().getAbsolutePath() + "]";
    }
}
