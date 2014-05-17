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
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class PlayLists extends Children.Keys<PlayList> implements PropertyChangeListener {
    
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
        return new ArrayList<>(PlayListSet.findInstance().playLists);
    }

    /**
     * This method will get called with one of the items passed to setKeys in 
     * addNotify above.
     * @param key A key to create the node for
     * @return A node corresponding to the key
     */
    @Override
    protected Node[] createNodes(PlayList key) {
        PlayListNode pln = new PlayListNode(key, new Songs(key));
        return new Node[] {pln};
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        refreshKeys();
    }
    
    private void refreshKeys() {
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
}
