/*
 * Songs.java
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

import java.util.Vector;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;
import java.util.logging.Level;
import org.openide.nodes.Index;

/**
 *
 * @author gburca
 */
public class Songs extends Children.Keys<Song> implements ChangeListener
{
    private PlayList playList;
    
    /** Creates a new instance of Songs */
    public Songs(PlayList playList) {
        this.playList = playList;
        playList.addChangeListener(WeakListeners.change(this, playList));
    }

    @Override
    protected void addNotify() {
        MainApp.log("Songs::addNotify", Level.FINEST);
        setKeys(getKeys());
    }
    
    private Vector<Song> getKeys() {
        Vector<Song> songKeys = new Vector<Song>();
        synchronized (playList.songs) {
            for (int i = 0; i < playList.songs.size(); i++) {
                songKeys.add(playList.songs.get(i));
            }
        }
        return songKeys;
    }
    
    @Override
    protected Node[] createNodes(Song key) {
        if (key != null) {
            return new Node[] {new SongNode(playList, key, new MusicPages(key))};
        } else {
            return null;
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        // See comments in PlayLists::stateChanged
        addNotify();
        Vector<Song> allKeys = getKeys();
        for (Song s: allKeys) {
            refreshKey(s);
        }
    }
    
    public Index getIndex() {
        return new SongIndexer();
    }

    
    public class SongIndexer extends Index.Support {

        @Override
        public Node[] getNodes() {
            return Songs.this.getNodes();
        }

        @Override
        public int getNodesCount() {
            return getNodes().length;
        }

        @Override
        public void reorder(int[] order) {
            playList.reorder(order);
            fireChangeEvent(new ChangeEvent(SongIndexer.this));
        }
    }
    
}
