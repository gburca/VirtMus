/*
 * PlayLists.java
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class PlayLists extends Children.Keys<PlayList> implements PropertyChangeListener {
    /* When changes happen and we need to rescan the playlists, we only want at most
    1 pending rescan task besides the one that's currently executing. Any more
    would be superfluous.
    */
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, 1, 5L, TimeUnit.SECONDS, new ArrayBlockingQueue(1), new ThreadPoolExecutor.DiscardPolicy());

    /**
     * Creates a new instance of PlayLists.
     */
    public PlayLists() {
        //Log.log("PlayLists::constructor thread: " + Thread.currentThread().getName());
    }

    /**
     * Initialize the PlayLists.
     * Keeping this separate from the constructor so we don't leak "this" in
     * the constructor.
     */
    public void init() {
        tpe.allowCoreThreadTimeOut(true);

        WeakPropertyChangeListener wpcl = new WeakPropertyChangeListener(this, PlayListSet.findInstance());
        PlayListSet.findInstance().addPropertyChangeListener(wpcl);
        // Pick up the changes that happened before we registered for changes.
        refreshKeys();
    }

    /**
     * This method is called whenever a list of available PlayLists is about to be
     * displayed. We need to create here a bunch of "keys" to represent each playlist.
     * The framework will then call createNode with each "key" in turn to obtain the
     * actual PlayList object to be displayed.
     *
     * See the "Recognizing a File Type" tutorial
     */
    @Override
    protected void addNotify() {
        //Log.log("PlayLists::addNotify " + Thread.currentThread().getName());
        setKeys(getKeys());
    }

    private ArrayList<PlayList> getKeys() {
        synchronized(PlayListSet.findInstance().playLists) {
            return new ArrayList<>(PlayListSet.findInstance().playLists);
        }
    }

    /**
     * This method will get called with one of the items passed to setKeys in
     * addNotify above.
     * @param key A key to create the node for
     * @return A node corresponding to the key
     */
    @Override
    protected Node[] createNodes(PlayList key) {
        Songs songs = new Songs(key);
        songs.init();

        PlayListNode pln = new PlayListNode(key, songs);
        return new Node[] {pln};
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (PlayListSet.PROP_NEW_PL_ADDED.equals(prop) ||
            PlayListSet.PROP_ALL_PL_LOADED.equals(prop)) {
            refreshKeys();
        }
    }

    /**
     * Handling re-scan in a separate thread to prevent deadlock. The property
     * change event can be fired from a synchronized(playLists) block, and
     * getKeys() iterates over (and locks) the same collection causing a potential
     * deadlock situation.
     * @see Tags.handleTagChange()
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
                List<PlayList> allKeys = getKeys();
                for (PlayList p: allKeys) {
                    refreshKey(p);
                }
            }
        });
    }
}
