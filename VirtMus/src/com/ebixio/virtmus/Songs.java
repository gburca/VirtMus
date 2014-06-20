/*
 * Songs.java
 *
 * Copyright (C) 2006-2014  Gabriel Burca (gburca dash virtmus at ebixio dot com)
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.swing.event.ChangeEvent;
import org.openide.nodes.Children;
import org.openide.nodes.Index;
import org.openide.nodes.Node;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class Songs extends Children.Keys<Song> implements PropertyChangeListener
{
    private final PlayList playList;

    /* When changes happen and we need to rescan the playlists, we only want at most
    1 pending rescan task besides the one that's currently executing. Any more
    would be superfluous.
    */
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, 1, 5L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1), new ThreadPoolExecutor.DiscardPolicy());


    /** Creates a new instance of Songs
     * @param playList The PlayList this song belongs to. */
    public Songs(PlayList playList) {
        tpe.allowCoreThreadTimeOut(true);

        this.playList = playList;
    }

    public void init() {
        playList.addPropertyChangeListener(new WeakPropertyChangeListener(this, playList));
    }

    @Override
    protected void addNotify() {
        Log.log(Level.FINEST, "Songs::addNotify");
        setKeys(getKeys());
    }

    private ArrayList<Song> getKeys() {
        ArrayList<Song> songKeys = new ArrayList<>();
        synchronized (playList.songs) {
            for (Song song : playList.songs) {
                songKeys.add(song);
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

    public Index getIndex() {
        return new SongIndexer();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (PlayList.PROP_SONG_ADDED.equals(evt.getPropertyName()) ||
            PlayList.PROP_SONG_REMOVED.equals(evt.getPropertyName())) {
            refreshKeys();
        }
    }

    /**
     * Handling refresh in a separate thread. This is so we don't get into a deadlock
     * situation with the propertyChange() being fired/called from a synchronized(songs)
     * block.
     * @see {@link Tags#refreshKeys()}
     */
    private void refreshKeys() {
        tpe.execute(new Runnable() {
            @Override
            public void run() {
                /* When setKeys is called from addNotify above, the class tries to be smart
                   and only calls createNodes for the newly added keys. If the content of a
                   node has changed, but the key remained the same, we need to call refreshKey(key)
                   for the change to be refreshed.
                 */
                addNotify();
                ArrayList<Song> allKeys = getKeys();
                for (Song s: allKeys) {
                    refreshKey(s);
                }
            }
        });
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
