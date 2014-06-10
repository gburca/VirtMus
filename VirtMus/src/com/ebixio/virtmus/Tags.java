/*
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

import com.ebixio.util.WeakPropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class Tags extends Children.Keys<String> implements PropertyChangeListener {
    public static final HashMap<String, Set<PlayList>> plTags = new HashMap<>();
    public static final HashMap<String, Set<Song>> songTags = new HashMap<>();
    private boolean addListeners = false;

    /* When changes happen and we need to rescan the tags, we only want at most
    1 pending rescan task besides the one that's currently executing. Any more
    would be superfluous.
    */
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, 1, 5L, TimeUnit.SECONDS, new ArrayBlockingQueue(1), new ThreadPoolExecutor.DiscardPolicy());

    public Tags() {
    }

    /**
     * Initialize the Tags.
     * Keeping this separate from the constructor so we don't leak "this" in
     * the constructor.
     */
    public void init() {
        tpe.allowCoreThreadTimeOut(true);

        // Listen for PlayLists added or removed
        WeakPropertyChangeListener wpcl = new WeakPropertyChangeListener(this, PlayListSet.findInstance());
        PlayListSet.findInstance().addPropertyChangeListener(wpcl);
        // Pick up changes that happened before we registered for changes.
        if (PlayListSet.findInstance().getPlayListsLoading() == 0) {
            addListeners = true;
        }
        refreshKeys();
    }

    @Override
    protected void addNotify() {
        setKeys(getKeys());
    }

    private synchronized ArrayList<String> getKeys() {
        if (!addListeners) {
            return new ArrayList<>();
        }

        PlayListSet pls = PlayListSet.findInstance();

        WeakPropertyChangeListener pcl = new WeakPropertyChangeListener(this, pls);
        pls.addPropertyChangeListener(PlayListSet.PROP_NEW_PL_ADDED, pcl);
        pls.addPropertyChangeListener(PlayListSet.PROP_PL_DELETED, pcl);

        List<PlayList> pl = PlayListSet.findInstance().playLists;
        synchronized (pl) {
            songTags.clear();
            plTags.clear();

            for (PlayList p: pl) {
                pcl = new WeakPropertyChangeListener(this, p);
                p.addPropertyChangeListener(PlayList.PROP_SONG_ADDED, pcl);
                p.addPropertyChangeListener(PlayList.PROP_SONG_REMOVED, pcl);

                if (p.type == PlayList.Type.AllSongs || p.type == PlayList.Type.Default) {
                    synchronized(p.songs) {
                        for (Song s: p.songs) {
                            // The user may add a tag at a later time
                            s.addPropertyChangeListener(Song.PROP_TAGS, new WeakPropertyChangeListener(this, s));
                            for (String tag: Utils.tags2list(s.getTags())) {
                                if (!songTags.containsKey(tag)) {
                                    songTags.put(tag, new HashSet<Song>());
                                }
                                songTags.get(tag).add(s);
                            }
                        }
                    }
                } else {
                    p.addPropertyChangeListener(PlayList.PROP_TAGS, pcl);

                    for (String tag: Utils.tags2list(p.getTags())) {
                        if (!plTags.containsKey(tag)) {
                            plTags.put(tag, new HashSet<PlayList>());
                        }
                        plTags.get(tag).add(p);
                    }
                }
            }
        }

        // Show each tag just once (even if it's in both plTags and songTags)
        Set<String> tagSet = new HashSet<>();
        tagSet.addAll(plTags.keySet());
        tagSet.addAll(songTags.keySet());

        ArrayList<String> tagKeys = new ArrayList<>(tagSet);
        Collections.sort(tagKeys);

        return tagKeys;
    }

    @Override
    protected Node[] createNodes(String key) {
        return new Node[] {new TagNode(key, new TagChildren(key))};
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (
            (evt.getSource() instanceof Song     && Song.PROP_TAGS.equals(evt.getPropertyName()))
         || (evt.getSource() instanceof PlayList && PlayList.PROP_TAGS.equals(evt.getPropertyName()))) {
            //Log.log("Tags changed for S/P", Level.INFO);
            refreshKeys();
        } else if (PlayListSet.PROP_ALL_SONGS_LOADED.equals(evt.getPropertyName())) {
            //Log.log("All songs loaded. Refreshing tag nodes.", Level.INFO);
            addListeners = true;
            refreshKeys();
        }
    }

    /**
     * Handling the tag re-scan in a separate thread. This is primarily so we
     * don't get into a deadlock situation where we get a property change event
     * and turn around to register for other events. For example: allSongsLoaded
     * comes in, and we register for the newPlayLists property.
     */
    private synchronized void refreshKeys() {
        tpe.execute(new Runnable() {
                @Override
                public void run() {
                    addNotify();
                    for (String s: getKeys()) {
                        refreshKey(s);
                    }
                }
            }
        );
    }

}
