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

import java.util.ArrayList;
import java.util.Collections;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class TagChildren extends Children.Keys<String> {

    private final String tag;
    public TagChildren(String tag) {
        this.tag = tag;
    }

    @Override
    protected void addNotify() {
        setKeys(new String[]{tag});
    }

    /**
     * Creates the PlayList and Song nodes corresponding to the given tag.
     *
     * The (sorted) PlayLists are first, followed by the songs (also sorted).
     *
     * @param key A tag
     * @return An array of PlayList and Song nodes that have the given tag.
     */
    @Override
    protected Node[] createNodes(String key) {
        ArrayList<PlayListNode> pls = new ArrayList<>();
        ArrayList<SongNode> songs = new ArrayList<>();

        if (Tags.plTags.containsKey(key)) {
            for (PlayList p: Tags.plTags.get(key)) {
                Songs s = new Songs(p);
                s.init();

                pls.add(new PlayListNode(p, s));
            }
        }
        if (Tags.songTags.containsKey(key)) {
            for (Song s: Tags.songTags.get(key)) {
                songs.add(new SongNode(s, new MusicPages(s)));
            }
        }

        Collections.sort(pls);
        Collections.sort(songs);

        Node allNodes[] = new Node[pls.size() + songs.size()];
        int idx = 0;
        for (PlayListNode p: pls) {
            allNodes[idx++] = p;
        }
        for (SongNode s: songs) {
            allNodes[idx++] = s;
        }

        return allNodes;
    }

}
