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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class Tags extends Children.Keys<String> implements PropertyChangeListener, ChangeListener {
    public static final HashMap<String, Set<PlayList>> plTags = new HashMap<>();
    public static final HashMap<String, Set<Song>> songTags = new HashMap<>();

    public Tags(MainApp ma) {
        // Listen for PlayLists added or removed
        ma.addPropertyChangeListener(MainApp.PROP_PL_LOADED, WeakListeners.propertyChange(this, ma));
    }

    @Override
    protected void addNotify() {
        setKeys(getKeys());
    }

    private synchronized ArrayList<String> getKeys() {
        List<PlayList> pl = MainApp.findInstance().playLists;
        synchronized (pl) {
            songTags.clear();
            plTags.clear();

            for (PlayList p: pl) {
                if (p.type == PlayList.Type.AllSongs || p.type == PlayList.Type.Default) {
                    for (Song s: p.songs) {
                        // The user may add a tag at a later time
                        s.addPropertyChangeListener(Song.PROP_TAGS, WeakListeners.propertyChange(this, s));
                        for (String tag: Utils.tags2list(s.getTags())) {
                            if (!songTags.containsKey(tag)) {
                                songTags.put(tag, new HashSet<Song>());
                            }
                            songTags.get(tag).add(s);
                        }
                    }
                } else {
                    // Get notified if new songs are added (deserialized)
                    p.addChangeListener(WeakListeners.change(this, p));

                    p.addPropertyChangeListener(PlayList.PROP_TAGS, WeakListeners.propertyChange(this, p));
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
    public void stateChanged(ChangeEvent e) {
        // PlayLists were added or removed
        handleTagChange();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (
            (evt.getSource() instanceof Song     && Song.PROP_TAGS.equals(evt.getPropertyName()))
         || (evt.getSource() instanceof PlayList && PlayList.PROP_TAGS.equals(evt.getPropertyName()))) {
            handleTagChange();
        } else if (MainApp.PROP_PL_LOADED.equals(evt.getPropertyName())) {

        }
    }

    private synchronized void handleTagChange() {
        addNotify();
        for (String s: getKeys()) {
            refreshKey(s);
        }
    }

}
