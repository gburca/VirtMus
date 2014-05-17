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

package com.ebixio.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A version of PropertyChangeSupport that adds a listener only once. The listener
 * must be of WeakPropertyChangeListener type, or else it is added unconditionally.
 *
 * @author Gabriel Burca &lt;gburca dash virtmus at ebixio dot com&gt;
 */
public class PropertyChangeSupportUnique extends PropertyChangeSupport {

    public PropertyChangeSupportUnique(Object sourceBean) {
        super(sourceBean);
    }

    public void addPropertyChangeListener(WeakPropertyChangeListener listener) {
        if (isNew(listener)) {
            super.addPropertyChangeListener(listener);
        }
    }

    public void addPropertyChangeListener(String propertyName, WeakPropertyChangeListener listener) {
        if (isNew(propertyName, listener)) {
            super.addPropertyChangeListener(propertyName, listener);
        }
    }

    private boolean isNew(WeakPropertyChangeListener newListener) {
        return !contains(getPropertyChangeListeners(), newListener);
    }

    private boolean isNew(String propertyName, WeakPropertyChangeListener newListener) {
        return !contains(getPropertyChangeListeners(propertyName), newListener);
    }

    /** Checks to see if the new listener is already in the list. */
    private boolean contains(PropertyChangeListener[] pcl, WeakPropertyChangeListener newListener) {
        for (PropertyChangeListener p: pcl) {
            if (p instanceof WeakPropertyChangeListener) {
                if (newListener.isSameListener(p)) {
                    return true;
                }
            }
        }
        return false;
    }
}
