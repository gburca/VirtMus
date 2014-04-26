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

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;

/**
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class PlayLists extends Children.Keys<Integer> implements ChangeListener {
    
    /**
     * Creates a new instance of PlayLists
     * @param ma The main application
     */
    public PlayLists(MainApp ma) {
        //Log.log("PlayLists::constructor thread: " + Thread.currentThread().getName());
        //ma.addAllPlayLists(NbPreferences.forModule(MainApp.class));
        ma.addPLChangeListener(WeakListeners.change(this, ma));
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
    
    private ArrayList<Integer> getKeys() {
        int sz = MainApp.findInstance().playLists.size();
        ArrayList<Integer> plKeys = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            plKeys.add(i);
        }
        return plKeys;
    }

    /**
     * This method will get called with one of the items passed to setKeys in 
     * addNotify above.
     * @param key A key to create the node for
     * @return A node corresponding to the key
     */
    @Override
    protected Node[] createNodes(Integer key) {
        List<PlayList> pl = MainApp.findInstance().playLists;
        PlayListNode pln = null;
        
        synchronized (pl) {
            if (pl.size() > key) {
                pln = new PlayListNode(pl.get(key), new Songs(pl.get(key)));
            }  
        }
        return new Node[] {pln};
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        //Log.log("PlayLists::stateChanged: " + this.toString());
        /* When setKeys is called from addNotify above, the class tries to be smart
           and only calls createNodes for the newly added keys. If the content of a
           node has changed, but the key remained the same, we need to call refreshKey(key)
           for the change to be refreshed.
         */
        addNotify();
        List<Integer> allKeys = getKeys();
        for (Integer i: allKeys) {
            refreshKey(i);
        }
    }
    

}
